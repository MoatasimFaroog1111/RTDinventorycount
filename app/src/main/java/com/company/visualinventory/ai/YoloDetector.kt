package com.company.visualinventory.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.RectF
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Production-oriented YOLO TFLite adapter.
 *
 * Supported output layouts:
 * 1) [1, N, 5 + C]  -> cx,cy,w,h,obj,class...
 * 2) [1, N, 4 + C]  -> cx,cy,w,h,class...
 * 3) [1, 5 + C, N] or [1, 4 + C, N] -> transposed YOLOv8 style.
 *
 * Boxes are returned normalized 0..1 relative to the camera bitmap after reversing letterbox padding.
 * Detection is not faked: if model/shape is unsupported an error is thrown to caller.
 */
class YoloDetector(private val context: Context, private val modelManager: ModelManager) {
    private var interpreter: Interpreter? = null
    private var inputShape: IntArray = intArrayOf(1, 640, 640, 3)
    private var inputType: DataType = DataType.FLOAT32
    private var outputShape: IntArray = intArrayOf()
    private var outputType: DataType = DataType.FLOAT32
    private val confidenceThreshold = 0.35f
    private val iouThreshold = 0.45f

    fun init(): Result<Unit> = runCatching {
        val path = modelManager.modelPath ?: error("Model not loaded")
        val modelBytes = if (path.startsWith("asset:")) {
            context.assets.open(path.removePrefix("asset:")).readBytes()
        } else {
            File(path).readBytes()
        }
        require(modelBytes.isNotEmpty()) { "model.tflite is empty" }
        val modelBuffer = ByteBuffer.allocateDirect(modelBytes.size).order(ByteOrder.nativeOrder())
        modelBuffer.put(modelBytes).rewind()
        interpreter?.close()
        interpreter = Interpreter(modelBuffer, Interpreter.Options().apply { setNumThreads(4) })
        val intr = interpreter ?: error("Interpreter did not initialize")
        inputShape = intr.getInputTensor(0).shape()
        inputType = intr.getInputTensor(0).dataType()
        outputShape = intr.getOutputTensor(0).shape()
        outputType = intr.getOutputTensor(0).dataType()
        require(inputShape.size == 4) { "Unsupported input shape: ${inputShape.joinToString()}" }
        require(outputShape.size == 3) { "Unsupported output shape: ${outputShape.joinToString()}" }
    }

    fun close() { interpreter?.close(); interpreter = null }

    fun detect(bitmap: Bitmap, frameId: Long, sessionId: Long): List<DetectionResult> {
        val intr = interpreter ?: error("Model not loaded")
        val labels = modelManager.labels
        require(labels.isNotEmpty()) { "Labels not loaded" }
        val prepared = preprocessLetterboxed(bitmap)
        val rows = runInferenceToRows(intr, prepared.inputBuffer, outputShape)
        return postProcess(rows, labels, frameId, sessionId, prepared)
    }

    private data class PreparedInput(
        val inputBuffer: ByteBuffer,
        val inputWidth: Int,
        val inputHeight: Int,
        val scale: Float,
        val padX: Float,
        val padY: Float,
        val sourceWidth: Int,
        val sourceHeight: Int
    )

    private fun preprocessLetterboxed(bitmap: Bitmap): PreparedInput {
        val inputHeight = inputShape[1]
        val inputWidth = inputShape[2]
        val scale = min(inputWidth.toFloat() / bitmap.width, inputHeight.toFloat() / bitmap.height)
        val resizedW = max(1, (bitmap.width * scale).toInt())
        val resizedH = max(1, (bitmap.height * scale).toInt())
        val resized = Bitmap.createScaledBitmap(bitmap, resizedW, resizedH, true)
        val canvasBitmap = Bitmap.createBitmap(inputWidth, inputHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(canvasBitmap)
        canvas.drawColor(Color.rgb(114, 114, 114))
        val padX = (inputWidth - resizedW) / 2f
        val padY = (inputHeight - resizedH) / 2f
        canvas.drawBitmap(resized, padX, padY, null)

        val bytesPerChannel = if (inputType == DataType.FLOAT32) 4 else 1
        val buffer = ByteBuffer.allocateDirect(1 * inputWidth * inputHeight * 3 * bytesPerChannel).order(ByteOrder.nativeOrder())
        val pixels = IntArray(inputWidth * inputHeight)
        canvasBitmap.getPixels(pixels, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        pixels.forEach { p ->
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            if (inputType == DataType.FLOAT32) {
                buffer.putFloat(r / 255f); buffer.putFloat(g / 255f); buffer.putFloat(b / 255f)
            } else {
                buffer.put(r.toByte()); buffer.put(g.toByte()); buffer.put(b.toByte())
            }
        }
        buffer.rewind()
        return PreparedInput(buffer, inputWidth, inputHeight, scale, padX, padY, bitmap.width, bitmap.height)
    }

    private fun runInferenceToRows(intr: Interpreter, input: ByteBuffer, shape: IntArray): Array<FloatArray> {
        val a = shape[0]; val b = shape[1]; val c = shape[2]
        require(a == 1) { "Only batch size 1 is supported" }
        val tensor = if (outputType == DataType.FLOAT32) {
            val out = Array(a) { Array(b) { FloatArray(c) } }
            intr.run(input, out)
            out[0]
        } else {
            val out = Array(a) { Array(b) { ByteArray(c) } }
            intr.run(input, out)
            val q = intr.getOutputTensor(0).quantizationParams()
            Array(b) { i -> FloatArray(c) { j -> ((out[0][i][j].toInt() and 0xFF) - q.zeroPoint) * q.scale } }
        }
        return if (b >= c) tensor else Array(c) { col -> FloatArray(b) { row -> tensor[row][col] } }
    }

    private fun postProcess(raw: Array<FloatArray>, labels: List<String>, frameId: Long, sessionId: Long, p: PreparedInput): List<DetectionResult> {
        val ts = System.currentTimeMillis()
        val candidates = raw.mapNotNull { decodeRow(it, labels, ts, frameId, sessionId, p) }
        return NmsUtils.suppress(candidates, iouThreshold)
    }

    private fun decodeRow(row: FloatArray, labels: List<String>, ts: Long, frameId: Long, sessionId: Long, p: PreparedInput): DetectionResult? {
        if (row.size < 5) return null
        val withObj = row.size >= 5 + labels.size
        val classStart = if (withObj) 5 else 4
        if (row.size <= classStart) return null
        val clsIdx = (classStart until row.size).maxByOrNull { row[it] }?.minus(classStart) ?: return null
        val classScore = row[classStart + clsIdx]
        val score = if (withObj) row[4] * classScore else classScore
        if (!score.isFinite() || score < confidenceThreshold) return null

        val cxModel = toModelCoord(row[0], p.inputWidth)
        val cyModel = toModelCoord(row[1], p.inputHeight)
        val wModel = toModelCoord(row[2], p.inputWidth)
        val hModel = toModelCoord(row[3], p.inputHeight)

        val leftSrc = ((cxModel - wModel / 2f - p.padX) / p.scale).coerceIn(0f, p.sourceWidth.toFloat())
        val topSrc = ((cyModel - hModel / 2f - p.padY) / p.scale).coerceIn(0f, p.sourceHeight.toFloat())
        val rightSrc = ((cxModel + wModel / 2f - p.padX) / p.scale).coerceIn(0f, p.sourceWidth.toFloat())
        val bottomSrc = ((cyModel + hModel / 2f - p.padY) / p.scale).coerceIn(0f, p.sourceHeight.toFloat())
        if (rightSrc <= leftSrc || bottomSrc <= topSrc) return null

        val box = RectF(leftSrc / p.sourceWidth, topSrc / p.sourceHeight, rightSrc / p.sourceWidth, bottomSrc / p.sourceHeight)
        return DetectionResult(labels.getOrElse(clsIdx) { "unknown" }, null, score, box, null, ts, null, frameId, sessionId)
    }

    private fun toModelCoord(v: Float, size: Int): Float = if (v <= 1.5f) v * size else v
}
