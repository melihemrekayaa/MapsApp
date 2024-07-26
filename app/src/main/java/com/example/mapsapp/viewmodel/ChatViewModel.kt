package com.example.mapsapp.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.model.Message
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> get() = _userName

    private var chatListener: ListenerRegistration? = null
    private var reverseChatListener: ListenerRegistration? = null

    fun listenForMessages(receiverId: String) {
        val currentUserId = auth.currentUser?.uid ?: return

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
                return@addSnapshotListener
            }

            combinedMessages.clear()
            for (doc in value!!) {
                val message = doc.toObject(Message::class.java)
                combinedMessages.add(message)
            }

            reverseChatListener = reverseChatQuery.addSnapshotListener { reverseValue, reverseError ->
                if (reverseError != null) {
                    return@addSnapshotListener
                }

                for (doc in reverseValue!!) {
                    val message = doc.toObject(Message::class.java)
                    combinedMessages.add(message)
                }

                combinedMessages.sortBy { it.timestamp }
                _messages.value = combinedMessages
            }
        }
    }

    fun sendMessage(receiverId: String, messageText: String) {
        if (messageText.isNotEmpty()) {
            val message = Message(
                senderId = auth.currentUser?.uid ?: "",
                receiverId = receiverId,
                message = messageText,
                timestamp = System.currentTimeMillis()
            )
            firestore.collection("messages").add(message)
        }
    }

    fun fetchUserName(userId: String): LiveData<String?> {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                _userName.value = document.getString("email")?.split("@")?.get(0)
            }
            .addOnFailureListener {
                _userName.value = null
            }
        return _userName
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
        reverseChatListener?.remove()
    }
}
