package com.company.visualinventory.ai

import android.graphics.RectF
import kotlin.math.max
import kotlin.math.min

class ObjectTracker(private val iouThreshold: Float = 0.4f) {
    private data class Track(val id: Int, var box: RectF, var label: String, var lastSeen: Long)
    private val tracks = mutableListOf<Track>()
    private var nextId = 1

    fun assign(detections: List<DetectionResult>, now: Long): List<DetectionResult> {
        val out = mutableListOf<DetectionResult>()
        detections.forEach { det ->
            val match = tracks.filter { it.label == det.label }.maxByOrNull { iou(it.box, det.boundingBox) }
            val trackedId = if (match != null && iou(match.box, det.boundingBox) >= iouThreshold) {
                match.box = det.boundingBox; match.lastSeen = now; match.id
            } else {
                val t = Track(nextId++, det.boundingBox, det.label, now); tracks.add(t); t.id
            }
            out.add(det.copy(trackingId = trackedId))
        }
        tracks.removeAll { now - it.lastSeen > 2_000 }
        return out
    }

    private fun iou(a: RectF, b: RectF): Float {
        val left = max(a.left, b.left); val top = max(a.top, b.top)
        val right = min(a.right, b.right); val bottom = min(a.bottom, b.bottom)
        if (right <= left || bottom <= top) return 0f
        val inter = (right - left) * (bottom - top)
        val union = a.width() * a.height() + b.width() * b.height() - inter
        return if (union <= 0f) 0f else inter / union
    }
}
