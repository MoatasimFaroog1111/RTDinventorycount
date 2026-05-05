package com.company.visualinventory.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

/** Converts CameraX YUV_420_888 frames to Bitmap while respecting rowStride and pixelStride. */
class YuvToRgbConverter {
    fun toBitmap(image: ImageProxy): Bitmap {
        val nv21 = yuv420888ToNv21(image)
        val yuv = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuv.compressToJpeg(Rect(0, 0, image.width, image.height), 92, out)
        return BitmapFactory.decodeByteArray(out.toByteArray(), 0, out.size())
            ?: error("Failed to decode YUV camera frame")
    }

    private fun yuv420888ToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val ySize = width * height
        val uvSize = width * height / 2
        val out = ByteArray(ySize + uvSize)
        val planes = image.planes

        copyPlane(planes[0].buffer, width, height, planes[0].rowStride, planes[0].pixelStride, out, 0, 1)
        // NV21 interleaves V then U after Y plane.
        copyPlane(planes[2].buffer, width / 2, height / 2, planes[2].rowStride, planes[2].pixelStride, out, ySize, 2)
        copyPlane(planes[1].buffer, width / 2, height / 2, planes[1].rowStride, planes[1].pixelStride, out, ySize + 1, 2)
        return out
    }

    private fun copyPlane(
        buffer: java.nio.ByteBuffer,
        width: Int,
        height: Int,
        rowStride: Int,
        pixelStride: Int,
        output: ByteArray,
        offset: Int,
        outputStride: Int
    ) {
        val row = ByteArray(rowStride)
        var outputOffset = offset
        for (y in 0 until height) {
            val bytesPerRow = if (pixelStride == 1 && outputStride == 1) width else rowStride
            val length = minOf(bytesPerRow, buffer.remaining())
            buffer.get(row, 0, length)
            if (pixelStride == 1 && outputStride == 1) {
                System.arraycopy(row, 0, output, outputOffset, width)
                outputOffset += width
            } else {
                for (x in 0 until width) {
                    val idx = x * pixelStride
                    if (idx < length && outputOffset < output.size) {
                        output[outputOffset] = row[idx]
                        outputOffset += outputStride
                    }
                }
            }
            val skip = rowStride - length
            if (skip > 0 && buffer.remaining() >= skip) buffer.position(buffer.position() + skip)
        }
    }
}
