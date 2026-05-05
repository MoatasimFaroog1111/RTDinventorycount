package com.company.visualinventory.ai

data class CountSummary(
    val total: Int,
    val byLabel: Map<String, Int>,
    val incomplete: Int,
    val confidenceAvg: Map<String, Float>
)

class InventoryCounter {
    private val seen = mutableSetOf<Int>()

    fun summarize(detections: List<DetectionResult>): CountSummary {
        val unique = detections.filter { detection ->
            val id = detection.trackingId
            id != null && seen.add(id)
        }
        val byLabel = unique.groupingBy { it.label }.eachCount()
        val incomplete = unique.count { it.isIncompleteUnit == true }
        val avg = unique.groupBy { it.label }.mapValues { (_, values) ->
            values.map { it.confidence }.average().toFloat()
        }
        return CountSummary(unique.size, byLabel, incomplete, avg)
    }

    fun reset() { seen.clear() }
}
