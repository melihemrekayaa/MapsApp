package com.example.mapsapp.view.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.MapsMainActivity
import com.example.mapsapp.databinding.FragmentUpdateCredentialsBinding
import com.example.mapsapp.viewmodel.UpdateCredentialsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
        super.onViewCreated(view, savedInstanceState)
        observeStatus()

        binding.btnUpdateEmail.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString().trim()
            val newEmail = binding.etNewEmail.text.toString().trim()

            if (currentPassword.isEmpty() || newEmail.isEmpty()) {
                showToast("Please enter both current password and new email")
                return@setOnClickListener
            }

            viewModel.reauthenticateAndChangeEmail(currentPassword, newEmail)
        }

        binding.btnUpdatePassword.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString().trim()
            val newPassword = binding.etNewPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmNewPassword.text.toString().trim()

            if (currentPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showToast("Please fill in all password fields")
                return@setOnClickListener
            }

            if (newPassword != confirmPassword) {
                showToast("New passwords do not match")
                return@setOnClickListener
            }

            if (newPassword.length < 6) {
                showToast("Password must be at least 6 characters")
                return@setOnClickListener
            }

            viewModel.updatePassword(currentPassword, newPassword)
        }
    }

    private fun observeStatus() {
        lifecycleScope.launch {
            viewModel.updateStatus.collect { status ->
                status?.let {
                    showToast(it)
                    disableButtons()

                    // Hem email hem şifre güncellemede aynı logout akışı
                    if (it.contains("updated successfully", ignoreCase = true)) {
                        logoutAndRedirect()
                    }

                    viewModel.clearStatus()
                }
            }
        }
    }

    private fun disableButtons() {
        binding.btnSendVerification.isEnabled = false
        binding.btnUpdatePassword.isEnabled = false
        binding.btnUpdateEmail.isEnabled = false
    }

    private fun logoutAndRedirect() {
        viewModel.logoutAndClearCredentials()
        viewModel.clearStaySignedIn()

        val intent = Intent(requireContext(), MapsMainActivity::class.java)
        intent.putExtra("FORCE_LOGOUT", true)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun onResume() {
        super.onResume()
        val user = viewModel.getCurrentUser()
        if (user == null) {
            logoutAndRedirect()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
