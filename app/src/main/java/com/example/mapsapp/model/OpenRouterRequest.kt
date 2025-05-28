package com.example.mapsapp.model

data class OpenRouterRequest(
    val model: String = "mistralai/mixtral-8x7b",
    val messages: List<OpenRouterMessage>
)

data class OpenRouterMessage(
    val role: String,
    val content: String
)
