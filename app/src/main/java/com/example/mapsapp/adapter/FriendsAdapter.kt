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
) : ListAdapter<User, FriendsAdapter.FriendViewHolder>(DiffCallback) {

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

        fun bind(user: User) {
            friendName.text = user.name

            val lastSeenText = user.lastSeenTimestamp?.let {
                val diff = System.currentTimeMillis() - it
                val minutes = (diff / 1000 / 60).toInt()
                if (minutes < 60) "Last seen ${minutes} min ago"
                else "Last seen ${minutes / 60} h ago"
            } ?: "Unknown"

            friendStatus.text = when {
                user.isInCall -> "In Call"
                user.isOnline == true -> "" // Online ise yazı yok
                else -> lastSeenText
            }

            val statusDrawable = when {
                user.isInCall -> R.drawable.circle_red
                user.isOnline == true -> R.drawable.circle_green
                else -> R.drawable.circle_gray
            }
            friendStatusIcon.setImageResource(statusDrawable)

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

    fun getItemAt(position: Int): User = getItem(position)

    companion object {
        private val DiffCallback = object : DiffUtil.ItemCallback<User>() {
            override fun areItemsTheSame(oldItem: User, newItem: User): Boolean =
                oldItem.uid == newItem.uid

            override fun areContentsTheSame(oldItem: User, newItem: User): Boolean =
                oldItem == newItem
        }
    }
}
