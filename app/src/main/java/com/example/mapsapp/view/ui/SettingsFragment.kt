package com.example.mapsapp.view.ui

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentSettingsBinding
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import java.io.ByteArrayOutputStream

@AndroidEntryPoint
class SettingsFragment : Fragment(R.layout.fragment_settings) {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private lateinit var galleryLauncher: ActivityResultLauncher<String>
    private lateinit var cameraLauncher: ActivityResultLauncher<Uri>
    private var imageUri: Uri? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        _binding = FragmentSettingsBinding.bind(view)

        loadUserInfo()
        setupClickListeners()
        setupActivityResultLaunchers()
    }

    private fun loadUserInfo() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { snapshot ->
                val name = snapshot.getString("name") ?: "User"
                val base64Photo = snapshot.getString("photoBase64")

                binding.tvUserName.text = name

                if (!base64Photo.isNullOrBlank()) {
                    val imageBytes = Base64.decode(base64Photo, Base64.NO_WRAP)
                    Glide.with(this)
                        .asBitmap()
                        .load(imageBytes)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(binding.profileImage)
                } else {
                    Glide.with(this)
                        .load(R.drawable.ic_profile_placeholder)
                        .circleCrop()
                        .into(binding.profileImage)
                }
            }
    }

    private fun setupClickListeners() {
        binding.rowEditProfile.setOnClickListener {
            val action = SettingsFragmentDirections
                .actionSettingsFragmentToUpdateCredentialsFragment("email")
            findNavController().navigate(action)
        }

        binding.rowChangePassword.setOnClickListener {
            val action = SettingsFragmentDirections
                .actionSettingsFragmentToUpdateCredentialsFragment("password")
            findNavController().navigate(action)
        }

        binding.rowLanguage.setOnClickListener {
            Toast.makeText(requireContext(), "Language settings clicked", Toast.LENGTH_SHORT).show()
        }

        binding.rowLogout.setOnClickListener {
            auth.signOut()
            Toast.makeText(requireContext(), "Logged out", Toast.LENGTH_SHORT).show()
            findNavController().navigate(R.id.action_settingsFragment_to_loginFragment)
        }

        binding.profileCard.setOnClickListener {
            showImagePickerBottomSheet()
        }
    }

    private fun setupActivityResultLaunchers() {
        galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { handleImageUri(it) }
        }

        cameraLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            if (success && imageUri != null) {
                handleImageUri(imageUri!!)
            }
        }
    }

    private fun showImagePickerBottomSheet() {
        val dialog = BottomSheetDialog(requireContext())
        val view = LayoutInflater.from(context).inflate(R.layout.layout_bottom_sheet_profile_image, null)

        view.findViewById<LinearLayout>(R.id.optionGallery).setOnClickListener {
            dialog.dismiss()
            openGallery()
        }

        view.findViewById<LinearLayout>(R.id.optionCamera).setOnClickListener {
            dialog.dismiss()
            openCamera()
        }

        dialog.setContentView(view)
        dialog.show()
    }

    private fun openGallery() {
        galleryLauncher.launch("image/*")
    }

    private fun openCamera() {
        val uri = createImageUri()
        imageUri = uri
        cameraLauncher.launch(uri)
    }


    private fun createImageUri(): Uri {
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "profile_photo.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
        }

        return requireContext().contentResolver.insert(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        )!!
    }

    private fun handleImageUri(uri: Uri) {
        val bitmap = if (Build.VERSION.SDK_INT < 28) {
            MediaStore.Images.Media.getBitmap(requireContext().contentResolver, uri)
        } else {
            val source = ImageDecoder.createSource(requireContext().contentResolver, uri)
            ImageDecoder.decodeBitmap(source)
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream)
        val imageBytes = byteArrayOutputStream.toByteArray()
        val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)

        saveProfileBase64ToFirestore(base64String)

        Glide.with(this)
            .load(uri)
            .placeholder(R.drawable.ic_profile_placeholder)
            .circleCrop()
            .into(binding.profileImage)
    }

    private fun saveProfileBase64ToFirestore(base64: String) {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid)
            .update("photoBase64", base64)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile photo updated.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to update profile photo.", Toast.LENGTH_SHORT).show()
            }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
