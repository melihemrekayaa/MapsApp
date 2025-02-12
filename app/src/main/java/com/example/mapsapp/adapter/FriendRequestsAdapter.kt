package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ItemFriendRequestBinding
import com.example.mapsapp.model.User

class FriendRequestsAdapter(
    private var requests: List<User>,
    private val onAcceptClick: (User) -> Unit
) : RecyclerView.Adapter<FriendRequestsAdapter.FriendRequestViewHolder>() {

    fun updateRequests(newRequests: List<User>) {
        requests = newRequests
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FriendRequestViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_friend_request, parent, false)
        return FriendRequestViewHolder(view)
    }

    override fun onBindViewHolder(holder: FriendRequestViewHolder, position: Int) {
        val request = requests[position]
        holder.bind(request)
    }

    override fun getItemCount(): Int = requests.size

    inner class FriendRequestViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val friendName: TextView = itemView.findViewById(R.id.friendName)
        private val acceptButton: Button = itemView.findViewById(R.id.acceptButton)

        fun bind(request: User) {
            friendName.text = request.name

            acceptButton.setOnClickListener {
                onAcceptClick(request)
            }
        }
    }
}