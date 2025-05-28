package com.example.mapsapp.view.ui.components

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
    private var fragment: Fragment? = null

    fun setupNavigation(fragment: Fragment) {
        this.fragment = fragment

        val tabItems = listOf(
            Triple(binding.homeButton, binding.homeLabel, R.id.homeFragment to "Home"),
            Triple(binding.mapsButton, binding.mapsLabel, R.id.mapFragment to "Maps"),
            Triple(binding.chatButton, binding.chatLabel, R.id.chatInterfaceFragment to "Chat"),
            Triple(binding.settingsButton, binding.settingsLabel, R.id.settingsFragment to "Settings")
        )

        tabItems.forEach { (button, label, navData) ->
            button.setOnClickListener {
                selectTab(button.id)
                navigateIfNotOn(navData.first, navData.second)
            }
        }

        // Chat Bot butonu activity olduğu için ayrı yönetiyoruz
        binding.botButton.setOnClickListener {
            selectTab(binding.botButton.id)
            fragment?.let {
                NavigationHelper.navigateTo(it, "Chat Bot")
            }
        }

        // Başlangıçta Home seçili
        selectTab(binding.homeButton.id)
    }

    private fun navigateIfNotOn(destinationId: Int, destination: String) {
        fragment?.let { frag ->
            val navController = frag.findNavController()
            if (navController.currentDestination?.id != destinationId) {
                NavigationHelper.navigateTo(frag, destination)
            }
        }
    }

    private fun selectTab(selectedId: Int) {
        val tabIds = listOf(
            R.id.homeButton to binding.homeLabel,
            R.id.mapsButton to binding.mapsLabel,
            R.id.chatButton to binding.chatLabel,
            R.id.botButton to binding.botLabel,
            R.id.settingsButton to binding.settingsLabel
        )

        for ((id, label) in tabIds) {
            val tab = findViewById<LinearLayout>(id)
            val params = tab.layoutParams as LinearLayout.LayoutParams
            if (id == selectedId) {
                params.weight = 2f
                tab.setBackgroundResource(R.drawable.bg_selected_nav_item)
                animateLabelVisibility(label, true)
            } else {
                params.weight = 1f
                tab.setBackgroundColor(android.graphics.Color.TRANSPARENT)
                animateLabelVisibility(label, false)
            }
            tab.layoutParams = params
        }
    }

    private fun animateLabelVisibility(label: TextView?, visible: Boolean) {
        label ?: return

        if (visible) {
            label.visibility = View.VISIBLE
            label.alpha = 0f
            label.scaleX = 0.8f
            label.scaleY = 0.8f
            label.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(200)
                .start()
        } else {
            label.animate()
                .alpha(0f)
                .scaleX(0.8f)
                .scaleY(0.8f)
                .setDuration(200)
                .withEndAction {
                    label.visibility = View.GONE
                }
                .start()
        }
    }
}
