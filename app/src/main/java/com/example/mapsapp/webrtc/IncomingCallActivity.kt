package com.example.mapsapp.webrtc

import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import com.example.mapsapp.databinding.ActivityIncomingCallBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class IncomingCallActivity : AppCompatActivity() {

    private lateinit var binding: ActivityIncomingCallBinding
    private var isActionTaken = false
    private var ringtone: Ringtone? = null
    private var vibrator: Vibrator? = null

    private var roomId: String = ""
    private var callerUid: String = ""
    private var isVideoCall: Boolean = true

    private val firebaseDb = FirebaseDatabase.getInstance()

    @Inject
    lateinit var firebaseClient: FirebaseClient

    companion object {
        var isActive = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityIncomingCallBinding.inflate(layoutInflater)
        setContentView(binding.root)

        roomId = intent.getStringExtra("roomId") ?: ""
        callerUid = intent.getStringExtra("callerUid") ?: ""
        isVideoCall = intent.getBooleanExtra("isVideoCall", true)

        if (roomId.isBlank() || callerUid.isBlank()) {
            finish()
            return
        }

        binding.tvCallerName.text = "User: $callerUid"
        startRingtoneAndVibration()

        binding.tvAccept.setOnClickListener { answerCall() }
        binding.tvReject.setOnClickListener { rejectCall() }

        binding.incomingCallRoot.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    v.tag = event.x
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaX = event.x - (v.tag as? Float ?: 0f)
                    if (kotlin.math.abs(deltaX) > 150) {
                        if (deltaX > 0) answerCall() else rejectCall()
                    } else {
                        v.performClick()  // ☑️ Uyarıyı çözen satır
                    }
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
            Log.e("IncomingCall", "Ringtone/Vibration error", e)
        }
    }

    private fun stopRingtoneAndVibration() {
        try {
            ringtone?.stop()
            vibrator?.cancel()
        } catch (_: Exception) {}
    }

    private fun answerCall() {
        if (isActionTaken) return
        isActionTaken = true

        stopRingtoneAndVibration()
        firebaseClient.acceptCall(roomId)

        clearCallRequest {
            val intent = Intent(this, CallActivity::class.java).apply {
                putExtra("roomId", roomId)
                putExtra("callerUid", callerUid)
                putExtra("isCaller", false)
                putExtra("isVideoCall", isVideoCall)
            }
            startActivity(intent)
            finish()
        }
    }

    private fun rejectCall() {
        if (isActionTaken) return
        isActionTaken = true

        stopRingtoneAndVibration()
        firebaseClient.rejectCall(roomId)
        firebaseClient.cancelCall(roomId)    // ← ekledik

        clearCallRequest { finish() }
    }



    private fun clearCallRequest(onComplete: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return onComplete()
        firebaseDb.getReference("callRequests").child(uid)
            .orderByChild("roomId").equalTo(roomId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { it.ref.removeValue() }
                    onComplete()
                }
                override fun onCancelled(error: DatabaseError) = onComplete()
            })
    }



    override fun onDestroy() {
        super.onDestroy()
        stopRingtoneAndVibration()
    }

    override fun onStart() {
        super.onStart()
        isActive = true
    }

    override fun onStop() {
        super.onStop()
        isActive = false
    }
}
