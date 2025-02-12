package com.example.mapsapp

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.example.mapsapp.util.AppLifecycleListener
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class MainApplication : Application() {

    @Inject
    lateinit var appLifecycleListener: AppLifecycleListener

    override fun onCreate() {
        super.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(appLifecycleListener)
    }
}