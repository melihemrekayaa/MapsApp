package com.example.mapsapp.view.ui

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
import com.example.mapsapp.webrtc.service.MainServiceRepository
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val view = binding.root

        mainServiceRepository.startService(firebaseAuth.getCurrentUser()!!.uid)

        val recyclerView: RecyclerView = binding.recyclerView
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        recyclerView.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )

        homeViewModel.user.observe(viewLifecycleOwner, Observer { user ->
            binding.welcomeMessage.text = "Welcome, ${user?.email}"
        })

        recyclerView.adapter = CardAdapter(DataProvider.getCardItems()) { cardItem ->
            if (cardItem.title == "Chat") {
                NavigationHelper.navigateTo(this, "Chat", "receiver_user_id") // receiverId gönderiliyor
            } else {
                NavigationHelper.navigateTo(this, cardItem.title)
            }
        }

        binding.exitBtn.setOnClickListener {
            showExitConfirmationDialog()
        }




        return view
    }

    private fun showExitConfirmationDialog() {
        val alertDialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirm Exit")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { dialog, _ ->
                authViewModel.logout()
                val action = HomeFragmentDirections.actionHomeFragmentToLoginFragment()
                findNavController().navigate(action)
                dialog.dismiss() // Dialogu kapat
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss() // Dialogu kapat
            }
            .create()

        alertDialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
