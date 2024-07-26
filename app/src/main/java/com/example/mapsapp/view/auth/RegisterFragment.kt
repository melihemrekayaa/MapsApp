package com.example.mapsapp.view.auth

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentRegisterBinding
import com.example.mapsapp.model.User
import com.example.mapsapp.viewmodel.AuthViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var locationManager: LocationManager
    private lateinit var locationListener: LocationListener
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private var currentLocation : GeoPoint ?= null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        locationManager = requireActivity().getSystemService(LocationManager::class.java)

        permissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()){ granted ->
            if (granted) {
                getUserLocation()
            } else {
                Toast.makeText(requireContext(), "Konum izni reddedildi", Toast.LENGTH_SHORT).show()
            }
        }

        // Kullanıcının konumunu burada almak
        getUserLocation()

        binding.registerButton.setOnClickListener {
            registerUser()
        }

        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                saveUserLocation(uid = user.uid)
                Toast.makeText(requireContext(), "Kayıt başarılı", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_registerFragment_to_homeFragment)
            } else {
                Toast.makeText(requireContext(), "Kayıt başarısız.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserLocation(uid: String){
        val userData = hashMapOf(
            "uid" to uid,
            "email" to authViewModel.user.value?.email,
            "location" to currentLocation
        )

        firestore.collection("users").document(uid).set(userData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Location Saved", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Location Couldn't Saved", Toast.LENGTH_SHORT).show()
            }
    }

    private fun getUserLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationListener = object : LocationListener {
                override fun onLocationChanged(location: Location) {
                    currentLocation = GeoPoint(location.latitude, location.longitude)
                    locationManager.removeUpdates(this)
                }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, locationListener)
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun registerUser(){
        val email = binding.emailEditTextRegister.text.toString()
        val password = binding.passwordEditTextRegister.text.toString()

        getUserLocation()

        if (email.isNotEmpty() && password.isNotEmpty() && currentLocation != null) {
            authViewModel.register(email, password, currentLocation!!)
        } else {
            Toast.makeText(requireContext(), "Fill in the blanks", Toast.LENGTH_SHORT).show()
        }
    }
}