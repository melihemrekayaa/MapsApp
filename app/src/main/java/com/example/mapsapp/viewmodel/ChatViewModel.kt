package com.example.mapsapp.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.model.Message
import com.example.mapsapp.webrtc.FirebaseClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.firestore.Query
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val firebaseClient: FirebaseClient // ✅ Inject edildi
) : ViewModel() {

    private val _messages = MutableLiveData<List<Message>>()
    val messages: LiveData<List<Message>> get() = _messages

    private var chatListener: ListenerRegistration? = null

    fun listenForMessages(receiverId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        val combinedMessages = mutableListOf<Message>()

        val chatQuery = firestore.collection("messages")
            .whereIn("senderId", listOf(currentUserId, receiverId))
            .whereIn("receiverId", listOf(currentUserId, receiverId))
            .orderBy("timestamp", Query.Direction.ASCENDING)

        chatListener = chatQuery.addSnapshotListener { value, error ->
            if (error != null) return@addSnapshotListener

            combinedMessages.clear()
            value?.forEach { doc ->
                val message = doc.toObject(Message::class.java)
                combinedMessages.add(message)
            }

            _messages.value = combinedMessages
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

    fun sendCallRequest(
        receiverId: String,
        isVideoCall: Boolean,
        roomId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            val senderUid = auth.currentUser?.uid ?: return@launch
            val success = firebaseClient.sendCallRequest(
                receiverId = receiverId,
                roomId = roomId,
                isVideoCall = isVideoCall,
                senderUid = senderUid
            )
            onResult(success)
        }
    }

    fun rejectIncomingCall(roomId: String) {
        viewModelScope.launch {
            firebaseClient.rejectCall(roomId)
        }
    }

    fun cancelOutgoingCall(roomId: String) {
        viewModelScope.launch {
            firebaseClient.cancelCall(roomId)
        }
    }

    fun clearCall(roomId: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            firebaseClient.cancelCall(roomId)
            firebaseClient.removeCallRequest(uid, roomId)
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
    }

}

