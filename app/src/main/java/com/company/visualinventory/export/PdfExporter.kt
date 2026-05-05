package com.company.visualinventory.export

import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.company.visualinventory.data.InventoryItem
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter {
    fun export(items: List<InventoryItem>, file: File, sessionId: Long = 0L) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val margin = 36f
        val titlePaint = Paint().apply { textSize = 18f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 11f; isFakeBoldText = true }
        val textPaint = Paint().apply { textSize = 10f }
        val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date())
        val grouped = items.groupBy { it.label }
        val summary = "Session: $sessionId | Date: $date | Total unique rows: ${items.size} | Labels: ${grouped.size} | Incomplete: ${items.count { it.incomplete == true }}"

        var pageNo = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
        var canvas = page.canvas
        var y = margin

        fun newPage() {
            document.finishPage(page)
            pageNo += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNo).create())
            canvas = page.canvas
            y = margin
            canvas.drawText("AI Visual Inventory Report - Page $pageNo", margin, y, titlePaint)
            y += 30f
        }

        canvas.drawText("AI Visual Inventory Report", margin, y, titlePaint); y += 28f
        canvas.drawText(summary, margin, y, textPaint); y += 28f
        canvas.drawText("Label", margin, y, headerPaint)
        canvas.drawText("Count", 220f, y, headerPaint)
        canvas.drawText("Avg Conf.", 280f, y, headerPaint)
        canvas.drawText("Incomplete", 360f, y, headerPaint)
        y += 18f
        grouped.forEach { (label, rows) ->
            if (y > pageHeight - 60) newPage()
            canvas.drawText(label.take(28), margin, y, textPaint)
            canvas.drawText(rows.size.toString(), 220f, y, textPaint)
            canvas.drawText("%.2f".format(rows.map { it.confidence }.average()), 280f, y, textPaint)
            canvas.drawText(rows.count { it.incomplete == true }.toString(), 360f, y, textPaint)
            y += 16f
        }
        y += 20f
        canvas.drawText("Detailed Rows", margin, y, headerPaint); y += 18f
        items.forEach { item ->
            if (y > pageHeight - 50) newPage()
            canvas.drawText("${item.label.take(24)} | ${"%.2f".format(item.confidence)} | incomplete=${item.incomplete ?: "unknown"} | ${item.timestamp}", margin, y, textPaint)
            y += 15f
        }
        document.finishPage(page)
        file.outputStream().use { document.writeTo(it) }
        document.close()
    }
}
