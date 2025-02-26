package com.example.mapsapp.webrtc.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mapsapp.webrtc.repository.MainRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainServiceReceiver : BroadcastReceiver() {

    @Inject
    lateinit var mainRepository: MainRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            "REJECT_CALL" -> {
                val callerId = intent.getStringExtra("callerId")
                Log.d("MainServiceReceiver", "Call rejected from: $callerId")

                // Firebase veya Signaling Server'a çağrının reddedildiğini bildir
                callerId?.let {
                    mainRepository.sendEndCall()
                }

                // Bildirimi kapat
                val notificationManager = context?.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
                notificationManager?.cancel(1001)
            }
        }
    }
}
