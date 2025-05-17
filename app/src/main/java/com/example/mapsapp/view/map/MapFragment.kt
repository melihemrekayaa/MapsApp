// app/src/main/java/com/example/mapsapp/view/map/MapFragment.kt
package com.example.mapsapp.view.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.fragment.app.viewModels
import com.example.mapsapp.R
import com.example.mapsapp.databinding.FragmentMapBinding
import com.example.mapsapp.model.FriendLocation
import com.example.mapsapp.util.BaseFragment
import com.example.mapsapp.viewmodel.FriendLocationViewModel
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MapFragment : BaseFragment() {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: FriendLocationViewModel by viewModels()
    private lateinit var annotationManager: com.mapbox.maps.plugin.annotation.generated.PointAnnotationManager
    private val markers = mutableMapOf<String, com.mapbox.maps.plugin.annotation.generated.PointAnnotation>()

    private lateinit var permissionLauncher: ActivityResultLauncher<String>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Register permission callback
        permissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
            if (granted) {
                initializeMap()
            } else {
                Toast.makeText(
                    requireContext(),
                    "Location permission is required to show your position.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

        // Check or request permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            initializeMap()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Button listeners
        binding.btnMyLocation.setOnClickListener { goToMyLocation() }
        binding.btnFriends.setOnClickListener {
            FriendsBottomSheet { friendLocation: FriendLocation ->
                goToFriendLocation(friendLocation)
            }.show(childFragmentManager, "FriendsSheet")
        }
    }

    private fun initializeMap() {
        binding.mapView.getMapboxMap().loadStyleUri(Style.MAPBOX_STREETS) {
            annotationManager = binding.mapView.annotations.createPointAnnotationManager()
            observeFriendLocations()
            viewModel.fetchFriendLocations()
            saveMyLocationToFirestore()
        }
    }

    private fun saveMyLocationToFirestore() {
        // Ensure permission before accessing location
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        client.lastLocation.addOnSuccessListener { loc ->
            loc?.let {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .update("location", GeoPoint(it.latitude, it.longitude))
            }
        }.addOnFailureListener {
            // Optionally log failure
        }
    }


    private fun observeFriendLocations() {
        viewModel.locations.observe(viewLifecycleOwner) { list ->
            // Eski işaretçileri sil
            markers.values.forEach { annotationManager.delete(it) }
            markers.clear()

            list.forEach { friend ->
                val point = Point.fromLngLat(friend.lng, friend.lat)
                val options = PointAnnotationOptions()
                    .withPoint(point)
                    // İkon ekle (isteğe bağlı)
//                .withIconImage(yourBitmap)

                    // Arkadaş e-mail’ini metin olarak göster
                    .withTextField(friend.email)
                    .withTextSize(12.0)
                    .withTextColor(Color.BLACK)
                    .withTextAnchor(TextAnchor.TOP)

                val annotation = annotationManager.create(options)
                markers[friend.uid] = annotation
            }
        }
    }


    private fun goToMyLocation() {
        // Ensure permission
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        client.lastLocation.addOnSuccessListener { loc ->
            loc?.let {
                val pt = Point.fromLngLat(it.longitude, it.latitude)
                binding.mapView.getMapboxMap()
                    .setCamera(CameraOptions.Builder().center(pt).zoom(15.0).build())
            }
        }
    }

    private fun goToFriendLocation(loc: FriendLocation) {
        val pt = Point.fromLngLat(loc.lng, loc.lat)
        binding.mapView.getMapboxMap()
            .setCamera(CameraOptions.Builder().center(pt).zoom(15.0).build())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
