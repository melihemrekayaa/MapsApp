package com.example.mapsapp.view.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentRegisterBinding
import com.example.mapsapp.viewmodel.AuthViewModel

class RegisterFragment : Fragment() {
    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.registerButton.setOnClickListener {
            val email = binding.emailEditTextRegister.text.toString()
            val password = binding.passwordEditTextRegister.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                authViewModel.register(email, password)
            } else {
                Toast.makeText(requireContext(), "Bilgileri doldurun", Toast.LENGTH_SHORT).show()
            }
        }

        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Toast.makeText(requireContext(), "Kayıt başarılı", Toast.LENGTH_SHORT)
                    .show()
                findNavController().navigate(R.id.action_registerFragment_to_mapsActivity)
            } else {
                Toast.makeText(requireContext(), "Kayıt başarısız.", Toast.LENGTH_SHORT).show()
            }
        }
    }

}