package com.example.mapsapp.view.auth

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.databinding.FragmentRegisterBinding
import com.example.mapsapp.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class RegisterFragment : Fragment() {

    private var _binding: FragmentRegisterBinding? = null
    private val binding get() = _binding!!
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRegisterBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.loginRedirectText.setOnClickListener {
            val action = RegisterFragmentDirections.actionRegisterFragmentToLoginFragment()
            findNavController().navigate(action)
        }

        // Register Button Click Listener
        binding.registerButton.setOnClickListener {
            val name = binding.nameEditTextRegister.text.toString().trim()
            val email = binding.emailEditTextRegister.text.toString().trim()
            val password = binding.passwordEditTextRegister.text.toString().trim()

            if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                toggleLoading(true) // Show ProgressBar, hide button
                authViewModel.register(name, email, password)
            } else {
                showToast("Please fill in all the fields.")
            }
        }

        // Observe Auth Result from ViewModel
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.authResult.collectLatest { result ->
                when (result) {
                    is AuthViewModel.AuthResult.Loading -> {
                        toggleLoading(true)
                    }
                    is AuthViewModel.AuthResult.Success -> {
                        toggleLoading(false)
                        showToast(result.message)
                        navigateToHome()
                    }
                    is AuthViewModel.AuthResult.Error -> {
                        toggleLoading(false)
                        showToast(result.message)
                    }
                    null -> Unit
                }
            }
        }
    }

    // Toggle ProgressBar and Button Visibility
    private fun toggleLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.registerButton.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    // Navigate to Home Fragment
    private fun navigateToHome() {
        val action = RegisterFragmentDirections.actionRegisterFragmentToHomeFragment()
        findNavController().navigate(action)
    }

    // Show Toast Message
    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
