package com.company.visualinventory.data

class InventoryRepository(private val dao: InventoryDao) {
 suspend fun createSession() = dao.insertSession(InventorySession(createdAt = System.currentTimeMillis()))
 suspend fun saveItem(item: InventoryItem) = dao.insertItem(item)
 suspend fun sessions() = dao.sessions()
 suspend fun items(sessionId: Long) = dao.items(sessionId)
}
