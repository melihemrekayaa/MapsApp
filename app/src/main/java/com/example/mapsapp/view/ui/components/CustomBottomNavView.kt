package com.example.mapsapp.view.ui.components

import android.content.Context
import android.content.Intent
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.CustomBottomNavBinding
import com.example.mapsapp.util.NavigationHelper

class CustomBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = CustomBottomNavBinding.inflate(LayoutInflater.from(context), this, true)
    private var fragment: Fragment? = null  // Store fragment reference

    fun setupNavigation(fragment: Fragment) {
        this.fragment = fragment  // Save the reference for later use

        binding.homeButton.setOnClickListener {
            navigateIfNotOn(R.id.homeFragment, "Home")
        }
        binding.mapsButton.setOnClickListener {
            navigateIfNotOn(R.id.mapFragment, "Maps") // ✅ navigation_graph içindeki ID
        }
        binding.chatButton.setOnClickListener {
            navigateIfNotOn(R.id.chatInterfaceFragment, "Chat")
        }
        binding.chatBotButton.setOnClickListener {
            navigateIfNotOn(R.id.chatBotActivity, "Chat Bot")
        }
        binding.settingsButton.setOnClickListener {
            navigateIfNotOn(R.id.settingsFragment, "Settings")
        }
    }

    private fun navigateIfNotOn(currentDestinationId: Int, destination: String) {
        fragment?.let { frag ->
            val navController = frag.findNavController()
            if (navController.currentDestination?.id != currentDestinationId) {
                NavigationHelper.navigateTo(frag, destination)
            }
        }
    }


}
