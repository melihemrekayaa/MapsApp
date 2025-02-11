package com.example.mapsapp.view.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.example.mapsapp.databinding.CustomBottomNavBinding
import com.example.mapsapp.util.NavigationHelper

class CustomBottomNavView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = CustomBottomNavBinding.inflate(LayoutInflater.from(context), this, true)

    fun setupNavigation(fragment: androidx.fragment.app.Fragment) {
        binding.homeButton.setOnClickListener {
            NavigationHelper.navigateTo(fragment, "Home")
        }
        binding.mapsButton.setOnClickListener {
            NavigationHelper.navigateTo(fragment, "Maps")
        }
        binding.chatButton.setOnClickListener {
            NavigationHelper.navigateTo(fragment, "Chat")
        }
        binding.chatBotButton.setOnClickListener {
            NavigationHelper.navigateTo(fragment, "Chat Bot")
        }
        binding.voiceCallButton.setOnClickListener {
            NavigationHelper.navigateTo(fragment, "Voice Call")
        }
    }
}
