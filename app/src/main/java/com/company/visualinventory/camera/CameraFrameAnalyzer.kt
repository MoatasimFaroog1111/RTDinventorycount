package com.company.visualinventory.camera

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.company.visualinventory.ai.DetectionResult
import com.company.visualinventory.ai.IncompleteUnitDetector
import com.company.visualinventory.ai.ObjectTracker
import com.company.visualinventory.ai.YoloDetector
import com.company.visualinventory.utils.ImageUtils

class CameraFrameAnalyzer(
    private val detector: YoloDetector,
    private val tracker: ObjectTracker,
    private val incompleteUnitDetector: IncompleteUnitDetector,
    private val sessionIdProvider: () -> Long,
    private val shouldAnalyze: () -> Boolean,
    private val onDetections: (List<DetectionResult>) -> Unit,
    private val onError: (String) -> Unit
) : ImageAnalysis.Analyzer {
    private var frameId = 0L
    private val converter = YuvToRgbConverter()

    override fun analyze(image: ImageProxy) {
        try {
            if (!shouldAnalyze()) return
            frameId++
            val bitmap = converter.toBitmap(image)
            val rotated = ImageUtils.rotate(bitmap, image.imageInfo.rotationDegrees)
            val raw = detector.detect(rotated, frameId, sessionIdProvider())
            val enriched = raw.map { it.copy(isIncompleteUnit = incompleteUnitDetector.detect(it.label, it.boundingBox, it.confidence)) }
            val tracked = tracker.assign(enriched, System.currentTimeMillis())
            onDetections(tracked)
        } catch (t: Throwable) {
            onError(t.message ?: "analysis failed")
        } finally {
            image.close()
        }
    }
}
