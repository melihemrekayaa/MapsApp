package com.example.mapsapp.webrtc.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

open class MyEventListener : ValueEventListener {
    private val TAG = "MyEventListener"

    override fun onDataChange(snapshot: DataSnapshot) {
        Log.d(TAG, "Data changed: ${snapshot.value}")
        // Veri değişikliklerini işlemek için buraya kod ekleyebilirsiniz
    }

    override fun onCancelled(error: DatabaseError) {
        Log.e(TAG, "Database error: ${error.message}")
        // Hataları işlemek için buraya kod ekleyebilirsiniz
    }
}
