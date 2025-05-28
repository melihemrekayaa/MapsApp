package com.example.mapsapp.view.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.mapsapp.databinding.FragmentUpdateCredentialsBinding
import com.example.mapsapp.view.ui.UpdateCredentialsFragmentArgs
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class UpdateCredentialsFragment : Fragment() {

    private var _binding: FragmentUpdateCredentialsBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()
    private val args by navArgs<UpdateCredentialsFragmentArgs>()

    private var currentUser: FirebaseUser? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentUpdateCredentialsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        currentUser = auth.currentUser
        applyMode(args.type)
        setupListeners()
    }

    private fun applyMode(mode: String) {
        when (mode) {
            "email" -> {
                binding.etNewEmail.isVisible = true
                binding.etNewPassword.isVisible = false
                binding.etConfirmNewPassword.isVisible = false

                binding.etNewEmail.setText(currentUser?.email ?: "")
            }
            "password" -> {
                binding.etNewEmail.isVisible = false
                binding.etNewPassword.isVisible = true
                binding.etConfirmNewPassword.isVisible = true
            }
        }
    }

    private fun setupListeners() {
        binding.btnUpdate.setOnClickListener {
            val currentPassword = binding.etCurrentPassword.text.toString()
            val newEmail = binding.etNewEmail.text.toString()
            val newPassword = binding.etNewPassword.text.toString()
            val confirmPassword = binding.etConfirmNewPassword.text.toString()

            if (currentPassword.isBlank()) {
                showToast("Please enter your current password.")
                return@setOnClickListener
            }

            val user = currentUser
            if (user == null) {
                showToast("User not found.")
                return@setOnClickListener
            }

            val credential = EmailAuthProvider.getCredential(user.email ?: "", currentPassword)

            user.reauthenticate(credential).addOnCompleteListener { reauthTask ->
                if (!reauthTask.isSuccessful) {
                    showToast("Reauthentication failed.")
                    return@addOnCompleteListener
                }

                when (args.type) {
                    "email" -> {
                        if (newEmail.isBlank() || newEmail == user.email) {
                            showToast("Please enter a new email.")
                            return@addOnCompleteListener
                        }

                        user.updateEmail(newEmail).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                showToast("Email updated successfully.")
                                findNavController().navigateUp()
                            } else {
                                showToast("Email update failed: ${updateTask.exception?.message}")
                            }
                        }

                    }

                    "password" -> {
                        if (newPassword.length < 6 || newPassword != confirmPassword) {
                            showToast("Passwords must match and be at least 6 characters.")
                            return@addOnCompleteListener
                        }

                        user.updatePassword(newPassword).addOnCompleteListener { updateTask ->
                            if (updateTask.isSuccessful) {
                                showToast("Password updated successfully.")
                                findNavController().navigateUp()
                            } else {
                                showToast("Password update failed: ${updateTask.exception?.message}")
                            }
                        }

                    }
                }
            }
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
