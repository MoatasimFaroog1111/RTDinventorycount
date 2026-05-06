package com.company.visualinventory.ui.reports

import android.app.Application
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.room.Room
import com.company.visualinventory.data.AppDatabase
import com.company.visualinventory.data.InventoryRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class ReportRow(val title: String, val lines: List<String>)

@Composable
fun ReportsScreen() {
    val rows = remember { mutableStateListOf<ReportRow>() }
    val context = LocalContext.current.applicationContext as Application
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val db = Room.databaseBuilder(context, AppDatabase::class.java, "inventory.db").build()
            val repo = InventoryRepository(db.dao())
            val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
            rows.clear()
            repo.sessions().forEach { s ->
                val items = repo.items(s.id)
                val grouped = items.groupBy { it.label }
                val lines = mutableListOf<String>()
                lines += "Timestamp: ${fmt.format(Date(s.createdAt))}"
                lines += "Total rows: ${items.size}"
                lines += "Incomplete units: ${items.count { it.incomplete == true }}"
                grouped.forEach { (label, list) ->
                    lines += "$label | count=${list.size} | avg=${"%.2f".format(list.map { it.confidence }.average())} | incomplete=${list.count { it.incomplete == true }}"
                }
                rows += ReportRow("Session ${s.id}", lines)
            }
        }
    }
    Column {
        Text("Inventory Sessions")
        LazyColumn {
            items(rows) { row ->
                Card { Column { Text(row.title); row.lines.forEach { Text(it) } } }
            }
        }
    }
}
