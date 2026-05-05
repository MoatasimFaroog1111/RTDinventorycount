package com.company.visualinventory.ai

import android.content.Context
import android.net.Uri
import com.company.visualinventory.utils.Constants
import java.io.File

class ModelManager(private val context: Context) {
    var modelPath: String? = null
        private set
    var labels: List<String> = emptyList()
        private set

    private val internalModelFile: File get() = File(context.filesDir, Constants.INTERNAL_MODEL_FILE)
    private val internalLabelsFile: File get() = File(context.filesDir, Constants.INTERNAL_LABELS_FILE)

    fun loadBestAvailable(): Result<Unit> = runCatching {
        when {
            internalModelFile.exists() && internalLabelsFile.exists() -> {
                loadInternalLabels()
                modelPath = internalModelFile.absolutePath
            }
            assetExists(Constants.DEFAULT_MODEL) && assetExists(Constants.DEFAULT_LABELS) -> loadDefaultAssets().getOrThrow()
            else -> error("Model not loaded. Upload model.tflite and labels.txt from Settings/Upload.")
        }
    }

    fun loadDefaultAssets(): Result<Unit> = runCatching {
        require(assetExists(Constants.DEFAULT_MODEL)) { "Missing assets/${Constants.DEFAULT_MODEL}" }
        require(assetExists(Constants.DEFAULT_LABELS)) { "Missing assets/${Constants.DEFAULT_LABELS}" }
        labels = context.assets.open(Constants.DEFAULT_LABELS).bufferedReader().use { reader ->
            reader.readLines().map { it.trim() }.filter { it.isNotBlank() }
        }
        require(labels.isNotEmpty()) { "assets/${Constants.DEFAULT_LABELS} is empty" }
        modelPath = "asset:${Constants.DEFAULT_MODEL}"
    }

    fun loadLabels(uri: Uri): Result<Unit> = runCatching {
        val display = uri.toString().lowercase()
        require(display.contains("labels") || display.endsWith(".txt") || display.contains(".txt")) {
            "Select a labels.txt file"
        }
        val lines = context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
            reader.readLines().map { it.trim() }.filter { it.isNotBlank() }
        } ?: error("labels file unreadable")
        require(lines.isNotEmpty()) { "labels.txt is empty" }
        internalLabelsFile.writeText(lines.joinToString("\n"))
        labels = lines
    }

    fun copyModelToInternal(uri: Uri): Result<String> = runCatching {
        val display = uri.toString().lowercase()
        require(display.contains(".tflite") || display.contains("model")) { "Select a .tflite model file" }
        context.contentResolver.openInputStream(uri)?.use { input ->
            internalModelFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("model file unreadable")
        require(internalModelFile.length() > 1024) { "model.tflite is too small or invalid" }
        modelPath = internalModelFile.absolutePath
        internalModelFile.absolutePath
    }

    fun hasReadyModel(): Boolean = modelPath != null && labels.isNotEmpty()

    private fun loadInternalLabels() {
        labels = internalLabelsFile.readLines().map { it.trim() }.filter { it.isNotBlank() }
        require(labels.isNotEmpty()) { "Uploaded labels.txt is empty" }
    }

    private fun assetExists(name: String): Boolean = runCatching { context.assets.open(name).close() }.isSuccess
}
