package com.example.mapsapp.view.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentUpdateCredentialsBinding
import com.example.mapsapp.viewmodel.UpdateCredentialsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest

@AndroidEntryPoint
class UpdateCredentialsFragment : Fragment() {

    private var _binding: FragmentUpdateCredentialsBinding? = null
    private val binding get() = _binding!!
    private val viewModel: UpdateCredentialsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateCredentialsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        applyMode()
        setupListeners()
        observeViewModel()
    }

    private fun applyMode() {
        // Varsayılan olarak e-posta güncelleme modunu açar
        binding.etNewEmail.isVisible = true
        binding.etNewPassword.isVisible = false
        binding.etConfirmNewPassword.isVisible = false
        binding.btnSendVerification.isVisible = true
        binding.btnUpdateEmail.isVisible = true
        binding.btnUpdatePassword.isVisible = false
    }

    private fun setupListeners() {
        binding.btnSendVerification.setOnClickListener {
            val email = binding.etNewEmail.text.toString().trim()
            if (email.isEmpty()) {
                toast("New email cannot be empty.")
            } else {
                viewModel.sendVerificationMail(email)
            }
        }

        binding.btnUpdateEmail.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newEmail = binding.etNewEmail.text.toString()
            if (currentPassword.isBlank() || newEmail.isBlank()) {
                toast("Please fill in all fields.")
                return@setOnClickListener
            }
            viewModel.updateEmailAfterVerification(currentPassword, newEmail)
        }

        binding.btnUpdatePassword.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmNewPassword.text.toString()

            if (currentPassword.isBlank() || newPassword.isBlank() || confirmPassword.isBlank()) {
                toast("Please fill in all fields.")
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                toast("Passwords do not match.")
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                toast("Password must be at least 6 characters.")
                return@setOnClickListener
            }

            viewModel.updatePassword(currentPassword, newPassword)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launchWhenStarted {
            viewModel.updateStatus.collectLatest { status ->
                status?.let {
                    toast(it)

                    if (it.contains("Verification email sent", true) || it.contains("Email updated", true)) {
                        findNavController().navigate(R.id.loginFragment) {
                            popUpTo(R.id.nav_graph) {
                                inclusive = true
                            }
                            launchSingleTop = true
                        }
                    }

                    viewModel.clearStatus()
                }
            }
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_LONG).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
