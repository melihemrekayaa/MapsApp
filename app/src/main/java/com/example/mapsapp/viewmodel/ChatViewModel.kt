package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject


class ChatViewModel  constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    private val currentMessages = mutableListOf<Message>()
    private var chatListener: ListenerRegistration? = null
    private var reverseChatListener: ListenerRegistration? = null

    fun getCurrentUserId(): String? {
        return auth.currentUser?.uid
    }

    fun listenForMessages(receiverId: String) {
        val currentUserId = getCurrentUserId() ?: ""
        val chatQuery = firestore.collection("messages")
            .whereEqualTo("senderId", currentUserId)
            .whereEqualTo("receiverId", receiverId)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        val reverseChatQuery = firestore.collection("messages")
            .whereEqualTo("senderId", receiverId)
            .whereEqualTo("receiverId", currentUserId)
            .orderBy("timestamp", Query.Direction.ASCENDING)

        chatListener = chatQuery.addSnapshotListener { value, error ->
            if (error != null) {
                return@addSnapshotListener
            }

            value?.documentChanges?.forEach { change ->
                val message = change.document.toObject(Message::class.java)
                when (change.type) {
                    com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                        currentMessages.add(message)
                        _messages.value = currentMessages.toList()
                    }
                    com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                        val index = currentMessages.indexOfFirst { it.timestamp == message.timestamp }
                        if (index != -1) {
                            currentMessages[index] = message
                            _messages.value = currentMessages.toList()
                        }
                    }
                    com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                        currentMessages.removeIf { it.timestamp == message.timestamp }
                        _messages.value = currentMessages.toList()
                    }
                }
            }

            reverseChatListener = reverseChatQuery.addSnapshotListener { reverseValue, reverseError ->
                if (reverseError != null) {
                    return@addSnapshotListener
                }

                reverseValue?.documentChanges?.forEach { change ->
                    val message = change.document.toObject(Message::class.java)
                    when (change.type) {
                        com.google.firebase.firestore.DocumentChange.Type.ADDED -> {
                            currentMessages.add(message)
                            _messages.value = currentMessages.toList()
                        }
                        com.google.firebase.firestore.DocumentChange.Type.MODIFIED -> {
                            val index = currentMessages.indexOfFirst { it.timestamp == message.timestamp }
                            if (index != -1) {
                                currentMessages[index] = message
                                _messages.value = currentMessages.toList()
                            }
                        }
                        com.google.firebase.firestore.DocumentChange.Type.REMOVED -> {
                            currentMessages.removeIf { it.timestamp == message.timestamp }
                            _messages.value = currentMessages.toList()
                        }
                    }
                }
                currentMessages.sortBy { it.timestamp }
                _messages.value = currentMessages.toList()
            }
        }
    }

    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isNotEmpty()) {
            val message = Message(
                senderId = getCurrentUserId() ?: "",
                receiverId = receiverId,
                message = messageText,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("messages").add(message)
        }
    }

    fun setupToolbarTitle(receiverId: String, onTitleFetched: (String) -> Unit, onError: () -> Unit) {
        firestore.collection("users").document(receiverId).get()
            .addOnSuccessListener { document ->
                val userName = document.getString("email")?.split("@")?.get(0)
                onTitleFetched(userName ?: "User")
            }
            .addOnFailureListener {
                onError()
            }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
        reverseChatListener?.remove()
    }
}
