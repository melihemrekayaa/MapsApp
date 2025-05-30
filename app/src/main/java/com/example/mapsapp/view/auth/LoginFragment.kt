package com.example.mapsapp.view.auth

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.addCallback
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentLoginBinding
import com.example.mapsapp.viewmodel.AuthViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

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

        binding.registerRedirectText.setOnClickListener {
            val navController = findNavController()
            val action = LoginFragmentDirections.actionLoginFragmentToRegisterFragment()
            navController.navigate(action)
        }

        authViewModel.checkLoginState(forceDisable = requireActivity().intent?.getBooleanExtra("FORCE_LOGOUT", false) == true)

        setupListeners()
        observeViewModel()
    }

    private fun setupListeners(){
        binding.loginButton.setOnClickListener {
            val email = binding.emailEditTextLogin.text.toString().trim()
            val password = binding.passwordEditTextLogin.text.toString().trim()
            val staySignedIn = binding.checkBoxStaySignedIn.isChecked

            if (email.isNotEmpty() && password.isNotEmpty()) {
                toggleLoading(true)
                authViewModel.login(email, password, staySignedIn)
            } else {
                showToast("Please fill in all the fields.")
            }
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            authViewModel.authResult.collectLatest { result ->
                if (!isAdded || isDetached || view == null) return@collectLatest

                when (result) {
                    is AuthViewModel.AuthResult.Loading -> toggleLoading(true)
                    is AuthViewModel.AuthResult.Success -> {
                        toggleLoading(false)
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
        val user = authViewModel.getCurrentUser()
        if (user != null && !user.isEmailVerified) {
            showToast("Please verify your email before continuing.")
            // Eğer istersen burada direkt email gönderimi de tetikleyebilirsin:
            // user.sendEmailVerification()
            return
        }

        val navController = findNavController()
        val currentDestination = navController.currentDestination?.id

        Log.d("Navigation", "Current Destination: $currentDestination")
        Log.d("Navigation", "Attempting to navigate to HomeFragment")

        if (currentDestination != R.id.homeFragment) {
            val action = LoginFragmentDirections.actionLoginFragmentToHomeFragment()
            navController.navigate(action)
        } else {
            Log.d("Navigation", "Already on HomeFragment. Skipping navigation.")
        }
    }


    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
