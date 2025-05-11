package com.example.mapsapp.util
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mapsapp.R
import com.example.mapsapp.webrtc.IncomingCallActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ChildEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase

abstract class BaseFragment : Fragment() {

    override fun onStart() {
        super.onStart()
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseDatabase.getInstance()
            .getReference("callRequests")
            .child(uid)
            .addChildEventListener(object : ChildEventListener {
                override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                    val roomId = snapshot.child("roomId").getValue(String::class.java) ?: return
                    val callerUid = snapshot.child("callerUid").getValue(String::class.java) ?: return
                    val isVideoCall = snapshot.child("isVideoCall").getValue(Boolean::class.java) ?: true

                    snapshot.ref.removeValue() // 🔥 Sadece 1 kez tetiklenmesi için

                    val intent = Intent(requireContext(), IncomingCallActivity::class.java).apply {
                        putExtra("roomId", roomId)
                        putExtra("callerUid", callerUid)
                        putExtra("isVideoCall", isVideoCall)
                    }
                    startActivity(intent)
                }

                override fun onCancelled(error: DatabaseError) {}
                override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
                override fun onChildRemoved(snapshot: DataSnapshot) {}
                override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            })
    }


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
    protected open fun applyBottomInsetToView(targetView: View) {
        view?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { _, insets ->
                val navBarInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                val params = targetView.layoutParams as ViewGroup.MarginLayoutParams
                params.bottomMargin = navBarInsets.bottom + 16 // Navigation bar yüksekliği kadar ekle
                targetView.layoutParams = params
                insets
            }
        }
    }


}
