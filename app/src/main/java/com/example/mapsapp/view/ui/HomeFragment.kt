package com.example.mapsapp.view.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentHomeBinding
import com.example.mapsapp.view.chatbot.ChatBotActivity
import com.example.mapsapp.viewmodel.HomeViewModel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        homeViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            binding.emailTextView.text = "Welcome, ${user?.email}"
        })

        binding.mapsBtn.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToMapsActivity()
            findNavController().navigate(action)
        }

        binding.chatBtn.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToChatInterfaceFragment()
            findNavController().navigate(action)
        }

        binding.chatBotBtn.setOnClickListener {
            // ChatBotActivity'yi başlatmak için Intent kullanma
            val intent = Intent(activity, ChatBotActivity::class.java)
            startActivity(intent)
        }

        binding.signOutBtn.setOnClickListener {
            homeViewModel.logout()
            val action = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
