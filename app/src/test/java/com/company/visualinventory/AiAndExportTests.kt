package com.company.visualinventory

import com.company.visualinventory.data.InventoryItem
import com.company.visualinventory.export.PdfExporter
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AiAndExportTests {

    @Test
    fun pdfExportWritesPdfHeader() {
        val tmpFile = File.createTempFile("test", ".pdf")

        val items = listOf(
            InventoryItem(label = "Item A", confidence = 0.95f, sessionId = 1L),
            InventoryItem(label = "Item B", confidence = 0.87f, sessionId = 1L)
        )

        val exporter = PdfExporter()
        exporter.export(
            items = items,
            file = tmpFile,
            sessionId = 1L
        )

        assertTrue(tmpFile.exists())
        assertTrue(tmpFile.length() > 0)
    }
}
