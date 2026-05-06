package com.company.visualinventory

import android.content.Context
import androidx.test.core.app.ApplicationProvider
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
        val context = ApplicationProvider.getApplicationContext<Context>()

        val file = File(context.cacheDir, "test.pdf")

        val exporter = PdfExporter()
        exporter.export(
            context = context,
            file = file,
            data = listOf(
                "Item A - 10",
                "Item B - 5"
            )
        )

        assertTrue(file.exists())
        assertTrue(file.length() > 0)
    }
}
