package com.company.visualinventory.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class InventorySession(@PrimaryKey(autoGenerate = true) val id: Long = 0, val createdAt: Long)
