package com.company.visualinventory.ai

import android.graphics.RectF

data class DetectionResult(
    val label: String,
    val category: String? = null,
    val confidence: Float,
    val boundingBox: RectF,
    val trackingId: Int? = null,
    val timestamp: Long,
    val isIncompleteUnit: Boolean? = null,
    val frameId: Long,
    val sessionId: Long
)
