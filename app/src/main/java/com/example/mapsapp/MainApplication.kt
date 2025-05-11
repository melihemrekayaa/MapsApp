package com.example.mapsapp

import android.app.Application
import android.content.Intent
import com.example.mapsapp.webrtc.CallObserver
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.example.mapsapp.webrtc.IncomingCallActivity
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        FirebaseAuth.getInstance().addAuthStateListener { auth ->
            val userId = auth.currentUser?.uid
            if (userId != null) {
                CallObserver.start(applicationContext) // ✅ Login olduysa dinlemeye başla
            } else {
                CallObserver.stop() // 🚫 Logout olduysa dinlemeyi bırak
            }
        }
    }
}
