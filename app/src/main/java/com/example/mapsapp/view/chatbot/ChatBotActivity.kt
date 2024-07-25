package com.example.mapsapp.view.chatbot

import ChatRequest
import Message
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.adapter.BotChatAdapter
import com.example.mapsapp.databinding.ActivityChatBotBinding
import com.example.mapsapp.model.BotChatMessage
import com.google.firebase.firestore.FirebaseFirestore
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import android.util.Log
import kotlinx.coroutines.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

class ChatBotActivity : AppCompatActivity() {

    private lateinit var chatAdapter: BotChatAdapter
    private val messages = mutableListOf<BotChatMessage>()
    private lateinit var binding: ActivityChatBotBinding
    private lateinit var firestore: FirebaseFirestore

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
    }

    private fun addMessage(message: String, isUser: Boolean) {
        val chatMessage = BotChatMessage(message, isUser, System.currentTimeMillis())
        messages.add(chatMessage)
        chatAdapter.notifyItemInserted(messages.size - 1)
        binding.recyclerViewChat.scrollToPosition(messages.size - 1)
        sendMessageToFirestore(chatMessage)
    }

    private fun sendMessageToServer(message: String) {
        val messagesList = listOf(Message("user", message))
        val request = ChatRequest(messagesList)
        RetrofitClient.instance.sendMessage(request).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.isSuccessful) {
                    processStreamedResponse(response.body())
                } else {
                    addMessage("Hata: ${response.code()}", false)
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("ChatBotActivity", "onFailure: ${t.message}")
                addMessage("Başarısız: ${t.message}", false)
            }
        })
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
