package com.company.visualinventory.ai

import android.graphics.RectF

class IncompleteUnitDetector {
    fun detect(label: String, box: RectF, confidence: Float): Boolean? {
        if (confidence < 0.3f) return null
        val ratio = box.width() / box.height().coerceAtLeast(1f)
        return when {
            label.contains("bottle", true) && ratio > 0.8f -> true
            label.contains("box", true) && ratio < 0.5f -> true
            else -> false
        }
    }
}
