package com.example.mapsapp

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.setupWithNavController
import com.example.mapsapp.databinding.ActivityMainMapsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment)
        val navController = navHostFragment?.findNavController()
        val bottomNavigationView = binding.bottomNavBar
        binding.bottomNavBar.setupWithNavController(navController!!)



        navController.addOnDestinationChangedListener{
                _,destination,_ ->
            when(destination.id){
                R.id.loginFragment -> bottomNavigationView.visibility = View.GONE
                R.id.registerFragment -> bottomNavigationView.visibility = View.GONE
                R.id.chatFragment -> bottomNavigationView.visibility = View.GONE
                else -> bottomNavigationView.visibility = View.VISIBLE
            }
        }




    }


}
