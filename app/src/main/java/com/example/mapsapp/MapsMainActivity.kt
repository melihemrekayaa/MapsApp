package com.example.mapsapp

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mapsapp.databinding.ActivityMainMapsBinding
import com.example.mapsapp.view.ui.components.CustomBottomNavView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        // ✅ Dark Mode ayarı
        applyDarkModeFromPreferences()

        super.onCreate(savedInstanceState)
        binding = ActivityMainMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupNavigation()
    }

    private fun applyDarkModeFromPreferences() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
        val navController = navHostFragment?.findNavController()
        val bottomNavigationView = findViewById<CustomBottomNavView>(R.id.customButtonNav)

        // İlk açılışta doğru fragment için setup
        val currentFragment = navHostFragment?.childFragmentManager?.fragments?.firstOrNull()
        currentFragment?.let { bottomNavigationView.setupNavigation(it) }

        // Fragment değiştikçe bottom nav visibility güncelle
        navController?.addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.loginFragment, R.id.registerFragment, R.id.chatFragment -> {
                    bottomNavigationView.visibility = View.GONE
                }

                else -> {
                    bottomNavigationView.visibility = View.VISIBLE
                }
            }
        }
    }

    private fun setupOnlineStatusRealtimeDb() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val ref = FirebaseDatabase.getInstance().getReference("usersOnlineStatus/$uid")

        ref.setValue(
            mapOf(
                "isOnline" to true,
                "lastSeen" to ServerValue.TIMESTAMP
            )
        )

        ref.onDisconnect().setValue(
            mapOf(
                "isOnline" to false,
                "lastSeen" to ServerValue.TIMESTAMP
            )
        )
    }

    override fun onStart() {
        super.onStart()
        setupOnlineStatusRealtimeDb()
    }
}
