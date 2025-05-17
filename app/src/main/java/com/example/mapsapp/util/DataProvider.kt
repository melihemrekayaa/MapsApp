package com.example.mapsapp.util

import com.example.mapsapp.R

object DataProvider {
    fun getCardItems(): List<CardItem> {
        return listOf(
            CardItem(
                title = "Chat",
                iconResId = R.drawable.chat,
                subtitle = "Send and receive messages instantly with friends"
            ),
            CardItem(
                title = "Chat Bot",
                iconResId = R.drawable.chatbot,
                subtitle = "Ask anything, get instant answers from your assistant"
            ),
            CardItem(
                title = "Maps",
                iconResId = R.drawable.maps,
                subtitle = "Explore locations and get real-time directions"
            ),
            CardItem(
                title = "Voice Call",
                iconResId = R.drawable.baseline_voice_chat_24,
                subtitle = "Make crystal-clear voice calls to your contacts"
            ),
            CardItem(
                title = "Video Call",
                iconResId = R.drawable.ic_video_call,
                subtitle = "Start high-quality video calls anytime, anywhere"
            )
        )
    }
}
