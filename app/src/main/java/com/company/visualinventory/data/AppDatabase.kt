package com.company.visualinventory.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [InventorySession::class, InventoryItem::class], version = 1)
abstract class AppDatabase : RoomDatabase() { abstract fun dao(): InventoryDao }
