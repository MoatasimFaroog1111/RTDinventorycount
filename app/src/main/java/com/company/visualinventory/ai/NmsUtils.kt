package com.company.visualinventory.ai

object NmsUtils {
    fun suppress(input: List<DetectionResult>, iouThreshold: Float): List<DetectionResult> {
        val sorted = input.sortedByDescending { it.confidence }.toMutableList()
        val kept = mutableListOf<DetectionResult>()
        while (sorted.isNotEmpty()) {
            val best = sorted.removeAt(0)
            kept.add(best)
            sorted.removeAll { it.label == best.label && IoUUtils.iou(it.boundingBox, best.boundingBox) > iouThreshold }
        }
        return kept
    }
}
