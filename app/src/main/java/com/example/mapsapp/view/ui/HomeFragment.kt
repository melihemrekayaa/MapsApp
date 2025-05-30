package com.example.mapsapp.view.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.CardAdapter
import com.example.mapsapp.databinding.FragmentHomeBinding
import com.example.mapsapp.repository.AuthRepository
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.util.DataProvider
import com.example.mapsapp.util.NavigationHelper
import com.example.mapsapp.viewmodel.AuthViewModel
import com.example.mapsapp.viewmodel.HomeViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var firebaseAuth: AuthRepository

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        observeUserInfo()
        setupRecyclerView()
        setupListeners()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        saveMyLocationOnce()
    }

    private fun observeUserInfo() {
        homeViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.welcomeMessage.text = "Welcome, ${user?.email ?: "User"}"
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = CardAdapter(DataProvider.getCardItems()) { cardItem ->
                NavigationHelper.navigateTo(this@HomeFragment, cardItem.title)
            }
        }
    }

    private fun setupListeners() {
        binding.exitBtn.setOnClickListener {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { dialog, _ ->
                authViewModel.logout()

                val navOptions = androidx.navigation.NavOptions.Builder()
                    .setPopUpTo(R.id.nav_graph, true) // 🔥 stack'i komple temizler
                    .setLaunchSingleTop(true)
                    .build()

                findNavController().navigate(R.id.loginFragment, null, navOptions)

                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }


    private fun saveMyLocationOnce() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED) return

        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        client.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
            .addOnSuccessListener { loc ->
                if (loc != null) {
                    val currentUser = FirebaseAuth.getInstance().currentUser
                    if (currentUser != null) {
                        FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(currentUser.uid)
                            .update("location", GeoPoint(loc.latitude, loc.longitude))
                    } else {
                        Toast.makeText(requireContext(), "Kullanıcı oturumu bulunamadı", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "Konum alınamadı", Toast.LENGTH_SHORT).show()
                }
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
