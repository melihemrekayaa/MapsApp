package com.example.mapsapp.model

data class OpenRouterResponse(
    val choices: List<OpenRouterChoice>
)

data class OpenRouterChoice(
    val message: OpenRouterMessage
)

