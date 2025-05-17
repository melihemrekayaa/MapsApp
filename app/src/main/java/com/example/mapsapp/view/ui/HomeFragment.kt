package com.example.mapsapp.view.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.adapter.CardAdapter
import com.example.mapsapp.databinding.FragmentHomeBinding
import com.example.mapsapp.repository.AuthRepository
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.util.DataProvider
import com.example.mapsapp.util.NavigationHelper
import com.example.mapsapp.viewmodel.AuthViewModel
import com.example.mapsapp.viewmodel.HomeViewModel
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
                val action = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
                findNavController().navigate(action)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
