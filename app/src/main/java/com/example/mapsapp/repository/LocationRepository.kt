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
                    val uid = doc.getString("uid")
                    val name = doc.getString("name")
                    val photoBase64 = doc.getString("photoBase64")

                    if (location != null && uid != null) {
                        FriendLocation(
                            uid = uid,
                            name = name ?: "",
                            photoBase64 = photoBase64,
                            lat = location.latitude,
                            lng = location.longitude
                        )
                    } else null
                }
                onResult(list)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}
