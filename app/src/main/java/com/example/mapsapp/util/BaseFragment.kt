package com.example.mapsapp.util
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R

abstract class BaseFragment : Fragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentDestination = findNavController().currentDestination?.id
                val previousDestination = findNavController().previousBackStackEntry?.destination?.id

                if (previousDestination == R.id.loginFragment || previousDestination == R.id.registerFragment) {
                    // Geri tuşu hiçbir şey yapmasın
                    return
                } else {
                    // Normal geri işlemi
                    isEnabled = false
                    requireActivity().onBackPressed()
                }
            }
        })
    }
}
