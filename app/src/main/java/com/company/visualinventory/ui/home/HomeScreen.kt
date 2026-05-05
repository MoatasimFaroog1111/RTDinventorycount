package com.company.visualinventory.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.navigation.NavController

@Composable fun HomeScreen(nav: NavController) {
 Column {
  Button({ nav.navigate("camera") }) { Text("Camera") }
  Button({ nav.navigate("export") }) { Text("Export") }
  Button({ nav.navigate("upload") }) { Text("Upload Label File") }
  Button({ nav.navigate("reports") }) { Text("Reports") }
  Button({ nav.navigate("settings") }) { Text("Model Settings") }
 }
}
