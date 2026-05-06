package com.company.visualinventory

import com.company.visualinventory.data.InventoryItem
import com.company.visualinventory.export.CsvExporter
import com.company.visualinventory.export.PdfExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class AiAndExportTests {

    private fun sampleItems() = listOf(
        InventoryItem(
            sessionId  = 1L,
            label      = "Apple",
            category   = "Fruit",
            confidence = 0.95f,
            timestamp  = 1000L,
            incomplete = false
        ),
        InventoryItem(
            sessionId  = 1L,
            label      = "Banana",
            category   = null,
            confidence = 0.80f,
            timestamp  = 2000L,
            incomplete = null
        )
    )

    // ── CSV: pure Kotlin, no Android APIs ─────────────────────────────────

    @Test
    fun csvExport_fileIsCreatedAndNotEmpty() {
        val file = File.createTempFile("inv_test", ".csv")
        CsvExporter().export(sampleItems(), file)
        assertTrue("CSV file should exist", file.exists())
        assertTrue("CSV file should not be empty", file.length() > 0)
    }

    @Test
    fun csvExport_firstLineIsHeader() {
        val file = File.createTempFile("inv_test", ".csv")
        CsvExporter().export(sampleItems(), file)
        val firstLine = file.readLines().first()
        assertEquals("label,category,confidence,incomplete,timestamp", firstLine)
    }

    @Test
    fun csvExport_rowCountMatchesItems() {
        val file = File.createTempFile("inv_test", ".csv")
        CsvExporter().export(sampleItems(), file)
        // header + 2 data rows = 3 lines
        assertEquals(3, file.readLines().size)
    }

    @Test
    fun csvExport_emptyList_onlyHeader() {
        val file = File.createTempFile("inv_test", ".csv")
        CsvExporter().export(emptyList(), file)
        val lines = file.readLines()
        assertEquals(1, lines.size)
        assertEquals("label,category,confidence,incomplete,timestamp", lines[0])
    }

    @Test
    fun csvExport_nullCategory_writesEmptyString() {
        val file = File.createTempFile("inv_test", ".csv")
        CsvExporter().export(sampleItems(), file)
        val bananaRow = file.readLines()[2]
        val fields = bananaRow.split(",")
        assertEquals("", fields[1])   // category column should be blank
    }

    // ── InventoryItem: pure data class, no Android APIs ───────────────────

    @Test
    fun inventoryItem_defaultIdIsZero() {
        val item = InventoryItem(
            sessionId  = 5L,
            label      = "Box",
            category   = "Storage",
            confidence = 0.99f,
            timestamp  = 9999L,
            incomplete = false
        )
        assertEquals(0L, item.id)
    }

    @Test
    fun inventoryItem_nullableFieldsAcceptNull() {
        val item = InventoryItem(
            sessionId  = 2L,
            label      = "Unknown",
            category   = null,
            confidence = 0.50f,
            timestamp  = 0L,
            incomplete = null
        )
        assertEquals(null, item.category)
        assertEquals(null, item.incomplete)
    }

    // ── PDF: smoke test only (Robolectric not required) ───────────────────
    // PdfDocument uses Android framework stubs in unit-test JVM.
    // We only verify the call does not throw an unexpected exception;
    // content validation belongs in instrumented tests.

    @Test
    fun pdfExport_doesNotThrow() {
        val file = File.createTempFile("inv_test", ".pdf")
        try {
            PdfExporter().export(
                items     = sampleItems(),
                file      = file,
                sessionId = 1L
            )
        } catch (e: Exception) {
            // Android stub throws UnsupportedOperationException for PdfDocument
            // in the JVM unit-test environment — that is expected and acceptable.
            val msg = e.message ?: ""
            assertTrue(
                "Only Android stub exceptions are tolerated, got: $msg",
                e is UnsupportedOperationException || msg.contains("not mocked", ignoreCase = true)
            )
        }
    }
}
