package com.example.mapsapp.webrtc.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.mapsapp.webrtc.ui.CloseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainServiceReceiver : BroadcastReceiver() {

    @Inject lateinit var serviceRepository: MainServiceRepository

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "ACTION_EXIT") {
            // Hizmeti durdurmak ve uygulamayı kapatmak için gerekli işlemleri yap
            serviceRepository.stopService()

            context?.let {
                // CloseActivity'i başlat ve mevcut activity yığınını temizle
                val closeIntent = Intent(it, CloseActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                it.startActivity(closeIntent)
            }
        }
    }
}
