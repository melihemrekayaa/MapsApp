package com.example.mapsapp.webrtc

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import javax.inject.Inject

class IncomingCallActivity : AppCompatActivity() {

    private lateinit var tvCallerName: TextView
    private lateinit var tvAccept: ImageView
    private lateinit var tvReject: ImageView

    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private var x1 = 0f
    private var x2 = 0f
    private val MIN_DISTANCE = 150

    private var roomId: String = ""
    private var callerUid: String = ""
    private var isVideoCall: Boolean = true

    private val firebaseDb = FirebaseDatabase.getInstance()

    @Inject
    lateinit var firebaseClient: FirebaseClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_incoming_call)

        roomId = intent.getStringExtra("roomId") ?: ""
        callerUid = intent.getStringExtra("callerUid") ?: ""
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)

        if (roomId.isBlank() || callerUid.isBlank()) {
            Toast.makeText(this, "Geçersiz çağrı bilgisi", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        tvCallerName = findViewById(R.id.tvCallerName)
        tvAccept = findViewById(R.id.tvAccept)
        tvReject = findViewById(R.id.tvReject)

        tvCallerName.text = "User: $callerUid"

        startRingtoneAndVibration()

        tvAccept.setOnClickListener { answerCall() }
        tvReject.setOnClickListener { rejectCall() }

        findViewById<View>(R.id.incomingCallRoot).setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    x1 = event.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    x2 = event.x
                    val deltaX = x2 - x1
                    if (kotlin.math.abs(deltaX) > MIN_DISTANCE) {
                        if (deltaX > 0) answerCall() else rejectCall()
                    } else view.performClick()
                    true
                }
                else -> false
            }
        }
    }

    private fun startRingtoneAndVibration() {
        try {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ringtone = RingtoneManager.getRingtone(applicationContext, notification)
            ringtone?.play()

            vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 1000, 1000)
            vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } catch (e: Exception) {
            Log.e("IncomingCallActivity", "Ses veya titreşim başlatılamadı", e)
        }
    }

    private fun stopRingtoneAndVibration() {
        ringtone?.stop()
        vibrator?.cancel()
    }

    private fun answerCall() {
        stopRingtoneAndVibration()
        firebaseClient.acceptCall(roomId)

        clearCallRequest {
            val intent = Intent(this@IncomingCallActivity, CallActivity::class.java).apply {
                putExtra("roomId", roomId)
                putExtra("callerUid", callerUid)
                putExtra("isCaller", false)
                putExtra("isVideoCall", isVideoCall)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            startActivity(intent)
            finish()
        }
    }

    private fun rejectCall() {
        stopRingtoneAndVibration()
        firebaseClient.rejectCall(roomId)

        firebaseDb.getReference("calls").child(roomId).child("callEnded").setValue(true)
        clearCallRequest { finish() }
    }

    private fun clearCallRequest(onComplete: () -> Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete()

        val callRequestsRef = firebaseDb.getReference("callRequests").child(userId)
        callRequestsRef.orderByChild("roomId").equalTo(roomId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { it.ref.removeValue() }
                    onComplete()
                }

                override fun onCancelled(error: DatabaseError) {
                    onComplete()
                }
            })
    }

    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneAndVibration()
    }
}
