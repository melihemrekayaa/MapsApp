package com.example.mapsapp

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.mapsapp.databinding.ActivityMapsBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.mapsapp.util.getRandomColorMarker
import dagger.hilt.android.AndroidEntryPoint

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var locationListener: LocationListener
    private lateinit var locationManager: LocationManager
    private lateinit var permissionLauncher: ActivityResultLauncher<String>
    private lateinit var copyButton: Button
    private var selectedLatLng: LatLng? = null
    private val markers = mutableMapOf<String, Marker>()
    private var isZoomedIn = false
    private val initialZoomLevel = 10f
    private lateinit var initialCameraPosition: LatLng
    private lateinit var auth: FirebaseAuth
    private var selectedMarker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()

        binding.copyButton.setOnClickListener {
            copyCoordinates()
        }

        binding.cancelButton.setOnClickListener {
            removeLastMarker()
        }

        binding.zoomOutButton.setOnClickListener {
            zoomOut()
        }

        binding.zoomInButton.setOnClickListener {
            zoomIn()
        }

        binding.zoomOutMapButton.setOnClickListener {
            zoomOutMap()
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        registerLauncher()
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationListener = LocationListener { location ->
            val userLocation = LatLng(location.latitude, location.longitude)
            mMap.addMarker(MarkerOptions().position(userLocation).title("Your Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLocation, 15f))
            fetchUserLocations()
        }

        // Haritaya tıklanıldığında marker eklemek
        mMap.setOnMapClickListener { latLng ->
            selectedMarker?.remove() // Önceki seçilen markerı kaldır
            val marker = mMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
            selectedLatLng = latLng
            selectedMarker = marker
        }

        mMap.setOnMarkerClickListener { marker ->
            val userUid = marker.snippet
            val userEmail = marker.title
            if (userEmail == auth.currentUser?.email) {
                Toast.makeText(this, "This is your location!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "User Email: $userEmail", Toast.LENGTH_SHORT).show()
                if (userUid != null) {
                    openChatFragment(userUid,userEmail!!)
                }
            }
            true
        }

        fetchUserLocations()

        // Kullanıcıdan konum izni isteme
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            ) {
                Snackbar.make(
                    binding.root,
                    "Permission needed for location",
                    Snackbar.LENGTH_INDEFINITE
                ).setAction("Give permission") {
                    permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }.show()
            } else {
                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            }
        } else {
            showUserLocation()
        }
    }

    private fun zoomOut() {
        if (isZoomedIn) {
            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(initialCameraPosition, initialZoomLevel))
            isZoomedIn = false
            binding.zoomOutButton.visibility = View.GONE
        }
    }

    private fun zoomIn() {
        mMap.animateCamera(CameraUpdateFactory.zoomIn())
    }

    private fun zoomOutMap() {
        mMap.animateCamera(CameraUpdateFactory.zoomOut())
    }

    private fun registerLauncher() {
        permissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
                if (result) {
                    if (ContextCompat.checkSelfPermission(
                            this,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    ) {
                        locationManager.requestLocationUpdates(
                            LocationManager.GPS_PROVIDER,
                            0,
                            0f,
                            locationListener
                        )
                        showUserLocation()
                    }
                } else {
                    Toast.makeText(this@MapsActivity, "Permission needed!", Toast.LENGTH_LONG)
                        .show()
                }
            }
    }

    private fun copyCoordinates() {
        selectedLatLng?.let {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip =
                ClipData.newPlainText("Selected Location", "${it.latitude}, ${it.longitude}")
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Coordinates copied!", Toast.LENGTH_SHORT).show()
        } ?: run {
            Toast.makeText(this, "No coordinates to copy!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchUserLocations() {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("users").get().addOnSuccessListener { documents ->
            for (document in documents) {
                val geoPoint = document.getGeoPoint("location")
                val email = document.getString("email")
                val uid = document.getString("uid")

                if (geoPoint != null) {
                    val latLng = LatLng(geoPoint.latitude, geoPoint.longitude)
                    val markerOptions = MarkerOptions().position(latLng)
                        .title(email)
                        .snippet(uid)
                        .icon(BitmapDescriptorFactory.defaultMarker(getRandomColorMarker()))
                    mMap.addMarker(markerOptions)
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, it.localizedMessage, Toast.LENGTH_LONG).show()
        }
    }

    private fun showUserLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            }
        }
    }

    private fun animateCameraToLocation(location: LatLng){
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 15f), 2000, null)
    }

    private fun zoomToCurrentLocation() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
            val lastKnownLocation =
                locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            lastKnownLocation?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f), 2000, null)
            }
        }
    }

    private fun openChatFragment(userUid: String,userEmail: String) {
        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("receiverId", userUid)
        intent.putExtra("receiverName", userEmail.split("@")[0])
        startActivity(intent)
        finish()
    }

    private fun removeLastMarker() {
        selectedMarker?.remove()
        selectedMarker = null
        selectedLatLng = null
    }
}
