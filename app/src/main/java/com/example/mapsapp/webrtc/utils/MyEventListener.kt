package com.example.mapsapp.webrtc.utils

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener


class MyEventListener(
    private val onDataChanged: (DataSnapshot) -> Unit = {},
    private val onError: (DatabaseError) -> Unit = {}
) : ValueEventListener {

    override fun onDataChange(snapshot: DataSnapshot) {
        Log.d("MyEventListener", "Data changed: ${snapshot.value}")
        onDataChanged(snapshot)
    }

    override fun onCancelled(error: DatabaseError) {
        Log.e("MyEventListener", "Database error: ${error.message}")
        onError(error)
    }
}
