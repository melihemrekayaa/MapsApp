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
import com.example.mapsapp.webrtc.CallActivity
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class HomeFragment : BaseFragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private val homeViewModel: HomeViewModel by viewModels()
    private val authViewModel: AuthViewModel by viewModels()
    private var callListenerAdded = false
    private val firebaseDb = FirebaseDatabase.getInstance()


    @Inject
    lateinit var firebaseAuth: AuthRepository


    private lateinit var currentUserId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        currentUserId = firebaseAuth.getCurrentUser()?.uid ?: ""

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
                    val receiverUid = "receiver_user_id" // 🟡 TODO: Gerçek kullanıcı UID'si ile değiştir
                    val isVideoCall = true // veya false, butona göre ayarlanabilir
                    homeViewModel.sendCallRequest(receiverUid, isVideoCall)
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
        val myUid = homeViewModel.user.value?.uid ?: return

        FirebaseDatabase.getInstance()
            .getReference("callRequests")
            .child(myUid)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val callerUid = snapshot.child("callerUid").getValue(String::class.java) ?: return
                    val roomId = snapshot.child("roomId").getValue(String::class.java) ?: return
                    val isVideoCall = snapshot.child("isVideoCall").getValue(Boolean::class.java) ?: true

                    // Bildirimi göster
                    showIncomingCallDialog(roomId, callerUid, isVideoCall)
                }

                override fun onCancelled(error: DatabaseError) {}
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            })
    }



    private fun showIncomingCallDialog(roomId: String, callerUid: String, isVideoCall: Boolean) {
        AlertDialog.Builder(requireContext())
            .setTitle("Gelen ${if (isVideoCall) "Görüntülü" else "Sesli"} Çağrı")
            .setMessage("$callerUid seni arıyor.")
            .setPositiveButton("Kabul Et") { _, _ ->
                val intent = Intent(requireContext(), CallActivity::class.java).apply {
                    putExtra("roomId", roomId)
                    putExtra("callerUid", callerUid)
                    putExtra("isCaller", false)
                    putExtra("isVideoCall", isVideoCall)
                }
                startActivity(intent)
            }
            .setNegativeButton("Reddet") { _, _ ->
                FirebaseDatabase.getInstance()
                    .getReference("calls")
                    .child(roomId)
                    .child("status")
                    .setValue("rejected")
            }
            .setCancelable(false)
            .show()
    }




    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

