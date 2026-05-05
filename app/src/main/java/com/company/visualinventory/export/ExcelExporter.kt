package com.company.visualinventory.export

import com.company.visualinventory.data.InventoryItem
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File

class ExcelExporter {
    fun export(items: List<InventoryItem>, file: File) {
        require(file.extension.equals("xlsx", ignoreCase = true)) { "Excel export must use .xlsx" }
        XSSFWorkbook().use { workbook ->
            val sheet = workbook.createSheet("Inventory")
            val headers = listOf("Label", "Category", "Confidence", "Incomplete", "Timestamp")
            val header = sheet.createRow(0)
            headers.forEachIndexed { i, h -> header.createCell(i).setCellValue(h) }
            items.forEachIndexed { index, item ->
                val row = sheet.createRow(index + 1)
                row.createCell(0).setCellValue(item.label)
                row.createCell(1).setCellValue(item.category ?: "")
                row.createCell(2).setCellValue(item.confidence.toDouble())
                row.createCell(3).setCellValue(item.incomplete?.toString() ?: "unknown")
                row.createCell(4).setCellValue(item.timestamp.toDouble())
            }
            repeat(headers.size) { sheet.autoSizeColumn(it) }
            file.outputStream().use { workbook.write(it) }
        }
    }
}
