package com.example.mapsapp.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.mapsapp.model.Message
import com.example.mapsapp.webrtc.FirebaseClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import com.google.firebase.firestore.Query
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

    private var isCallActive = false  // ✅ Ekran sadece bir kez açılsın
    private var incomingCallRef: DatabaseReference? = null
    private var incomingCallListener: ChildEventListener? = null


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

    fun listenForIncomingCalls(userId: String, onCall: (roomId: String, callerUid: String, isVideoCall: Boolean) -> Unit) {
        val ref = FirebaseDatabase.getInstance().getReference("callRequests").child(userId)

        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val roomId = snapshot.child("roomId").getValue(String::class.java) ?: return
                val callerUid = snapshot.child("callerUid").getValue(String::class.java) ?: return
                val isVideoCall = snapshot.child("isVideoCall").getValue(Boolean::class.java) ?: true

                val callsRef = FirebaseDatabase.getInstance().getReference("calls").child(roomId)

                callsRef.child("callEnded").addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(endSnapshot: DataSnapshot) {
                        val callEnded = endSnapshot.getValue(Boolean::class.java) ?: false
                        if (!callEnded) {
                            // 👇 Çağrıyı yansıtmadan önce callRequest’i temizle
                            firebaseClient.removeCallRequest(userId, roomId) {
                                onCall(roomId, callerUid, isVideoCall)
                            }
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
            }

            override fun onCancelled(error: DatabaseError) {}
            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
        })
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
        context: Context,
        onComplete: (Boolean) -> Unit
    ) {
        val senderUid = auth.currentUser?.uid ?: return
        firebaseClient.sendCallRequest(
            receiverId = receiverId,
            roomId = roomId,
            isVideoCall = isVideoCall,
            senderUid = senderUid,
            onComplete = onComplete
        )
    }

    fun rejectIncomingCall(roomId: String) {
        firebaseClient.rejectCall(roomId)
    }

    fun cancelOutgoingCall(roomId: String) {
        firebaseClient.cancelCall(roomId)
    }

    fun clearCall(roomId: String) {
        firebaseClient.cancelCall(roomId)
        val uid = auth.currentUser?.uid ?: return
        firebaseClient.removeCallRequest(uid, roomId)
    }



    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()

        incomingCallRef?.removeEventListener(incomingCallListener!!)
        incomingCallRef = null
        incomingCallListener = null
    }

}

