package com.company.visualinventory.ui.export

import android.app.Application
import android.content.Intent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import androidx.room.Room
import com.company.visualinventory.data.AppDatabase
import com.company.visualinventory.data.InventoryRepository
import com.company.visualinventory.export.CsvExporter
import com.company.visualinventory.export.ExcelExporter
import com.company.visualinventory.export.PdfExporter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun ExportScreen() {
    val status = remember { mutableStateOf("Export the latest saved scan session.") }
    val context = LocalContext.current
    val app = context.applicationContext as Application
    val exported = remember { mutableStateOf<List<File>>(emptyList()) }
    Column {
        Button(onClick = {
            CoroutineScope(Dispatchers.IO).launch {
                runCatching {
                    val db = Room.databaseBuilder(app, AppDatabase::class.java, "inventory.db").build()
                    val repo = InventoryRepository(db.dao())
                    val session = repo.sessions().firstOrNull() ?: error("No saved scan session found")
                    val items = repo.items(session.id)
                    val csv = File(app.filesDir, "inventory_${session.id}.csv")
                    val xlsx = File(app.filesDir, "inventory_${session.id}.xlsx")
                    val pdf = File(app.filesDir, "inventory_${session.id}.pdf")
                    CsvExporter().export(items, csv)
                    ExcelExporter().export(items, xlsx)
                    PdfExporter().export(items, pdf, session.id)
                    exported.value = listOf(csv, xlsx, pdf)
                    status.value = "Exported session ${session.id}: CSV, XLSX, PDF"
                }.onFailure { status.value = "Export failed: ${it.message}" }
            }
        }) { Text("Export latest scan report") }
        Button(onClick = {
            val files = exported.value
            if (files.isEmpty()) {
                status.value = "Export first, then share files."
            } else {
                val uris = ArrayList(files.map { FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", it) })
                val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
                    type = "*/*"
                    putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Share inventory export"))
            }
        }) { Text("Share/Open exported files") }
        Text(status.value)
    }
}
