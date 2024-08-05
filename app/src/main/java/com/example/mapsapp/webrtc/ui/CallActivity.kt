package com.example.mapsapp.webrtc.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CallActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_call) // Yeni layout dosyasını kullanacağız

        if (savedInstanceState == null) {
            val fragment = CallFragment().apply {
                arguments = intent.extras
            }
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_view, fragment)
                .commitNow()
        }
    }
}
