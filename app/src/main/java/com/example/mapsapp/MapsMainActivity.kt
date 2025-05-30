package com.example.mapsapp

import android.content.Context
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.databinding.ActivityMainMapsBinding
import com.example.mapsapp.view.ui.components.CustomBottomNavView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMapsBinding
    private lateinit var bottomNav: CustomBottomNavView

    override fun onCreate(savedInstanceState: Bundle?) {
        applyDarkModeFromPreferences()
        super.onCreate(savedInstanceState)

        binding = ActivityMainMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bottomNav = findViewById(R.id.customButtonNav)

        setupNavigation() // sadece bu yeterli
    }





    private fun applyDarkModeFromPreferences() {
        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    private fun handleForceLogout(navController: NavController) {
        if (intent.getBooleanExtra("FORCE_LOGOUT", false)) {
            navController.navigate(R.id.loginFragment)
            intent.removeExtra("FORCE_LOGOUT") // tekrar tetiklenmesin diye
        }
    }


    private fun setupNavigation() {
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return
        val navController = navHostFragment.findNavController()

        handleForceLogout(navController)

        navController.addOnDestinationChangedListener { _, destination, _ ->
            bottomNav.visibility = when (destination.id) {
                R.id.loginFragment, R.id.registerFragment, R.id.chatFragment -> View.GONE
                else -> View.VISIBLE
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
        if (FirebaseAuth.getInstance().currentUser != null) {
            setupOnlineStatusRealtimeDb()
        }
    }

    override fun onResume() {
        super.onResume()

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as? NavHostFragment ?: return

        val currentFragment = navHostFragment.childFragmentManager.fragments.firstOrNull()
        currentFragment?.let {
            if (::bottomNav.isInitialized) {
                bottomNav.setupNavigation(it)
                val currentDestination = navHostFragment.navController.currentDestination?.id
                bottomNav.visibility = when (currentDestination) {
                    R.id.loginFragment, R.id.registerFragment -> View.GONE
                    else -> View.VISIBLE
                }
            }
        }
    }

}
