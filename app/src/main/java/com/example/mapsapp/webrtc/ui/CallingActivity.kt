package com.example.mapsapp.webrtc.ui

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.databinding.ActivityCallingBinding
import com.example.mapsapp.webrtc.utils.DataModelType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CallingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCallingBinding
    private lateinit var receiverId: String
    private var isVideoCall = true
    private lateinit var senderId: String

    private val firestore = FirebaseFirestore.getInstance()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCallingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        receiverId = intent.getStringExtra("target") ?: return finish()
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)
        senderId = FirebaseAuth.getInstance().currentUser?.uid ?: return finish()

        val callType = if (isVideoCall) "Video" else "Voice"
        binding.callingUsername.text = "Calling $receiverId ($callType)..."

        listenForCallResponse()

        binding.btnCancel.setOnClickListener {
            cancelCall()
        }

        // Timeout 30 saniye içinde cevap yoksa iptal
        handler.postDelayed({
            cancelCall()
        }, 30_000)
    }

    private fun listenForCallResponse() {
        firestore.collection("calls").document(senderId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val type = snapshot.getString("type")
                if (type == DataModelType.AcceptCall.name) {
                    Log.d("CallingActivity", "Call accepted!")
                    startActivity(Intent(this, CallActivity::class.java).apply {
                        putExtra("target", receiverId)
                        putExtra("isVideoCall", isVideoCall)
                        putExtra("isCaller", true)
                    })
                    finish()
                } else if (type == DataModelType.RejectCall.name) {
                    Log.d("CallingActivity", "Call rejected.")
                    finish()
                }
            }
    }

    private fun cancelCall() {
        firestore.collection("calls").document(receiverId).delete()
        firestore.collection("calls").document(senderId).delete()
        finish()
    }
}
