package com.example.mapsapp.memory

import com.example.mapsapp.model.BotMessage

object MemoryManager {
    private val memoryMap = mutableMapOf<String, MutableList<BotMessage>>()

    fun addMessage(userId: String, message: BotMessage) {
        if (userId !in memoryMap) {
            memoryMap[userId] = mutableListOf()
        }
        memoryMap[userId]?.add(message)
    }

    fun getLastMessages(userId: String, count: Int): List<BotMessage> {
        return memoryMap[userId]?.takeLast(count) ?: emptyList()
    }

    fun resetMemory(userId: String) {
        memoryMap[userId]?.clear()
    }
}
