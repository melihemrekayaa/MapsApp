package com.example.mapsapp.webrtc.service

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import javax.inject.Inject

class MainServiceRepository @Inject constructor(
    private val context: Context
) {

    fun startService(username: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = arrayOf(
                android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION,

            )

            if (!permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }) {
                Log.e("MainServiceRepository", "Gerekli izinler verilmedi!")
                return // İzinler verilmemişse servisi başlatma
            }
        }

        val intent = Intent(context, MainService::class.java)
        intent.putExtra("username", username)
        intent.action = "START_SERVICE"
        startServiceIntent(intent)
    }


    private fun startServiceIntent(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun setupViews(videoCall: Boolean, caller: Boolean, target: String) {
        val intent = Intent(context, MainService::class.java).apply {
            action = "SETUP_VIEWS"
            putExtra("isVideoCall", videoCall)
            putExtra("target", target)
            putExtra("isCaller", caller)
        }
        startServiceIntent(intent)
    }

    fun sendEndCall() {
        val intent = Intent(context, MainService::class.java)
        intent.action = "END_CALL"
        startServiceIntent(intent)
    }

    fun switchCamera() {
        val intent = Intent(context, MainService::class.java)
        intent.action = "SWITCH_CAMERA"
        startServiceIntent(intent)
    }

    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java).apply {
            action = "TOGGLE_AUDIO"
            putExtra("shouldBeMuted", shouldBeMuted)
        }
        startServiceIntent(intent)
    }

    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java).apply {
            action = "TOGGLE_VIDEO"
            putExtra("shouldBeMuted", shouldBeMuted)
        }
        startServiceIntent(intent)
    }

    fun stopService() {
        val intent = Intent(context, MainService::class.java)
        intent.action = "STOP_SERVICE"
        startServiceIntent(intent)
    }
}
