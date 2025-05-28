package com.example.mapsapp.model

data class ChatMessage(
    var text: String = "",
    var isUser: Boolean = true,
    var timestamp: Long = System.currentTimeMillis(),
    var isAnimated: Boolean = false
)
