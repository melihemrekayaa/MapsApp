package com.example.mapsapp.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mapsapp.model.Message
import com.example.mapsapp.model.MessageWithUserProfile
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

    private val _messages = MutableLiveData<List<MessageWithUserProfile>>()
    val messages: LiveData<List<MessageWithUserProfile>> get() = _messages

    private val _messagesWithProfiles = MutableLiveData<List<MessageWithUserProfile>>()
    val messagesWithProfiles: LiveData<List<MessageWithUserProfile>> get() = _messagesWithProfiles

    private val _userProfiles = MutableLiveData<Map<String, String>>() // Map<userId, photoBase64>
    val userProfiles: LiveData<Map<String, String>> get() = _userProfiles


    private var chatListener: ListenerRegistration? = null

    fun listenForMessages(receiverId: String) {
        val currentUserId = auth.currentUser?.uid ?: return
        firestore.collection("messages")
            .whereIn("senderId", listOf(currentUserId, receiverId))
            .whereIn("receiverId", listOf(currentUserId, receiverId))
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null || value == null) return@addSnapshotListener

                val messages = value.mapNotNull { it.toObject(Message::class.java) }
                fetchUserProfilesAndMap(messages)
            }
    }

    private fun fetchUserProfilesAndMap(messages: List<Message>) {
        val userIds = messages.map { it.senderId }.toSet()
        val usersRef = firestore.collection("users")
        val userMap = mutableMapOf<String, Pair<String, String>>() // userId -> Pair<name, photoBase64>

        userIds.forEach { userId ->
            usersRef.document(userId).get().addOnSuccessListener { document ->
                val name = document.getString("name") ?: ""
                val photo = document.getString("photoBase64") ?: ""
                userMap[userId] = Pair(name, photo)

                if (userMap.size == userIds.size) {
                    val mapped = messages.map { msg ->
                        val (name, photoBase64) = userMap[msg.senderId] ?: Pair("", "")
                        MessageWithUserProfile(msg, name, photoBase64)
                    }
                    _messagesWithProfiles.value = mapped
                }
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

