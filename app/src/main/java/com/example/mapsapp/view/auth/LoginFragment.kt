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
import com.example.mapsapp.databinding.FragmentLoginBinding
import com.example.mapsapp.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class LoginFragment : Fragment() {

    private val authViewModel: AuthViewModel by viewModels()
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Kullanıcı zaten giriş yaptıysa ana sayfaya yönlendir
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.isUserLoggedIn.collectLatest {
                if (it){
                    navigateToHome()
                }
            }
        }

        // Login Button Click
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditTextLogin.text.toString().trim()
            val password = binding.passwordEditTextLogin.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                toggleLoading(true) // ProgressBar göster, butonu gizle
                authViewModel.login(email, password)
            } else {
                showToast("Please fill in all the fields.")
            }
        }

        // Redirect to Register Page
        binding.registerRedirectText.setOnClickListener {
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            findNavController().navigate(action)
        }

        // ViewModel’den giriş durumu dinleme
        viewLifecycleOwner.lifecycleScope.launchWhenStarted {
            authViewModel.authResult.collectLatest { result ->
                when (result) {
                    is AuthViewModel.AuthResult.Loading -> toggleLoading(true)
                    is AuthViewModel.AuthResult.Success -> {
                        toggleLoading(false)
                        showToast("Login Successful")
                        navigateToHome()
                    }
                    is AuthViewModel.AuthResult.Error -> {
                        toggleLoading(false)
                        showToast(result.message)
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun toggleLoading(isLoading: Boolean) {
        binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
        binding.loginButton.visibility = if (isLoading) View.GONE else View.VISIBLE
    }

    private fun navigateToHome() {
        val action = LoginFragmentDirections.actionLoginFragmentToHomeFragment()
        findNavController().navigate(action)
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
