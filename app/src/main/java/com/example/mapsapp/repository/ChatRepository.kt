package com.example.mapsapp.repository

import com.example.mapsapp.model.ChatMessage
import com.google.firebase.database.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val db: FirebaseDatabase
) {
    fun sendMessage(userId: String, message: ChatMessage, messageId: String? = null) {
        val finalId = messageId ?: db.reference.push().key ?: return
        db.reference.child("chatMessages").child(userId).child(finalId).setValue(message)
    }

    fun listenMessages(userId: String, onData: (List<ChatMessage>) -> Unit) {
        db.reference.child("chatMessages").child(userId)
            .orderByChild("timestamp") // ← BU ŞART
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val messages = snapshot.children.mapNotNull {
                        it.getValue(ChatMessage::class.java)
                    }
                    onData(messages)
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }


    fun clearMessages(userId: String, onComplete: () -> Unit) {
        db.reference.child("chatMessages").child(userId).removeValue().addOnCompleteListener {
            onComplete()
        }
    }

}
