package com.company.visualinventory.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun ModelSettingsScreen() {
    Column {
        Text("Model Settings")
        Text("Required model file: model.tflite")
        Text("Required labels file: labels.txt")
        Text("Labels must match the model class order exactly. Detection is disabled until both are loaded.")
        Text("Use Upload Label File screen to import files into app internal storage.")
    }
}
