package com.company.visualinventory.ui.camera

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.company.visualinventory.ai.DetectionResult
import com.company.visualinventory.ai.InventoryCounter
import com.company.visualinventory.ai.ModelManager
import com.company.visualinventory.ai.YoloDetector
import com.company.visualinventory.data.AppDatabase
import com.company.visualinventory.data.InventoryItem
import com.company.visualinventory.data.InventoryRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CameraViewModel(app: Application) : AndroidViewModel(app) {
    private val db = Room.databaseBuilder(app, AppDatabase::class.java, "inventory.db").build()
    private val repo = InventoryRepository(db.dao())
    private val modelManager = ModelManager(app)
    val detector = YoloDetector(app, modelManager)
    private val counter = InventoryCounter()
    var sessionId: Long = 0
        private set

    private val persistedTrackIds = mutableSetOf<Int>()
    private val _warning = MutableStateFlow<String?>(null)
    val warning: StateFlow<String?> = _warning.asStateFlow()
    private val _count = MutableStateFlow(0)
    val count: StateFlow<Int> = _count.asStateFlow()
    private val _scanning = MutableStateFlow(false)
    val scanning: StateFlow<Boolean> = _scanning.asStateFlow()
    private val _modelStatus = MutableStateFlow("Model not loaded")
    val modelStatus: StateFlow<String> = _modelStatus.asStateFlow()
    private val _detections = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detections: StateFlow<List<DetectionResult>> = _detections.asStateFlow()

    init { prepareSessionAndModel() }

    private fun prepareSessionAndModel() {
        viewModelScope.launch {
            sessionId = repo.createSession()
            val load = modelManager.loadBestAvailable()
            load.onSuccess {
                detector.init().onSuccess {
                    _modelStatus.value = "Model loaded"
                    _warning.value = null
                }.onFailure {
                    _modelStatus.value = "Model not loaded"
                    _warning.value = "Model load failure: ${it.message}"
                }
            }.onFailure {
                _modelStatus.value = "Model not loaded"
                _warning.value = it.message ?: "Missing model.tflite or labels.txt"
            }
        }
    }

    fun startScanning() {
        if (_modelStatus.value != "Model loaded") {
            _warning.value = "Model not loaded. Upload model.tflite and labels.txt before scanning."
            _scanning.value = false
            return
        }
        _scanning.value = true
    }

    fun stopScanning() { _scanning.value = false; _detections.value = emptyList() }

    fun isScanning(): Boolean = _scanning.value

    fun onAnalyzerError(message: String) { _warning.value = message }

    fun onDetections(list: List<DetectionResult>) {
        if (!_scanning.value) return
        _detections.value = list
        val summary = counter.summarize(list)
        if (summary.total > 0) _count.value += summary.total
        viewModelScope.launch {
            list.filter { it.trackingId != null && persistedTrackIds.add(it.trackingId!!) }.forEach {
                repo.saveItem(
                    InventoryItem(
                        sessionId = sessionId,
                        label = it.label,
                        category = it.category,
                        confidence = it.confidence,
                        timestamp = it.timestamp,
                        incomplete = it.isIncompleteUnit
                    )
                )
            }
        }
    }

    override fun onCleared() {
        detector.close()
        super.onCleared()
    }
}
