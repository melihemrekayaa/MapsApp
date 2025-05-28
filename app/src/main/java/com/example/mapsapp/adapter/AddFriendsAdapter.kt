package com.example.mapsapp.adapter

import android.util.Base64
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ItemAddFriendBinding
import com.example.mapsapp.model.User

class AddFriendsAdapter(
    private var users: MutableList<User>,
    private val onAddFriendClick: (User) -> Unit,
    private val onCancelRequestClick: (User) -> Unit
) : RecyclerView.Adapter<AddFriendsAdapter.AddFriendViewHolder>() {

    inner class AddFriendViewHolder(private val binding: ItemAddFriendBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.userName.text = user.name

            // Base64 profil fotoğrafı çözme
            user.photoBase64?.let { base64 ->
                try {
                    val cleanBase64 = base64.substringAfter(",", base64)
                    val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    binding.userProfilePic.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    binding.userProfilePic.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } ?: run {
                binding.userProfilePic.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // Buton görünürlüğü
            binding.addFriendButton.visibility = if (user.isRequestSent) View.GONE else View.VISIBLE
            binding.cancelButton.visibility = if (user.isRequestSent) View.VISIBLE else View.GONE

            // Tıklama olayları
            binding.addFriendButton.setOnClickListener { onAddFriendClick(user) }
            binding.cancelButton.setOnClickListener { onCancelRequestClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddFriendViewHolder {
        val binding = ItemAddFriendBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return AddFriendViewHolder(binding)
    }

    override fun onBindViewHolder(holder: AddFriendViewHolder, position: Int) {
        holder.bind(users[position])
    }

    override fun getItemCount() = users.size

    fun updateUsers(newUsers: List<User>) {
        users = newUsers.toMutableList()
        notifyDataSetChanged()
    }

    fun updateFriendStatus(user: User, isRequestSent: Boolean) {
        val index = users.indexOfFirst { it.uid == user.uid }
        if (index != -1) {
            users[index] = users[index].copy(isRequestSent = isRequestSent)
            notifyItemChanged(index)
        }
    }
}
