package com.example.mapsapp.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.FriendLocation
import com.example.mapsapp.model.User

class FriendSheetAdapter(
    private val onMapClick: (FriendLocation) -> Unit,
    private val onChatClick: (User) -> Unit
) : ListAdapter<Pair<User, Boolean>, FriendSheetAdapter.VH>(Diff) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_sheet, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    inner class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profileImage)
        private val nameTv: TextView = itemView.findViewById(R.id.friendName)
        private val statusTv: TextView = itemView.findViewById(R.id.friendStatus)
        private val statusIcon: ImageView = itemView.findViewById(R.id.friendStatusIcon)
        private val btnChat: Button = itemView.findViewById(R.id.btnChat)
        private val btnMap: Button = itemView.findViewById(R.id.btnMap)

        fun bind(pair: Pair<User, Boolean>) {
            val (user, isOnline) = pair

            nameTv.text = user.name
            statusTv.text = if (isOnline) "Online" else "Offline"
            statusIcon.setImageResource(if (isOnline) R.drawable.circle_green else R.drawable.circle_gray)

            val base64 = user.photoBase64
            if (!base64.isNullOrBlank()) {
                try {
                    val clean = base64.substringAfter(",")
                    val bytes = Base64.decode(clean, Base64.DEFAULT)
                    val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    profileImage.setImageBitmap(bmp)
                } catch (_: Exception) {
                    profileImage.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } else {
                profileImage.setImageResource(R.drawable.ic_profile_placeholder)
            }

            btnMap.setOnClickListener {
                val loc = FriendLocation(
                    uid = user.uid,
                    name = user.name,
                    photoBase64 = user.photoBase64,
                    lat = user.location?.latitude ?: 0.0,
                    lng = user.location?.longitude ?: 0.0
                )
                onMapClick(loc)
            }

            btnChat.setOnClickListener {
                onChatClick(user)
            }
        }
    }

    companion object {
        private val Diff = object : DiffUtil.ItemCallback<Pair<User, Boolean>>() {
            override fun areItemsTheSame(old: Pair<User, Boolean>, new: Pair<User, Boolean>) =
                old.first.uid == new.first.uid

            override fun areContentsTheSame(old: Pair<User, Boolean>, new: Pair<User, Boolean>) =
                old == new
        }
    }
}
