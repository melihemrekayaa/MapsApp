package com.example.mapsapp.view.chatbot

import com.example.mapsapp.memory.MemoryManager
import com.example.mapsapp.model.BotMessage
import com.example.mapsapp.model.ChatRequest
import com.example.mapsapp.model.BotChatMessage
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.adapter.BotChatAdapter
import com.example.mapsapp.databinding.ActivityChatBotBinding
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.UUID

@AndroidEntryPoint
class ChatBotActivity : AppCompatActivity() {

    private lateinit var chatAdapter: BotChatAdapter
    private val messages = mutableListOf<BotChatMessage>()
    private lateinit var binding: ActivityChatBotBinding
    private lateinit var firestore: FirebaseFirestore
    private val userId: String = UUID.randomUUID().toString() // Benzersiz kullanıcı kimliği oluştur

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firestore = FirebaseFirestore.getInstance()

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = "Chat Bot"
        binding.toolbar.setNavigationOnClickListener {
            onBackPressed()
        }

        chatAdapter = BotChatAdapter(messages)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChat.adapter = chatAdapter

        binding.buttonSend.setOnClickListener {
            val message = binding.editTextMessage.text.toString()
            if (message.isNotEmpty()) {
                addMessage(message, true)
                sendMessageToServer(message)
                binding.editTextMessage.text.clear()
            } else {
                Toast.makeText(this, "Mesaj boş olamaz", Toast.LENGTH_SHORT).show()
            }
        }

        binding.buttonReset.setOnClickListener {
            resetMemory()
        }
    }

    private fun addMessage(message: String, isUser: Boolean) {
        val chatMessage = BotChatMessage(message, isUser, System.currentTimeMillis())
        messages.add(chatMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewChat.scrollToPosition(messages.size - 1)
        sendMessageToFirestore(chatMessage)

        val botMessage = BotMessage(if (isUser) "user" else "bot", message)
        MemoryManager.addMessage(userId, botMessage)
    }

    private fun sendMessageToServer(message: String) {
        CoroutineScope(Dispatchers.IO).launch {
            val messageList = MemoryManager.getLastMessages(userId, 5)
            Log.d("ChatBotActivity", "Sending last 5 messages to server: $messageList")
            val request = ChatRequest(messages = messageList)
            try {
                val response = RetrofitClient.instance.sendMessage(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        processStreamedResponse(response.body())
                    } else {
                        addMessage("Hata: ${response.code()}", false)
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatBotActivity", "onFailure: ${e.message}")
                withContext(Dispatchers.Main) {
                    addMessage("Başarısız: ${e.message}", false)
                }
            }
        }
    }

    private fun processStreamedResponse(responseBody: ResponseBody?) {
        responseBody ?: return

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val reader = BufferedReader(InputStreamReader(responseBody.byteStream()))
                var line: String?
                var accumulatedMessage = ""
                while (reader.readLine().also { line = it } != null) {
                    line?.let {
                        val content = it.removePrefix("data: ").trim()
                        if (content.isNotEmpty()) {
                            val char = extractContentFromJson(content)
                            if (char != null) {
                                accumulatedMessage += char
                                withContext(Dispatchers.Main) {
                                    updateBotMessage(accumulatedMessage)
                                }
                                delay(50)  // Her karakter arasında 50 milisaniye gecikme ekliyoruz
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ChatBotActivity", "Error processing stream: ${e.message}")
            }
        }
    }

    private fun extractContentFromJson(json: String): String? {
        return try {
            val jsonObject = JSONObject(json)
            jsonObject.getString("content")
        } catch (e: JSONException) {
            Log.e("ChatBotActivity", "JSON parsing error: ${e.message}")
            null
        }
    }

    private fun updateBotMessage(message: String) {
        if (messages.isNotEmpty() && !messages.last().isUser) {
            messages[messages.size - 1].message = message
            chatAdapter.notifyItemChanged(messages.size - 1)
        } else {
            val chatMessage = BotChatMessage(message, false, System.currentTimeMillis())
            messages.add(chatMessage)
            chatAdapter.notifyItemInserted(messages.size - 1)
        }
        binding.recyclerViewChat.scrollToPosition(messages.size - 1)

        // Bot mesajını hafızaya ekle
        val botMessage = BotMessage("bot", message)
        MemoryManager.addMessage(userId, botMessage)
    }

    private fun resetMemory() {
        CoroutineScope(Dispatchers.IO).launch {
            val request = ChatRequest(messages = emptyList())
            try {
                val response = RetrofitClient.instance.resetMemory(request).execute()
                withContext(Dispatchers.Main) {
                    if (response.isSuccessful) {
                        Toast.makeText(this@ChatBotActivity, "Memory reset successful.", Toast.LENGTH_SHORT).show()
                        MemoryManager.resetMemory(userId) // Kullanıcı hafızasını sıfırla
                    } else {
                        Toast.makeText(this@ChatBotActivity, "Memory reset failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatBotActivity, "Memory reset failed.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun sendMessageToFirestore(chatMessage: BotChatMessage) {
        val messageData = hashMapOf(
            "message" to chatMessage.message,
            "isUser" to chatMessage.isUser,
            "timestamp" to chatMessage.timestamp
        )

        firestore.collection("chats")
            .add(messageData)
    }
}
