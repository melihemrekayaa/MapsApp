package com.example.mapsapp.view.ui.auth

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentLoginBinding
import com.example.mapsapp.viewmodel.AuthViewModel

class LoginFragment : Fragment() {

    private lateinit var authViewModel: AuthViewModel
    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        authViewModel = ViewModelProvider(this)[AuthViewModel::class.java]
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val view = binding.root

        binding.loginBtn.setOnClickListener {
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()

            authViewModel.login(email, password)
        }

        binding.registerBtn.setOnClickListener {
            findNavController().navigate(R.id.action_loginFragment_to_registerFragment)
        }

        authViewModel.user.observe(viewLifecycleOwner) { user ->
            if (user != null) {
                Toast.makeText(requireContext(), "Giriş Başarılı", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.action_loginFragment_to_mapFragment)
            }
            else{
                Toast.makeText(requireContext(), "Giriş Başarısız", Toast.LENGTH_SHORT).show()
            }
        }




        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}