package com.example.mapsapp.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.User

class FriendsAdapter(
    private val onFriendClick: (User) -> Unit,
    private val onRemoveClick: (User) -> Unit
) : ListAdapter<Triple<User, Boolean, Boolean>, FriendsAdapter.FriendViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend, parent, false)
        return FriendViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FriendViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val friendStatus: TextView = itemView.findViewById(R.id.friendStatus)
        private val friendStatusIcon: ImageView = itemView.findViewById(R.id.friendStatusIcon)
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)

        fun bind(item: Triple<User, Boolean, Boolean>) {
            val user = item.first
            val isOnline = item.second
            val isInCall = item.third

            friendName.text = user.name

            val lastSeenText = user.lastSeenTimestamp?.let {
                val diff = System.currentTimeMillis() - it
                val minutes = (diff / 1000 / 60).toInt()
                if (minutes < 60) "Last seen ${minutes} min ago"
                else "Last seen ${minutes / 60} h ago"
            } ?: "Unknown"

            // Durum metni
            friendStatus.text = when {
                isInCall -> "In Call"
                isOnline -> "" // Online ise yazı göstermiyoruz
                else -> lastSeenText
            }

            // Durum ikonu
            val statusDrawable = when {
                isInCall -> R.drawable.circle_red
                isOnline -> R.drawable.circle_green
                else -> R.drawable.circle_gray
            }
            friendStatusIcon.setImageResource(statusDrawable)

            // Base64 profil fotoğrafı gösterimi
            user.photoBase64?.let { base64 ->
                try {
                    val cleanBase64 = base64.substringAfter(",", base64)
                    val imageBytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    profileImage.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } ?: run {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }

            itemView.setOnClickListener { onFriendClick(user) }
        }




    }

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<Triple<User, Boolean, Boolean>>() {
            override fun areItemsTheSame(
                oldItem: Triple<User, Boolean, Boolean>,
                newItem: Triple<User, Boolean, Boolean>
            ): Boolean = oldItem.first.uid == newItem.first.uid

            override fun areContentsTheSame(
                oldItem: Triple<User, Boolean, Boolean>,
                newItem: Triple<User, Boolean, Boolean>
            ): Boolean = oldItem == newItem
        }
    }
}

