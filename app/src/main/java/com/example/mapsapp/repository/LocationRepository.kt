package com.example.mapsapp.repository

import com.example.mapsapp.model.FriendLocation
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

class LocationRepository @Inject constructor() {

    private val firestore = FirebaseFirestore.getInstance()

    fun getFriendLocations(onResult: (List<FriendLocation>) -> Unit) {
        firestore.collection("users").get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull { doc ->
                    val location = doc.getGeoPoint("location")
                    val email = doc.getString("email")
                    val uid = doc.getString("uid")

                    if (location != null && uid != null) {
                        FriendLocation(uid, email ?: "", location.latitude, location.longitude)
                    } else null
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}
