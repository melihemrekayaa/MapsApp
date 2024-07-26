package com.example.mapsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.navigation.findNavController
import androidx.navigation.ui.setupActionBarWithNavController
import com.example.mapsapp.databinding.ActivityMainBinding
import com.example.mapsapp.view.ui.ChatFragment
import dagger.hilt.android.AndroidEntryPoint


class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)


        val navController = findNavController(R.id.nav_host_fragment)
        setupActionBarWithNavController(navController)

        val receiverId = intent.getStringExtra("receiverId")
        if (receiverId != null) {

            val chatFragment = ChatFragment().apply {
                arguments = Bundle().apply {
                    putString("receiverId", receiverId)
                }
            }
            supportFragmentManager.commit {
                replace(R.id.nav_host_fragment, chatFragment)
                addToBackStack(null)
            }
        }

    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}
