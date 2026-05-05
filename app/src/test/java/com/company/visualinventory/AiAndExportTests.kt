package com.company.visualinventory

import android.graphics.RectF
import com.company.visualinventory.ai.DetectionResult
import com.company.visualinventory.ai.InventoryCounter
import com.company.visualinventory.ai.IoUUtils
import com.company.visualinventory.ai.NmsUtils
import com.company.visualinventory.data.InventoryItem
import com.company.visualinventory.export.CsvExporter
import com.company.visualinventory.export.PdfExporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AiAndExportTests {
    @Test fun nmsRemovesOverlap() {
        val now = 1L
        val a = DetectionResult("box", confidence = 0.9f, boundingBox = RectF(0f,0f,1f,1f), timestamp = now, frameId = 1, sessionId = 1)
        val b = DetectionResult("box", confidence = 0.8f, boundingBox = RectF(0f,0f,1f,1f), timestamp = now, frameId = 1, sessionId = 1)
        assertEquals(1, NmsUtils.suppress(listOf(a,b), 0.5f).size)
    }

    @Test fun iouWorks() {
        assertTrue(IoUUtils.iou(RectF(0f,0f,1f,1f), RectF(0f,0f,1f,1f)) > 0.99f)
    }

    @Test fun uniqueCounting() {
        val c = InventoryCounter()
        val list = listOf(
            DetectionResult("box", confidence = 0.9f, boundingBox = RectF(), trackingId = 1, timestamp = 1, frameId = 1, sessionId = 1),
            DetectionResult("box", confidence = 0.8f, boundingBox = RectF(), trackingId = 1, timestamp = 1, frameId = 2, sessionId = 1)
        )
        assertEquals(1, c.summarize(list).total)
    }

    @Test fun csvExportWritesHeader() {
        val f = File.createTempFile("inv", ".csv")
        CsvExporter().export(emptyList(), f)
        assertTrue(f.readText().contains("label,category,confidence,incomplete,timestamp"))
    }

    @Test fun pdfExportWritesPdfHeader() {
        val f = File.createTempFile("inv", ".pdf")
        PdfExporter().export(listOf(InventoryItem(sessionId = 1, label = "box", category = null, confidence = 0.9f, timestamp = 1, incomplete = false)), f)
        assertTrue(f.readBytes().take(4).toByteArray().decodeToString().contains("%PDF"))
    }
}
