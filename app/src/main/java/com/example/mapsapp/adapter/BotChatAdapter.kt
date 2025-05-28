package com.example.mapsapp.adapter

import android.view.*
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.model.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

class BotChatAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val messages = mutableListOf<ChatMessage>()

    fun submitList(newList: List<ChatMessage>) {
        messages.clear()
        messages.addAll(newList)
        notifyDataSetChanged()
    }


    override fun getItemViewType(position: Int): Int {
        return if (messages[position].isUser) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == 0) {
            val view = inflater.inflate(R.layout.item_user_message, parent, false)
            UserViewHolder(view)
        } else {
            val view = inflater.inflate(R.layout.item_ai_message, parent, false)
            AiViewHolder(view)
        }
    }

    override fun getItemCount(): Int = messages.size

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(message.timestamp))

        if (holder is UserViewHolder) {
            holder.textMessage.text = message.text
            holder.textTime.text = time
        } else if (holder is AiViewHolder) {
            holder.textMessage.text = message.text
            holder.textTime.text = time
        }
    }


    private fun typeWriter(textView: TextView, fullText: String, index: Int = 0) {
        if (index <= fullText.length) {
            textView.text = fullText.substring(0, index)
            textView.postDelayed({
                typeWriter(textView, fullText, index + 1)
            }, 20L)
        }
    }

    class UserViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val textTime: TextView = view.findViewById(R.id.textTime)
    }

    class AiViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textMessage: TextView = view.findViewById(R.id.textMessage)
        val textTime: TextView = view.findViewById(R.id.textTime)
    }
}
