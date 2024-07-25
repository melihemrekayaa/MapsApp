package com.example.mapsapp.model

data class BotChatMessage(
    var message: String,
    val isUser: Boolean,
    val timestamp: Long
)
