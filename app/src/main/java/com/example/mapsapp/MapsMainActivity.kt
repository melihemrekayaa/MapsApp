package com.example.mapsapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.mapsapp.databinding.ActivityMainMapsBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapsMainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainMapsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)


    }


}
