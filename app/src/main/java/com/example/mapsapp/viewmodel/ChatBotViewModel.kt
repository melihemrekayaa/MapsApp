package com.example.mapsapp.viewmodel

import androidx.lifecycle.*
import com.example.mapsapp.model.*
import com.example.mapsapp.repository.ChatRepository
import com.example.mapsapp.service.OpenRouterApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatBotViewModel @Inject constructor(
    private val repository: ChatRepository,
    private val openRouterApi: OpenRouterApi
) : ViewModel() {

    private val _messages = MutableLiveData<List<ChatMessage>>()
    val messages: LiveData<List<ChatMessage>> = _messages

    fun listenMessages(userId: String) {
        repository.listenMessages(userId) {
            _messages.postValue(it)
        }
    }

    fun sendUserMessage(userId: String, text: String) {
        val userMessage = ChatMessage(text = text, isUser = true)
        repository.sendMessage(userId, userMessage)

        viewModelScope.launch {
            try {
                val request = OpenRouterRequest(
                    model = "openai/gpt-3.5-turbo",
                    messages = listOf(OpenRouterMessage("user", text))
                )

                val response = openRouterApi.sendMessage(request)
                val reply = response.choices.firstOrNull()?.message?.content

                if (!reply.isNullOrEmpty()) {
                    val aiMessage = ChatMessage(
                        text = reply,
                        isUser = false,
                        isAnimated = true
                    )
                    repository.sendMessage(userId, aiMessage)
                }
            } catch (e: Exception) {
                val errorMessage = ChatMessage(
                    text = "Hata: ${e.localizedMessage ?: "timeout"}",
                    isUser = false
                )
                repository.sendMessage(userId, errorMessage)
            }
        }
    }


    fun clearChat(userId: String) {
        repository.clearMessages(userId) {
            _messages.value = emptyList()
        }
    }

}
