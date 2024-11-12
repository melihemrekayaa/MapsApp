package com.example.mapsapp.util

import com.example.mapsapp.R

object DataProvider {
    fun getCardItems(): List<CardItem> {
        return listOf(
            CardItem("Chat", R.drawable.chat, "You can chat with your friends here"),
            CardItem("Chat Bot",R.drawable.chatbot, "Need help? Let's get into ChatBot"),
            CardItem("Maps",R.drawable.maps,"Get directions to your destination"),
            CardItem("Voice Call", R.drawable.baseline_voice_chat_24,"Call your friends via voice call"),
            CardItem("Video Call", R.drawable.ic_video_call, "Call your friends via video call")
        )
    }
}
