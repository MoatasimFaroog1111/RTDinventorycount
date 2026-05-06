package com.company.visualinventory.ui.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.company.visualinventory.ai.IncompleteUnitDetector
import com.company.visualinventory.ai.ObjectTracker
import com.company.visualinventory.camera.CameraFrameAnalyzer
import java.util.concurrent.Executors

@Composable
fun CameraScreen(nav: NavController, vm: CameraViewModel = viewModel()) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current
    val count by vm.count.collectAsState()
    val warning by vm.warning.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val modelStatus by vm.modelStatus.collectAsState()
    val detections by vm.detections.collectAsState()
    val executor = remember { Executors.newSingleThreadExecutor() }
    var hasPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    DisposableEffect(Unit) { onDispose { executor.shutdown() } }

    Column {
        if (hasPermission) {
            Box(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                AndroidView(
                    factory = {
                        val previewView = PreviewView(it)
                        val providerFuture = ProcessCameraProvider.getInstance(it)
                        providerFuture.addListener({
                            val provider = providerFuture.get()
                            val preview = Preview.Builder().build().also { p -> p.setSurfaceProvider(previewView.surfaceProvider) }
                            val analysis = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                            analysis.setAnalyzer(
                                executor,
                                CameraFrameAnalyzer(
                                    detector = vm.detector,
                                    tracker = ObjectTracker(),
                                    incompleteUnitDetector = IncompleteUnitDetector(),
                                    sessionIdProvider = { vm.sessionId },
                                    shouldAnalyze = { vm.isScanning() },
                                    onDetections = vm::onDetections,
                                    onError = vm::onAnalyzerError
                                )
                            )
                            provider.unbindAll()
                            provider.bindToLifecycle(lifecycle, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
                        }, ContextCompat.getMainExecutor(it))
                        previewView
                    },
                    modifier = Modifier.fillMaxWidth().height(360.dp)
                )
                Canvas(modifier = Modifier.fillMaxWidth().height(360.dp)) {
                    detections.forEach { det ->
                        val b = det.boundingBox
                        drawRect(
                            color = Color.Green,
                            topLeft = Offset(b.left * size.width, b.top * size.height),
                            size = Size(b.width() * size.width, b.height() * size.height),
                            style = Stroke(width = 3f)
                        )
                    }
                }
            }
        } else {
            Text("Camera permission is required to scan inventory.")
        }
        Text("Model status: $modelStatus")
        Row {
            Button(onClick = vm::startScanning) { Text("Start Scanning") }
            Button(onClick = vm::stopScanning) { Text("Stop Scanning") }
        }
        Button(onClick = { nav.navigate("reports") }) { Text("Report") }
        Button(onClick = { nav.navigate("export") }) { Text("Export") }
        Text("Scanning: ${if (scanning) "ON" else "OFF"}")
        Text("Unique persisted detected count: $count")
        if (warning != null) Text("Warning: $warning")
    }
}
