package com.example.mapsapp.util

import android.content.Intent
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.view.chatbot.ChatBotActivity

object NavigationHelper {

    fun navigateTo(fragment: Fragment, destination: String) {
        when (destination) {
            "Home" -> {
                fragment.findNavController().navigate(R.id.homeFragment)
            }
            "Chat" -> {
                fragment.findNavController().navigate(R.id.chatInterfaceFragment)
            }
            "Chat Bot" -> {
                val intent = Intent(fragment.requireContext(), ChatBotActivity::class.java)
                fragment.startActivity(intent)
            }
            "Maps" -> {
                fragment.findNavController().navigate(R.id.mapFragment)
            }
            "Settings" -> {
                fragment.findNavController().navigate(R.id.settingsFragment)
            }
            else -> {
                // Log veya fallback
            }
        }
    }
}
