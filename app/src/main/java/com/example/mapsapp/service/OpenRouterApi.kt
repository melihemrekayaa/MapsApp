package com.example.mapsapp.service

import com.example.mapsapp.model.OpenRouterRequest
import com.example.mapsapp.model.OpenRouterResponse
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface OpenRouterApi {
    @Headers("Content-Type: application/json")
    @POST("v1/chat/completions")
    suspend fun sendMessage(
        @Body request: OpenRouterRequest
    ): OpenRouterResponse
}
