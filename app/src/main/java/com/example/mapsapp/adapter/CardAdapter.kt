package com.example.mapsapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mapsapp.R
import com.example.mapsapp.util.CardItem

class CardAdapter(
    private val items: List<CardItem>,
    private val onNavigate: (CardItem) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val cardTitle: TextView = itemView.findViewById(R.id.cardTitle)
        val cardSubtitle: TextView = itemView.findViewById(R.id.cardSubtitle)
        val cardImage: ImageView = itemView.findViewById(R.id.cardImage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.card_item_gradient, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = items[position]

        holder.cardTitle.text = item.title
        holder.cardSubtitle.text = item.subtitle

        // Animasyon
        val animation = AnimationUtils.loadAnimation(holder.itemView.context, R.anim.slide_in_left)
        holder.itemView.startAnimation(animation)

        // Arka plan ve ikon ataması
        when (position) {
            0 -> {
                holder.cardImage.setImageResource(R.drawable.chat)
                holder.itemView.setBackgroundResource(R.drawable.bg_gradient_chat)
            }
            1 -> {
                holder.cardImage.setImageResource(R.drawable.chatbot)
                holder.itemView.setBackgroundResource(R.drawable.bg_gradient_chatbot)
            }
            2 -> {
                holder.cardImage.setImageResource(R.drawable.maps)
                holder.itemView.setBackgroundResource(R.drawable.bg_gradient_maps)
            }
            3 -> {
                holder.cardImage.setImageResource(R.drawable.baseline_voice_chat_24)
                holder.itemView.setBackgroundResource(R.drawable.bg_gradient_voice)
            }
            4 -> {
                holder.cardImage.setImageResource(R.drawable.ic_video_call)
                holder.itemView.setBackgroundResource(R.drawable.bg_gradient_video)
            }
            else -> {
                holder.cardImage.setImageResource(R.drawable.ic_close_red)
                holder.itemView.setBackgroundResource(R.drawable.back_card_background)
            }
        }

        holder.itemView.setOnClickListener {
            onNavigate(item)
        }
    }

    override fun getItemCount(): Int = items.size
}
