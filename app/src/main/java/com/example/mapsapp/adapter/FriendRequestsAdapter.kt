package com.example.mapsapp.adapter

import android.graphics.BitmapFactory
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.User

class FriendRequestsAdapter(
    private val onAcceptClick: (User) -> Unit,
    private val onRejectClick: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.RequestViewHolder>() {

    private val userList = mutableListOf<User>()

    inner class RequestViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val name: TextView = view.findViewById(R.id.userName)
        private val image: ImageView = view.findViewById(R.id.userProfilePic)
        private val acceptButton: ImageButton = view.findViewById(R.id.addFriendButton)
        private val rejectButton: ImageButton = view.findViewById(R.id.cancelButton)

        fun bind(user: User) {
            name.text = user.name

            user.photoBase64?.let {
                try {
                    val cleanBase64 = it.substringAfter(",", it)
                    val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    image.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    image.setImageResource(R.drawable.ic_profile_placeholder)
                }
            } ?: run {
                image.setImageResource(R.drawable.ic_profile_placeholder)
            }

            acceptButton.setOnClickListener { onAcceptClick(user) }
            rejectButton.setOnClickListener { onRejectClick(user) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return RequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: RequestViewHolder, position: Int) {
        holder.bind(userList[position])
    }

    override fun getItemCount(): Int = userList.size

    fun submitList(users: List<User>) {
        userList.clear()
        userList.addAll(users)
        notifyDataSetChanged()
    }
}
