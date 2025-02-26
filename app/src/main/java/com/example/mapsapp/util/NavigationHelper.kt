package com.example.mapsapp.util

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.view.ui.HomeFragmentDirections

object NavigationHelper {

    fun navigateTo(fragment: Fragment, destination: String, receiverId: String? = null, receiverName: String? = null) {
        when (destination) {
            "Home" -> {
                fragment.findNavController().navigate(R.id.action_global_homeFragment)
            }
            "Chat" -> {

                fragment.findNavController().navigate(R.id.action_global_chatInterfaceFragment)

            }
            "Chat Bot" -> {
                fragment.findNavController().navigate(R.id.action_global_chatBotActivity)
            }
            "Maps" -> {
                fragment.findNavController().navigate(R.id.action_global_mapsActivity)
            }
            "Settings" -> {
                fragment.findNavController().navigate(R.id.action_global_settingsFragment)
            }
        }
    }
}
