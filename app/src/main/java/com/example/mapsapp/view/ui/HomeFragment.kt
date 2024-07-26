package com.example.mapsapp.view.ui

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentHomeBinding
import com.example.mapsapp.view.chatbot.ChatBotActivity
import com.example.mapsapp.viewmodel.HomeViewModel
import dagger.hilt.android.AndroidEntryPoint


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

        homeViewModel.user.observe(viewLifecycleOwner) { user ->
            val userEmail = user?.email
            binding.emailTextView.text = "Welcome, $userEmail"
        }

        binding.mapsBtn.setOnClickListener {
            //findNavController().navigate(R.id.action_homeFragment_to_mapsActivity)
        }

        binding.chatBtn.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_chatInterfaceFragment)
        }

        binding.chatBotBtn.setOnClickListener {
            val intent = Intent(activity, ChatBotActivity::class.java)
            startActivity(intent)
        }

        binding.signOutBtn.setOnClickListener {
            homeViewModel.logout()
            findNavController().navigate(R.id.action_homeFragment_to_loginFragment)
        }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
