package com.example.mapsapp.view.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.adapter.CardAdapter
import com.example.mapsapp.databinding.FragmentHomeBinding
import com.example.mapsapp.repository.AuthRepository
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.util.DataProvider
import com.example.mapsapp.util.NavigationHelper
import com.example.mapsapp.viewmodel.AuthViewModel
import com.example.mapsapp.viewmodel.HomeViewModel
import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.example.mapsapp.webrtc.model.DataModel
import com.example.mapsapp.webrtc.service.MainServiceRepository
import com.example.mapsapp.webrtc.ui.CallActivity
import com.example.mapsapp.webrtc.utils.DataModelType
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()

    @Inject
    lateinit var mainServiceRepository: MainServiceRepository

    @Inject
    lateinit var firebaseAuth: AuthRepository

    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        currentUserId = firebaseAuth.getCurrentUser()?.uid ?: ""
        mainServiceRepository.startService(currentUserId)

        observeUserInfo()
        setupRecyclerView()
        setupListeners()
        listenForIncomingCalls()

        return binding.root
    }

    private fun observeUserInfo() {
        homeViewModel.user.observe(viewLifecycleOwner) { user ->
            binding.welcomeMessage.text = "Welcome, ${user?.email}"
        }
    }

    private fun setupRecyclerView() {
        binding.recyclerView.apply {
            layoutManager = GridLayoutManager(requireContext(), 2)
            addItemDecoration(DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL))
            adapter = CardAdapter(DataProvider.getCardItems()) { cardItem ->
                if (cardItem.title == "Chat") {
                    NavigationHelper.navigateTo(this@HomeFragment, "Chat", "receiver_user_id")
                } else {
                    NavigationHelper.navigateTo(this@HomeFragment, cardItem.title)
                }
            }
        }
    }

    private fun setupListeners() {
        binding.exitBtn.setOnClickListener {
            showExitConfirmationDialog()
        }
    }

    private fun showExitConfirmationDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { dialog, _ ->
                authViewModel.logout()
                val action = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
                findNavController().navigate(action)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .create()

        alertDialog.show()
    }

    private fun listenForIncomingCalls() {
        FirebaseFirestore.getInstance().collection("calls")
            .document(currentUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val call = snapshot.toObject(DataModel::class.java) ?: return@addSnapshotListener
                if (!call.isValid() || call.target != currentUserId) return@addSnapshotListener

                showIncomingCallDialog(call)
            }
    }

    private fun showIncomingCallDialog(call: DataModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Incoming ${call.type?.name}")
            .setMessage("${call.sender} is calling you.")
            .setPositiveButton("Accept") { _, _ ->
                FirebaseClient().acceptCall(call.target!!)
                startActivity(Intent(requireContext(), CallActivity::class.java).apply {
                    putExtra("target", call.sender)
                    putExtra("isVideoCall", call.type == DataModelType.StartVideoCall)
                    putExtra("isCaller", false)
                })
            }
            .setNegativeButton("Reject") { _, _ ->
                FirebaseClient().rejectCall(call.target!!)
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

