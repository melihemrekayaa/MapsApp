package com.example.mapsapp.adapter

import android.animation.AnimatorInflater
import android.animation.AnimatorSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.widget.ImageView
import com.example.mapsapp.R
import com.example.mapsapp.util.CardItem


class CardAdapter(private val items: List<CardItem>,
                  private val onNavigate: (CardItem) -> Unit
) : RecyclerView.Adapter<CardAdapter.CardViewHolder>() {

    inner class CardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val frontCard: View = itemView.findViewById(R.id.frontCard)
        val backCard: View = itemView.findViewById(R.id.backCard)
        val title: TextView = itemView.findViewById(R.id.cardTitle)
        val logoImage: ImageView = itemView.findViewById(R.id.logoImage)
        val backgroundLogo: ImageView = itemView.findViewById(R.id.backgroundLogo)
        val backCardText: TextView = itemView.findViewById(R.id.backCardText)
        val navigateButton : Button = itemView.findViewById(R.id.navigateButton)
        var isBackVisible = false
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.card_item, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val item = items[position]
        holder.title.text = item.title
        holder.backgroundLogo.setImageResource(item.logoResId) // Arka plandaki bulanık logo
        holder.logoImage.setImageResource(item.logoResId) // Kartın ön yüzündeki net logo
        holder.backCardText.text = item.backText

        holder.frontCard.setOnClickListener {
            flipCard(holder)
        }

        holder.backCard.setOnClickListener {
            flipCard(holder)
        }

        holder.navigateButton.setOnClickListener {
            onNavigate(item)
        }
    }

    override fun getItemCount(): Int = items.size

    private fun flipCard(holder: CardViewHolder) {
        val flipOut = AnimatorInflater.loadAnimator(holder.itemView.context, R.animator.card_flip_out) as AnimatorSet
        val flipIn = AnimatorInflater.loadAnimator(holder.itemView.context, R.animator.card_flip_in) as AnimatorSet

        if (holder.isBackVisible) {
            flipOut.setTarget(holder.backCard)
            flipIn.setTarget(holder.frontCard)
            flipOut.start()
            flipIn.start()
            holder.backCard.visibility = View.GONE
            holder.frontCard.visibility = View.VISIBLE
            holder.isBackVisible = false
        } else {
            flipOut.setTarget(holder.frontCard)
            flipIn.setTarget(holder.backCard)
            flipOut.start()
            flipIn.start()
            holder.frontCard.visibility = View.GONE
            holder.backCard.visibility = View.VISIBLE
            holder.isBackVisible = true
        }
    }
}




