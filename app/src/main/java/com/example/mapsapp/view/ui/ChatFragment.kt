package com.example.mapsapp.view.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: ChatAdapter
    private val messages = mutableListOf<Message>()
    private var receiverId: String? = null

    private var chatListener: ListenerRegistration? = null
    private var reverseChatListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        arguments?.let {
            receiverId = it.getString("receiverId")
        }

        setupToolbar()  // Kullanıcı adını setupToolbar() içinde hemen çekiyoruz.

        adapter = ChatAdapter(messages, auth.currentUser?.uid ?: "")
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.sendButton.setOnClickListener {
            sendMessage()
        }

        listenForMessages()
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        return binding.root
    }

    private fun setupToolbar() {
        val toolbar = binding.chatToolbar
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        receiverId?.let { id ->
            firestore.collection("users").document(id).get()
                .addOnSuccessListener { document ->
                    val userName = document.getString("email")?.split("@")?.get(0)
                    toolbar.title = userName ?: "User"
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "Failed to load user info", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun sendMessage() {
        val messageText = binding.messageEditText.text.toString()
        if (messageText.isNotEmpty()) {
            val message = Message(
                senderId = auth.currentUser?.uid ?: "",
                receiverId = receiverId ?: "",
                message = messageText,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("messages").add(message)
            binding.messageEditText.text?.clear()
            binding.recyclerView.scrollToPosition(messages.size - 1)
        }
    }

    private fun listenForMessages() {
        val currentUserId = auth.currentUser?.uid ?: ""
        receiverId?.let { receiverId ->
            val chatQuery = firestore.collection("messages")
                .whereEqualTo("senderId", currentUserId)
                .whereEqualTo("receiverId", receiverId)
                .orderBy("timestamp", Query.Direction.ASCENDING)

            val reverseChatQuery = firestore.collection("messages")
                .whereEqualTo("senderId", receiverId)
                .whereEqualTo("receiverId", currentUserId)
                .orderBy("timestamp", Query.Direction.ASCENDING)

            val combinedMessages = mutableListOf<Message>()

            chatListener = chatQuery.addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(requireContext(), "Error while loading messages.", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                combinedMessages.clear()
                for (doc in value!!) {
                    val message = doc.toObject(Message::class.java)
                    combinedMessages.add(message)
                }

                reverseChatListener = reverseChatQuery.addSnapshotListener { reverseValue, reverseError ->
                    if (reverseError != null) {
                        Toast.makeText(requireContext(), "Error while loading messages.", Toast.LENGTH_SHORT).show()
                        return@addSnapshotListener
                    }

                    for (doc in reverseValue!!) {
                        val message = doc.toObject(Message::class.java)
                        combinedMessages.add(message)
                    }

                    combinedMessages.sortBy { it.timestamp }
                    messages.clear()
                    messages.addAll(combinedMessages)
                    adapter.notifyDataSetChanged()
                    binding.recyclerView.scrollToPosition(messages.size - 1)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chatListener?.remove()
        reverseChatListener?.remove()
        _binding = null
    }
}
