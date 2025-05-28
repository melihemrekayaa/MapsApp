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
import com.example.mapsapp.model.MessageWithUserProfile
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val currentUserId: String
) : ListAdapter<MessageWithUserProfile, RecyclerView.ViewHolder>(DiffCallback()) {

    companion object {
        private const val VIEW_TYPE_USER = 0
        private const val VIEW_TYPE_OTHER = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).message.senderId == currentUserId) VIEW_TYPE_USER else VIEW_TYPE_OTHER
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_USER) {
            val view = inflater.inflate(R.layout.item_user_message, parent, false)
            UserMessageViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_other_message, parent, false)
            OtherMessageViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UserMessageViewHolder -> holder.bind(item)
            is OtherMessageViewHolder -> holder.bind(item)
        }
    }

    private fun decodeBase64ToBitmap(base64Str: String): android.graphics.Bitmap? {
        return try {
            val decodedBytes = Base64.decode(base64Str, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    inner class UserMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.textMessage)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val imageProfile: ImageView = itemView.findViewById(R.id.imageUserProfile)

        fun bind(item: MessageWithUserProfile) {
            textMessage.text = item.message.message
            textTime.text = formatTime(item.message.timestamp)
            decodeBase64ToBitmap(item.userPhotoBase64)?.let {
                imageProfile.setImageBitmap(it)
            }
        }
    }

    inner class OtherMessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val textMessage: TextView = itemView.findViewById(R.id.textMessage)
        private val textTime: TextView = itemView.findViewById(R.id.textTime)
        private val imageProfile: ImageView = itemView.findViewById(R.id.imageUserProfile)

        fun bind(item: MessageWithUserProfile) {
            textMessage.text = item.message.message
            textTime.text = formatTime(item.message.timestamp)
            decodeBase64ToBitmap(item.userPhotoBase64)?.let {
                imageProfile.setImageBitmap(it)
            }
        }
    }

    class DiffCallback : DiffUtil.ItemCallback<MessageWithUserProfile>() {
        override fun areItemsTheSame(oldItem: MessageWithUserProfile, newItem: MessageWithUserProfile): Boolean {
            return oldItem.message.id == newItem.message.id
        }

        override fun areContentsTheSame(oldItem: MessageWithUserProfile, newItem: MessageWithUserProfile): Boolean {
            return oldItem == newItem
        }
    }
}
