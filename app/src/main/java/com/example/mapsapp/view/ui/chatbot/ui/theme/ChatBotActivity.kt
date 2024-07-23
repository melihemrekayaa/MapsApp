package com.example.mapsapp.view.ui.chatbot.ui.theme

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.R
import com.example.mapsapp.service.ChatRequest
import com.example.mapsapp.service.ChatResponse
import com.example.mapsapp.service.RetrofitClient
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatBotActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_bot)

        val editTextMessage = findViewById<EditText>(R.id.editTextMessage)
        val buttonSend = findViewById<Button>(R.id.buttonSend)
        val textViewResponse = findViewById<TextView>(R.id.textViewResponse)

        buttonSend.setOnClickListener {
            val message = editTextMessage.text.toString()
            if (message.isNotEmpty()) {
                sendMessageToServer(message) { response ->
                    textViewResponse.text = response
                }
            } else {
                Toast.makeText(this, "Mesaj boş olamaz", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendMessageToServer(message: String, callback: (String) -> Unit) {
        val request = ChatRequest(message)
        RetrofitClient.instance.sendMessage(request).enqueue(object : Callback<ChatResponse> {
            override fun onResponse(call: Call<ChatResponse>, response: Response<ChatResponse>) {
                if (response.isSuccessful) {
                    callback(response.body()?.response ?: "Cevap alınamadı")
                } else {
                    callback("Hata: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ChatResponse>, t: Throwable) {
                callback("Başarısız: ${t.message}")
            }
        })
    }
}
