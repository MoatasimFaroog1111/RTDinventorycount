package com.company.visualinventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InventoryItem(@PrimaryKey(autoGenerate = true) val id: Long = 0, val sessionId: Long, val label: String, val category: String?, val confidence: Float, val timestamp: Long, val incomplete: Boolean?)
