package com.company.visualinventory.export

import com.company.visualinventory.data.InventoryItem
import java.io.File

class CsvExporter {
    fun export(items: List<InventoryItem>, file: File) {
        val header = "label,category,confidence,incomplete,timestamp\n"
        val rows = items.joinToString("\n") {
            listOf(it.label, it.category ?: "", it.confidence.toString(), it.incomplete?.toString() ?: "unknown", it.timestamp.toString())
                .joinToString(",") { value -> value.replace(",", " ") }
        }
        file.writeText(header + rows)
    }
}
