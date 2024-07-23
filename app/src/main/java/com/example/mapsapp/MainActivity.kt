package com.example.mapsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.mapsapp.databinding.ActivityMainBinding
import com.example.mapsapp.view.ui.ChatFragment

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ActivityMainBinding'i kullanarak görünümü ayarlayın
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Intent'ten receiverId'yi alın
        val receiverId = intent.getStringExtra("receiverId")
        if (receiverId != null) {
            // ChatFragment'i oluşturun ve argümanları ayarlayın
            val chatFragment = ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("receiverId", receiverId)
                }
            }
            // ChatFragment'i yerleştirin
            supportFragmentManager.commit {
                replace(R.id.nav_host_fragment, chatFragment)
                addToBackStack(null)
            }
        }
    }
}
