package com.example.mapsapp.webrtc.service

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import javax.inject.Inject

class MainServiceRepository @Inject constructor(
    private val context: Context
) {

    companion object {
        private const val TAG = "MainServiceRepository"
    }

    /**
     * Servisi başlatan fonksiyon
     */
    fun startService(username: String) {
        if (!checkPermissions()) {
            Log.e(TAG, "Gerekli izinler verilmedi! Servis başlatılamıyor.")
            return
        }

        val intent = Intent(context, MainService::class.java).apply {
            putExtra("username", username)
            action = "START_SERVICE"
        }
        startServiceIntent(intent)
    }

    /**
     * Foreground servisi başlatır.
     */
    private fun startServiceIntent(intent: Intent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    /**
     * Kamera ve arama görünümlerini başlatır.
     */
    fun setupViews(videoCall: Boolean, caller: Boolean, target: String) {
        val intent = Intent(context, MainService::class.java).apply {
            action = "SETUP_VIEWS"
            putExtra("isVideoCall", videoCall)
            putExtra("target", target)
            putExtra("isCaller", caller)
        }
        startServiceIntent(intent)
    }

    /**
     * Çağrıyı bitirir.
     */
    fun sendEndCall() {
        val intent = Intent(context, MainService::class.java).apply {
            action = "END_CALL"
        }
        startServiceIntent(intent)
    }

    /**
     * Kamerayı değiştirir (Ön - Arka kamera geçişi).
     */
    fun switchCamera() {
        val intent = Intent(context, MainService::class.java).apply {
            action = "SWITCH_CAMERA"
        }
        startServiceIntent(intent)
    }

    /**
     * Mikrofon sesini açıp kapatır.
     */
    fun toggleAudio(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java).apply {
            action = "TOGGLE_AUDIO"
            putExtra("shouldBeMuted", shouldBeMuted)
        }
        startServiceIntent(intent)
    }

    /**
     * Kamera görüntüsünü açıp kapatır.
     */
    fun toggleVideo(shouldBeMuted: Boolean) {
        val intent = Intent(context, MainService::class.java).apply {
            action = "TOGGLE_VIDEO"
            putExtra("shouldBeMuted", shouldBeMuted)
        }
        startServiceIntent(intent)
    }

    /**
     * Servisi durdurur.
     */
    fun stopService() {
        val intent = Intent(context, MainService::class.java).apply {
            action = "STOP_SERVICE"
        }
        startServiceIntent(intent)
    }

    /**
     * Android 13 ve sonrası için gerekli izinleri kontrol eder.
     */
    private fun checkPermissions(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permissions = listOf(
                android.Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION
            )
            return permissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        }
        return true
    }
}
