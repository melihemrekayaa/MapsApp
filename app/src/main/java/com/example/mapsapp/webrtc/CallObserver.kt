package com.example.mapsapp.webrtc

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.mapsapp.webrtc.utils.awaitRemoveValue
import com.example.mapsapp.webrtc.utils.awaitSingle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

object CallObserver {

    private var isListening = false
    private var callJob: Job? = null
    private var currentUserId: String? = null

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private const val CALL_DEBOUNCE_MS = 3000L
    private val lastCallTimestamps = mutableMapOf<String, Long>()

    fun start(context: Context) {
        if (isListening) return

        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val ref = FirebaseDatabase.getInstance()
            .getReference("callRequests")
            .child(currentUserId!!)

        // Coroutine ile sürekli dinleme
        callJob = coroutineScope.launch {
            while (isActive) {
                try {
                    val snapshot = ref.awaitSingle()

                    if (!snapshot.exists()) {
                        delay(500)
                        continue
                    }

                    val roomId = snapshot.child("roomId").getValue(String::class.java) ?: continue
                    val callerUid = snapshot.child("callerUid").getValue(String::class.java) ?: continue
                    val isVideoCall = snapshot.child("isVideoCall").getValue(Boolean::class.java) ?: true

                    val now = System.currentTimeMillis()
                    if ((lastCallTimestamps[roomId] ?: 0) + CALL_DEBOUNCE_MS > now) {
                        Log.d("CallObserver", "⏱ Spam çağrı engellendi: $roomId")
                        delay(1000)
                        continue
                    }
                    lastCallTimestamps[roomId] = now

                    if (IncomingCallActivity.isActive || CallActivity.isActive) {
                        Log.d("CallObserver", "📵 Çağrı ekranı zaten açık.")
                        delay(1000)
                        continue
                    }

                    val callsSnapshot = FirebaseDatabase.getInstance()
                        .getReference("calls")
                        .child(roomId)
                        .awaitSingle()

                    val isEnded = callsSnapshot.child("callEnded").getValue(Boolean::class.java) ?: false
                    val status = callsSnapshot.child("status").getValue(String::class.java)

                    if (isEnded || status == "rejected") {
                        Log.d("CallObserver", "❌ Çağrı zaten bitmiş veya reddedilmiş → siliniyor.")
                        ref.awaitRemoveValue()
                        delay(1000)
                        continue
                    }

                    // Çağrı ekranı açılmadan önce sil
                    ref.awaitRemoveValue()

                    withContext(Dispatchers.Main) {
                        val intent = Intent(context, IncomingCallActivity::class.java).apply {
                            putExtra("roomId", roomId)
                            putExtra("callerUid", callerUid)
                            putExtra("isVideoCall", isVideoCall)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(intent)
                    }

                } catch (e: Exception) {
                    Log.e("CallObserver", "Hata: ${e.localizedMessage}")
                    delay(1000)
                }
            }
        }

        isListening = true
        Log.d("CallObserver", "✅ CallObserver coroutine ile dinlemeye başladı.")
    }

    fun stop() {
        callJob?.cancel()
        callJob = null
        isListening = false
        Log.d("CallObserver", "🛑 CallObserver durduruldu.")
    }
}
