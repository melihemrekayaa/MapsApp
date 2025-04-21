package com.example.mapsapp.view.ui

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.mapsapp.R
import com.example.mapsapp.adapter.ChatAdapter
import com.example.mapsapp.databinding.FragmentChatBinding
import com.example.mapsapp.model.Event
import com.example.mapsapp.model.User
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.viewmodel.ChatViewModel
import com.example.mapsapp.webrtc.firebaseClient.FirebaseClient
import com.example.mapsapp.webrtc.model.DataModel
import com.example.mapsapp.webrtc.repository.MainRepository
import com.example.mapsapp.webrtc.ui.CallActivity
import com.example.mapsapp.webrtc.utils.DataModelType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ChatFragment : BaseFragment() {

    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private val chatViewModel: ChatViewModel by viewModels()
    private lateinit var adapter: ChatAdapter
    private var receiverId: String? = null
    private var receiverName: String? = null
    private var userListener: ListenerRegistration? = null
    private lateinit var toolbar: androidx.appcompat.widget.Toolbar
    private var callDialogShown = false


    @Inject
    lateinit var mainRepository: MainRepository

    @Inject
    lateinit var firebaseClient: FirebaseClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChatBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        receiverId = arguments?.getString("receiverId")
        receiverName = arguments?.getString("receiverName")

        Log.d("ChatFragment", "receiverId: $receiverId") // 📌 **Gelen receiverId kontrolü**

        initChatUI()

        toolbar = binding.chatToolbar
        setupToolbar()

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_voice_call -> {
                    Log.d("ChatFragment", "🔊 Sesli Arama Başlatılıyor...")
                    startCall(false)
                    true
                }
                R.id.action_video_call -> {
                    Log.d("ChatFragment", "📹 Görüntülü Arama Başlatılıyor...")
                    startCall(true)
                    true
                }
                else -> false
            }
        }

        return binding.root
    }

    override fun onStart() {
        super.onStart()
        listenForIncomingCalls()
    }

    private fun listenForIncomingCalls() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("calls").document(userId)
            .addSnapshotListener { snapshot, e ->
                if (e != null || snapshot == null || !snapshot.exists()) return@addSnapshotListener

                val call = snapshot.toObject(DataModel::class.java) ?: return@addSnapshotListener

                // Kullanıcı daha önce reddettiyse gösterme
                if (!call.isValid() || call.target != userId || callDialogShown) return@addSnapshotListener

                Log.d("ChatFragment", "📞 Gelen Çağrı Algılandı: $call")

                // Ana ekran gerçekten hazırsa göster
                if (activity != null && isResumed) {
                    callDialogShown = true
                    showIncomingCallDialog(call)
                }
            }
    }





    private fun showIncomingCallDialog(call: DataModel) {
        AlertDialog.Builder(requireContext())
            .setTitle("Incoming Call")
            .setMessage("You have an incoming ${call.type} from ${call.sender}")
            .setPositiveButton("Accept") { _, _ ->
                firebaseClient.acceptCall(call.target!!)
                startCallActivity(call)
            }
            .setNegativeButton("Reject") { _, _ ->
                firebaseClient.rejectCall(call.target!!)
                rejectCall() // ← burada ID varsa
            }
            .setCancelable(false)
            .show()
    }



    private fun startCallActivity(call: DataModel) {
        val intent = Intent(context, CallActivity::class.java).apply {
            putExtra("target", call.sender)
            putExtra("isVideoCall", call.type == DataModelType.StartVideoCall) // ✅ Hata düzeltildi
            putExtra("isCaller", false) // Karşı taraf çağrıyı kabul ettiği için false
        }
        startActivity(intent)
    }

    private fun initChatUI() {
        adapter = ChatAdapter(chatViewModel.messages.value ?: emptyList(), auth.currentUser?.uid ?: "")
        binding.recyclerView.adapter = adapter
        binding.recyclerView.layoutManager = LinearLayoutManager(context)

        binding.sendButton.setOnClickListener {
            val messageText = binding.messageEditText.text.toString()
            if (messageText.isNotBlank() && receiverId != null) {
                chatViewModel.sendMessage(receiverId!!, messageText)
                binding.messageEditText.text?.clear()
                binding.recyclerView.scrollToPosition(adapter.itemCount - 1)
            }
        }

        binding.loadingIndicator.visibility = View.VISIBLE

        chatViewModel.messages.observe(viewLifecycleOwner) { messages ->
            adapter.updateMessages(messages)
            binding.recyclerView.scrollToPosition(messages.size - 1)
            binding.loadingIndicator.visibility = View.GONE
        }

        receiverId?.let { chatViewModel.listenForMessages(it) }
        activity?.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
    }

    private fun setupToolbar() {
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back)
        toolbar.navigationIcon?.setTint(resources.getColor(R.color.white))
        toolbar.title = receiverName ?: "User"
        toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun startCall(isVideoCall: Boolean) {
        getCameraAndMicPermission {
            receiverId?.let { receiverId ->
                val senderId = auth.currentUser?.uid ?: ""
                val senderName = auth.currentUser?.email?.substringBefore("@") ?: ""

                // 📌 Log: Çağrının kime yapıldığını kontrol et
                Log.d("ChatFragment", "📞 Çağrı başlatılıyor. Receiver ID: $receiverId | Sender ID: $senderId")

                mainRepository.sendConnectionRequest(receiverId, isVideoCall) { success ->
                    if (success) {
                        Log.d("ChatFragment", "✅ Çağrı isteği başarıyla gönderildi.")

                        startActivity(Intent(context, CallActivity::class.java).apply {
                            putExtra("target", receiverId)
                            putExtra("isVideoCall", isVideoCall)
                            putExtra("isCaller", true)
                            putExtra("username", senderName)
                        })
                    } else {
                        Log.e("ChatFragment", "🚨 Çağrı isteği başarısız oldu.")
                        Toast.makeText(context, "Çağrı başlatılamadı", Toast.LENGTH_SHORT).show()
                    }
                }
            } ?: run {
                Log.e("ChatFragment", "❌ Receiver ID bulunamadı. Çağrı başlatılamadı.")
            }
        }
    }

    private fun rejectCall() {
        val db = FirebaseFirestore.getInstance()
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        db.collection("calls").document(userId).delete().addOnSuccessListener {
            Log.d("ChatFragment", "❌ Çağrı Firestore'dan silindi (Reddedildi)")
        }.addOnFailureListener {
            Log.e("ChatFragment", "🚨 Çağrı silinemedi: ${it.message}")
        }
    }





    private fun getCameraAndMicPermission(onPermissionGranted: () -> Unit) {
        val permissions = arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        val permissionsToRequest = permissions.filter {
            ContextCompat.checkSelfPermission(requireContext(), it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsToRequest.isEmpty()) {
            Log.d("ChatFragment", "✅ Kamera ve Mikrofon izinleri zaten verilmiş.")
            onPermissionGranted()
        } else {
            Log.d("ChatFragment", "🚨 Kamera ve Mikrofon izinleri isteniyor: $permissionsToRequest")
            requestPermissions(permissionsToRequest.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Log.d("ChatFragment", "✅ Kullanıcı izinleri onayladı.")
                onPermissionGranted?.invoke()
            } else {
                Log.e("ChatFragment", "🚨 Kullanıcı izinleri reddetti.")
                Toast.makeText(requireContext(), "Çağrı için izinler gerekli.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onResume() {
        super.onResume()
        callDialogShown = false
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 10
        private var onPermissionGranted: (() -> Unit)? = null
    }
}
