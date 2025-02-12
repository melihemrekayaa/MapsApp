package com.example.mapsapp.util

import android.app.Application
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.mapsapp.repository.AuthRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

class AppLifecycleListener @Inject constructor(
    private val authRepository: AuthRepository
) : DefaultLifecycleObserver {

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        val userId = authRepository.getCurrentUser()?.uid
        if (userId != null) {
            authRepository.setUserOnline(userId)
            Log.d("AppLifecycleListener", "User is now ONLINE: $userId")
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        super.onStop(owner)
        val userId = authRepository.getCurrentUser()?.uid
        if (userId != null) {
            authRepository.setUserOffline(userId)
            Log.d("AppLifecycleListener", "User is now OFFLINE: $userId")
        }
    }
}
