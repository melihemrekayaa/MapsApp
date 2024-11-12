package com.example.mapsapp.util

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.view.ui.HomeFragmentDirections

object NavigationHelper {

    fun navigateTo(fragment: Fragment, destination: String, receiverId: String? = null) {
        when (destination) {
            "Chat" -> {
                val action = HomeFragmentDirections.actionHomeFragmentToChatInterfaceFragment()
                fragment.findNavController().navigate(action)
            }
            "Chat Bot" -> {
                val action = HomeFragmentDirections.actionHomeFragmentToChatBotActivity()
                fragment.findNavController().navigate(action)
            }
            "Maps" -> {
                val action = HomeFragmentDirections.actionHomeFragmentToMapsActivity()
                fragment.findNavController().navigate(action)
            }
            "Voice Call" -> {
                val action = HomeFragmentDirections.actionHomeFragmentToChatInterfaceFragment()
                fragment.findNavController().navigate(action)
            }
            "Video Call" -> {
                val action = HomeFragmentDirections.actionHomeFragmentToChatInterfaceFragment()
                fragment.findNavController().navigate(action)
            }
        }
    }
}

