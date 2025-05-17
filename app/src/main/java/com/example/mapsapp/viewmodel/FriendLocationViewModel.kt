// app/src/main/java/com/example/mapsapp/viewmodel/FriendLocationViewModel.kt
package com.example.mapsapp.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.mapsapp.model.FriendLocation
import com.example.mapsapp.repository.AuthRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
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

            // 1) Kullanıcının friends alanını çek
            val userDoc = firestore.collection("users")
                .document(currentUser)
                .get()
                .await()

            val friendIds = userDoc.get("friends") as? List<String> ?: emptyList()
            val collected = mutableListOf<FriendLocation>()

            // 2) Her bir arkadaş için GeoPoint + email oku
            friendIds.forEach { friendId ->
                val friendSnap = firestore.collection("users")
                    .document(friendId)
                    .get()
                    .await()

                val geo = friendSnap.getGeoPoint("location")
                val email = friendSnap.getString("email") ?: ""
                if (geo != null) {
                    collected += FriendLocation(
                        uid = friendId,
                        email = email,
                        lat = geo.latitude,
                        lng = geo.longitude
                    )
                }
            }

            // 3) Sonuçları yayınla
            _locations.postValue(collected)
        }
    }
}
