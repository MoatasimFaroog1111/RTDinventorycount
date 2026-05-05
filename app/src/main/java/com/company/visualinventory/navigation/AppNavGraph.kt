package com.company.visualinventory.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.company.visualinventory.ui.camera.CameraScreen
import com.company.visualinventory.ui.export.ExportScreen
import com.company.visualinventory.ui.home.HomeScreen
import com.company.visualinventory.ui.reports.ReportsScreen
import com.company.visualinventory.ui.settings.ModelSettingsScreen
import com.company.visualinventory.ui.upload.UploadScreen

@Composable
fun AppNavGraph() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "home") {
        composable("home") { HomeScreen(nav) }
        composable("camera") { CameraScreen(nav) }
        composable("upload") { UploadScreen() }
        composable("reports") { ReportsScreen() }
        composable("export") { ExportScreen() }
        composable("settings") { ModelSettingsScreen() }
    }
}
