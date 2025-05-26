package com.example.mapsapp.webrtc

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mapsapp.webrtc.utils.awaitRemoveValue
import com.example.mapsapp.webrtc.utils.awaitSingle
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.*

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

                    // 👇 Aktif çağrı ekranı varsa ekran açma
                    if (IncomingCallActivity.isActive || CallActivity.isActive) {
                        Log.d("CallObserver", "📵 Çağrı ekranı zaten açık.")
                        delay(1000)
                        continue
                    }

                    // ✅ 'calls' verisi var mı kontrolü
                    val callsSnapshot = FirebaseDatabase.getInstance()
                        .getReference("calls")
                        .child(roomId)
                        .awaitSingle()

                    val isEnded = callsSnapshot.child("callEnded").getValue(Boolean::class.java) ?: false
                    val status = callsSnapshot.child("status").getValue(String::class.java)

                    if (isEnded || status == "rejected") {
                        Log.d("CallObserver", "❌ Çağrı zaten bitmiş veya reddedilmiş → siliniyor.")
                        ref.awaitRemoveValue()
                        resetInCallIfOrphaned(roomId)
                        delay(1000)
                        continue
                    }

                    // 🔒 Güvenlik kontrolü: inCall true ama çağrı yoksa → resetle
                    val isUserInCall = FirebaseDatabase.getInstance()
                        .getReference("users").child(currentUserId!!).child("inCall")
                        .awaitSingle().getValue(Boolean::class.java) ?: false

                    if (isUserInCall && !callsSnapshot.exists()) {
                        Log.w("CallObserver", "🧹 Orphaned inCall temizleniyor...")
                        FirebaseDatabase.getInstance()
                            .getReference("users").child(currentUserId!!).child("inCall")
                            .setValue(false)
                    }

                    // 🎯 Çağrı ekranı açılmadan önce istek silinsin
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
                    Log.e("CallObserver", "⚠️ Hata: ${e.localizedMessage}")
                    delay(1000)
                }
            }
        }

        isListening = true
        Log.d("CallObserver", "✅ CallObserver coroutine ile dinlemeye başladı.")
    }

    private suspend fun resetInCallIfOrphaned(roomId: String) {
        try {
            val uid = currentUserId ?: return
            val userRef = FirebaseDatabase.getInstance().getReference("users").child(uid)
            val callRef = FirebaseDatabase.getInstance().getReference("calls").child(roomId)

            val callExists = callRef.awaitSingle().exists()
            if (!callExists) {
                Log.d("CallObserver", "💀 Boşta kalan inCall sıfırlanıyor...")
                userRef.child("inCall").setValue(false)
            }
        } catch (e: Exception) {
            Log.e("CallObserver", "resetInCallIfOrphaned hata: ${e.localizedMessage}")
        }
    }

    fun stop() {
        callJob?.cancel()
        callJob = null
        isListening = false
        Log.d("CallObserver", "🛑 CallObserver durduruldu.")
    }
}
