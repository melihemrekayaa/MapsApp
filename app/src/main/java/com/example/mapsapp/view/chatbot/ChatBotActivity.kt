package com.example.mapsapp.view.chatbot

import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.BotChatAdapter
import com.example.mapsapp.databinding.ActivityChatBotBinding
import com.example.mapsapp.view.ui.components.CustomBottomNavView
import com.example.mapsapp.viewmodel.ChatBotViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChatBotActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBotBinding
    private val viewModel: ChatBotViewModel by viewModels()
    private val adapter = BotChatAdapter()

    private val userId: String
        get() {
            val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()
            Log.d("ChatBot", "Current userId: $uid")
            return uid
        }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBotBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        observeMessages()
        setupSendButton()
        setupClearButton()
        viewModel.listenMessages(userId)
    }

    private fun setupRecyclerView() {
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply {
            stackFromEnd = true
        }
        binding.recyclerMessages.adapter = adapter
    }

    private fun observeMessages() {
        viewModel.messages.observe(this) { messages ->
            adapter.submitList(messages)
            binding.recyclerMessages.post {
                binding.recyclerMessages.scrollToPosition(messages.size - 1)
            }
        }
    }


    private fun setupSendButton() {
        binding.buttonSend.setOnClickListener {
            val text = binding.editMessage.text.toString().trim()
            if (text.isNotEmpty()) {
                viewModel.sendUserMessage(userId, text)
                binding.editMessage.setText("")
            }
        }
    }

    private fun setupClearButton() {
        binding.btnClearChat.setOnClickListener {
            viewModel.clearChat(userId)
        }
    }








}
