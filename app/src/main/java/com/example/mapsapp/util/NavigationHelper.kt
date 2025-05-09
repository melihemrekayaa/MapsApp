package com.example.mapsapp.util

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.view.ui.HomeFragmentDirections

object NavigationHelper {

    fun navigateTo(fragment: Fragment, destination: String, receiverId: String? = null, receiverName: String? = null) {
        when (destination) {
            "Home" -> {
                fragment.findNavController().navigate(R.id.homeFragment)
            }
            "Chat" -> {

                fragment.findNavController().navigate(R.id.chatInterfaceFragment)

            }
            "Chat Bot" -> {
                fragment.findNavController().navigate(R.id.chatBotActivity)
            }
            "Maps" -> {
                fragment.findNavController().navigate(R.id.mapFragment)
            }
            "Settings" -> {
                fragment.findNavController().navigate(R.id.settingsFragment)
            }
        }
    }
}
