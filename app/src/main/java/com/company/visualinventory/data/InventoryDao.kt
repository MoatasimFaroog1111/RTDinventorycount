package com.company.visualinventory.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface InventoryDao {
 @Insert suspend fun insertSession(session: InventorySession): Long
 @Insert suspend fun insertItem(item: InventoryItem)
 @Query("SELECT * FROM InventorySession ORDER BY createdAt DESC") suspend fun sessions(): List<InventorySession>
 @Query("SELECT * FROM InventoryItem WHERE sessionId = :sid") suspend fun items(sid: Long): List<InventoryItem>
}
