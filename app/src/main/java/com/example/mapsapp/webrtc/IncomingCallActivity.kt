package com.example.mapsapp.webrtc

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.*
import android.util.Log
import android.view.MotionEvent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.lifecycle.lifecycleScope
import com.example.mapsapp.databinding.ActivityIncomingCallBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
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

    @Inject
    lateinit var firebaseClient: FirebaseClient

    companion object {
        var isActive = false
    }

    @SuppressLint("ClickableViewAccessibility")
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

        lifecycleScope.launch {
            try {
                firebaseClient.acceptCall(roomId)
                firebaseClient.removeCallRequest(getCurrentUserId(), roomId)

                val intent = Intent(this@IncomingCallActivity, CallActivity::class.java).apply {
                    putExtra("roomId", roomId)
                    putExtra("callerUid", callerUid)
                    putExtra("isCaller", false)
                    putExtra("isVideoCall", isVideoCall)
                }
                startActivity(intent)
            } catch (e: Exception) {
                Log.e("IncomingCall", "answerCall exception: ${e.message}")
            } finally {
                finish()
            }
        }
    }

    private fun rejectCall() {
        if (isActionTaken) return
        isActionTaken = true

        stopRingtoneAndVibration()

        lifecycleScope.launch {
            try {
                // 1. Çağrıyı reddet → status: rejected
                firebaseClient.rejectCall(roomId)

                // 2. Çağrı verisini sil
                firebaseClient.cancelCall(roomId)

                // 3. Kullanıcının inCall durumunu sıfırla
                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return@launch
                firebaseClient.setUserInCall(uid, false)

                // 4. Kendi callRequest'ini temizle
                firebaseClient.removeCallRequest(uid, roomId)
            } catch (e: Exception) {
                Log.e("IncomingCall", "rejectCall exception: ${e.message}")
            } finally {
                finish()
            }
        }
    }




    private fun getCurrentUserId(): String {
        return FirebaseAuth.getInstance().currentUser?.uid ?: ""
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
