package com.company.visualinventory.ui.upload

import android.app.Application
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.company.visualinventory.ai.ModelManager

@Composable
fun UploadScreen() {
    val context = LocalContext.current.applicationContext as Application
    val manager = remember { ModelManager(context) }
    val status = remember { mutableStateOf("Model not loaded. Select labels.txt and model.tflite.") }

    val labelsPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            status.value = "No labels file selected."
        } else {
            manager.loadLabels(uri)
                .onSuccess { status.value = "labels.txt loaded: ${manager.labels.size} labels" }
                .onFailure { status.value = "Labels load failed: ${it.message}" }
        }
    }

    val modelPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) {
            status.value = "No model file selected."
        } else {
            manager.copyModelToInternal(uri)
                .onSuccess { status.value = "model.tflite copied internally. Return to Camera to scan." }
                .onFailure { status.value = "Model load failed: ${it.message}" }
        }
    }

    Column {
        Text(status.value)
        Button(onClick = { labelsPicker.launch(arrayOf("text/plain", "application/octet-stream", "*/*")) }) {
            Text("Select labels.txt")
        }
        Button(onClick = { modelPicker.launch(arrayOf("application/octet-stream", "*/*")) }) {
            Text("Select model.tflite")
        }
        Text("The app will not fake detections. Camera scanning remains disabled until a valid model is loaded.")
    }
}
