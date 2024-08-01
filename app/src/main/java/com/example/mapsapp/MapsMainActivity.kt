package com.example.mapsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.navigation.findNavController
import com.example.mapsapp.databinding.ActivityMainMapsBinding
import com.example.mapsapp.databinding.ActivityMapsBinding
import com.example.mapsapp.view.ui.ChatFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

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
