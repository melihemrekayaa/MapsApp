package com.example.mapsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mapsapp.model.FriendLocation
import com.example.mapsapp.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FriendLocationViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _locations = MutableLiveData<List<FriendLocation>>(emptyList())
    val locations: LiveData<List<FriendLocation>> = _locations

    fun fetchFriendLocations() {
        viewModelScope.launch {
            val currentUser = authRepository.getCurrentUser()?.uid ?: return@launch

            val userDoc = firestore.collection("users")
                .document(currentUser)
                .get()
                .await()

            val friendIds = userDoc.get("friends") as? List<String> ?: emptyList()
            val collected = mutableListOf<FriendLocation>()

            for (friendId in friendIds) {
                val friendSnap = firestore.collection("users")
                    .document(friendId)
                    .get()
                    .await()

                val geo = friendSnap.getGeoPoint("location")
                val name = friendSnap.getString("name") ?: ""
                val photoBase64 = friendSnap.getString("photoBase64")

                if (geo != null) {
                    collected += FriendLocation(
                        uid = friendId,
                        name = name,
                        photoBase64 = photoBase64,
                        lat = geo.latitude,
                        lng = geo.longitude
                    )
                }
            }

            _locations.postValue(collected)
        }
    }
}
