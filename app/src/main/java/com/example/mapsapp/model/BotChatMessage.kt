package com.example.mapsapp.model

data class BotChatMessage(
    val message: String = "",
    val sender: String = "", // "user" veya "ai"
    val timestamp: String? = null
)
