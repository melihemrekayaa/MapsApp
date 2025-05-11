package com.example.mapsapp.webrtc

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

object CallObserver {

    private var isListening = false
    private var callListener: ChildEventListener? = null
    private var currentUserId: String? = null

    fun start(context: Context) {
        if (isListening) return

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance()
            .getReference("callRequests")
            .child(currentUserId!!)

        callListener = object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val roomId = snapshot.child("roomId").getValue(String::class.java) ?: return
                val callerUid = snapshot.child("callerUid").getValue(String::class.java) ?: return
                val isVideoCall = snapshot.child("isVideoCall").getValue(Boolean::class.java) ?: true

                FirebaseDatabase.getInstance().getReference("calls")
                    .child(roomId)
                    .child("callEnded")
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(dataSnapshot: DataSnapshot) {
                            val isEnded = dataSnapshot.getValue(Boolean::class.java) ?: false

                            if (isEnded) {
                                Log.d("CallObserver", "❌ Çağrı zaten bitmişti → callRequest siliniyor.")
                                snapshot.ref.removeValue() // ✅ En kritik satır
                                return
                            }

                            if (!IncomingCallActivity.isActive && !CallActivity.isActive) {
                                val intent = Intent(context, IncomingCallActivity::class.java).apply {
                                    putExtra("roomId", roomId)
                                    putExtra("callerUid", callerUid)
                                    putExtra("isVideoCall", isVideoCall)
                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                }
                                context.startActivity(intent)

                                // ✅ Yalnızca çağrı ekranı açıldıysa callRequest silinir
                                snapshot.ref.removeValue()
                            } else {
                                Log.d("CallObserver", "Zaten çağrı ekranındayız. Veri silinmedi.")
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            Log.e("CallObserver", "callEnded kontrolü başarısız: ${error.message}")
                        }
                    })
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {}
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {
                Log.e("CallObserver", "callRequest dinleme iptal: ${error.message}")
            }
        }

        ref.addChildEventListener(callListener!!)
        isListening = true
        Log.d("CallObserver", "✅ CallObserver dinlemeye başladı.")
    }

    fun stop() {
        if (!isListening || currentUserId == null || callListener == null) return

        val ref = FirebaseDatabase.getInstance()
            .getReference("callRequests")
            .child(currentUserId!!)

        ref.removeEventListener(callListener!!)
        isListening = false
        Log.d("CallObserver", "🛑 CallObserver durduruldu.")
    }
}

