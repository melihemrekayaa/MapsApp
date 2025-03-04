package com.example.mapsapp.webrtc.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.ui.CloseActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainServiceReceiver : BroadcastReceiver() {

    @Inject lateinit var serviceRepository: MainServiceRepository
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == "ACTION_EXIT"){
            //we want to exit the whole application
            serviceRepository.stopService()
            context?.startActivity(Intent(context, CloseActivity::class.java))

        }

    }
}