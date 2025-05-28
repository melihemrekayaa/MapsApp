package com.example.mapsapp.manager

import android.content.Context
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.TextPart

class GeminiChatService(context: Context) {

    companion object {
        private const val TAG = "GeminiChatService"
    }

    private val chat: Chat

    init {
        val backend = GenerativeBackend.googleAI()
        Log.d(TAG, "Using backend: $backend") // ✅ LOG: hangi backend kullanılıyor?

        val generativeModel = Firebase.ai(
            backend = backend
        ).generativeModel(
            modelName = "gemini-2.0-flash"
        )

        Log.d(TAG, "Generative model initialized with gemini-2.0-flash")
        chat = generativeModel.startChat()
        Log.d(TAG, "Chat session started")
    }

    suspend fun sendMessage(userMessage: String): String {
        Log.d(TAG, "Sending message: $userMessage")

        val prompt = Content.Builder()
            .text(userMessage)
            .build()

        return try {
            val response = chat.sendMessage(prompt) // ❌ await yok artık
            Log.d(TAG, "Response received")

            val result = response.candidates.firstOrNull()?.content
                ?.parts?.filterIsInstance<TextPart>()
                ?.joinToString("\n") { it.text }
                ?: "Yanıt alınamadı"

            Log.d(TAG, "Parsed response: $result")
            result
        } catch (e: Exception) {
            Log.e(TAG, "Error while sending message", e)
            "Hata: ${e.localizedMessage}"
        }
    }

}
