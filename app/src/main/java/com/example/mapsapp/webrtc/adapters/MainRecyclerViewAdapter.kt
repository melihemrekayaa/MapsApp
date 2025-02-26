package com.example.mapsapp.webrtc.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.databinding.ItemMainRecyclerViewBinding
import com.example.mapsapp.webrtc.utils.UserStatus

class MainRecyclerViewAdapter(private val listener: Listener) :
    RecyclerView.Adapter<MainRecyclerViewAdapter.MainRecyclerViewHolder>() {

    private var usersList: MutableList<Pair<String, String>> = mutableListOf()

    fun updateList(newList: List<Pair<String, String>>) {
        usersList.clear()
        usersList.addAll(newList)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewHolder {
        val binding = ItemMainRecyclerViewBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MainRecyclerViewHolder(binding)
    }

    override fun getItemCount(): Int = usersList.size

    override fun onBindViewHolder(holder: MainRecyclerViewHolder, position: Int) {
        val user = usersList[position]
        holder.bind(user, {
            listener.onVideoCallClicked(it)
        }, {
            listener.onAudioCallClicked(it)
        })
    }

    fun updateUserStatus(username: String, status: String) {
        val index = usersList.indexOfFirst { it.first == username }
        if (index != -1) {
            usersList[index] = username to status
            notifyItemChanged(index)
        }
    }

    interface Listener {
        fun onVideoCallClicked(username: String)
        fun onAudioCallClicked(username: String)
    }

    class MainRecyclerViewHolder(private val binding: ItemMainRecyclerViewBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private val context = binding.root.context

        fun bind(
            user: Pair<String, String>,
            videoCallClicked: (String) -> Unit,
            audioCallClicked: (String) -> Unit
        ) {
            binding.apply {
                usernameTv.text = user.first
                when (user.second) {
                    UserStatus.ONLINE.name -> {
                        videoCallBtn.isVisible = true
                        audioCallBtn.isVisible = true
                        videoCallBtn.setOnClickListener { videoCallClicked.invoke(user.first) }
                        audioCallBtn.setOnClickListener { audioCallClicked.invoke(user.first) }
                        statusTv.setTextColor(context.getColor(R.color.light_green))
                        statusTv.text = "Online"
                    }
                    UserStatus.OFFLINE.name -> {
                        videoCallBtn.isVisible = false
                        audioCallBtn.isVisible = false
                        statusTv.setTextColor(context.getColor(R.color.red))
                        statusTv.text = "Offline"
                    }
                    UserStatus.IN_CALL.name -> {
                        videoCallBtn.isVisible = false
                        audioCallBtn.isVisible = false
                        statusTv.setTextColor(context.getColor(R.color.yellow))
                        statusTv.text = "In Call"
                    }
                }
            }
        }
    }
}
