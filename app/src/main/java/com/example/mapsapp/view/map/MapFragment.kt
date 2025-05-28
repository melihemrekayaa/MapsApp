package com.example.mapsapp.view.map

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.util.Base64
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
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.FirebaseFirestore
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.Style
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
    private lateinit var locationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        permissionLauncher = registerForActivityResult(RequestPermission()) { granted ->
            if (granted) initializeMap()
            else Toast.makeText(requireContext(), "Location permission is required.", Toast.LENGTH_LONG).show()
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            initializeMap()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        binding.btnMyLocation.setOnClickListener { goToMyLocation() }
        binding.btnFriends.setOnClickListener {
            FriendsBottomSheet { friendLocation ->
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

    private fun decodeBase64ToBitmap(base64: String): Bitmap? {
        return try {
            val cleanBase64 = base64.substringAfter(",", base64)
            val bytes = Base64.decode(cleanBase64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        } catch (e: Exception) {
            null
        }
    }

    private fun createProfileBitmapWithName(
        bitmap: Bitmap,
        name: String,
        imageSize: Int = 96,
        labelHeight: Int = 80,
        textSize: Float = 36f
    ): Bitmap {
        val labelWidth = maxOf(imageSize, (name.length * 22))
        val totalHeight = imageSize + labelHeight
        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, imageSize, imageSize, true)
        val output = Bitmap.createBitmap(labelWidth, totalHeight, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Beyaz zemin, rounded rect
        val labelRect = RectF(0f, 0f, labelWidth.toFloat(), labelHeight.toFloat())
        paint.color = Color.WHITE
        canvas.drawRoundRect(labelRect, 24f, 24f, paint) // <- Oval köşeler

        // 2. Border çizimi
        val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK // veya Color.DKGRAY
            style = Paint.Style.STROKE
            strokeWidth = 4f
        }
        canvas.drawRoundRect(labelRect, 24f, 24f, borderPaint)

        // 3. Metin
        paint.color = Color.BLACK
        paint.textSize = textSize
        paint.textAlign = Paint.Align.CENTER
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val xText = labelWidth / 2f
        val yText = labelHeight / 2f - (paint.descent() + paint.ascent()) / 2
        canvas.drawText(name, xText, yText, paint)

        // 4. Yuvarlak profil fotoğrafı (altta)
        val imageLeft = (labelWidth - imageSize) / 2f
        val imageTop = labelHeight.toFloat()
        val rectF = RectF(imageLeft, imageTop, imageLeft + imageSize, imageTop + imageSize)

        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        canvas.drawOval(rectF, imagePaint)
        imagePaint.xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_IN)
        canvas.drawBitmap(scaledBitmap, null, rectF, imagePaint)

        return output
    }






    private fun observeFriendLocations() {
        viewModel.locations.observe(viewLifecycleOwner) { list ->
            markers.values.forEach { annotationManager.delete(it) }
            markers.clear()

            list.forEach { friend ->
                val point = Point.fromLngLat(friend.lng, friend.lat)

                val photo = friend.photoBase64?.let { decodeBase64ToBitmap(it) }
                    ?: ContextCompat.getDrawable(requireContext(), R.drawable.ic_profile_placeholder)!!.toBitmap(96, 96)

                val finalBitmap = createProfileBitmapWithName(photo, friend.name)

                val options = PointAnnotationOptions()
                    .withPoint(point)
                    .withIconImage(finalBitmap)

                val annotation = annotationManager.create(options)
                markers[friend.uid] = annotation
            }
        }
    }

    private fun goToMyLocation() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        client.lastLocation.addOnSuccessListener { loc ->
            loc?.let {
                val pt = Point.fromLngLat(it.longitude, it.latitude)
                binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(pt).zoom(15.0).build())

                val myIcon = ContextCompat.getDrawable(requireContext(), R.drawable.ic_my_location_marker)?.toBitmap(128, 128)
                if (myIcon != null) {
                    val options = PointAnnotationOptions()
                        .withPoint(pt)
                        .withIconImage(myIcon)
                    annotationManager.create(options)
                }
            }
        }
    }

    private fun saveMyLocationToFirestore() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) return

        val client = LocationServices.getFusedLocationProviderClient(requireContext())
        client.lastLocation.addOnSuccessListener { loc ->
            loc?.let {
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(FirebaseAuth.getInstance().currentUser!!.uid)
                    .update("location", GeoPoint(it.latitude, it.longitude))
            }
        }
    }



    private fun goToFriendLocation(loc: FriendLocation) {
        val pt = Point.fromLngLat(loc.lng, loc.lat)
        binding.mapView.getMapboxMap().setCamera(CameraOptions.Builder().center(pt).zoom(15.0).build())
    }

    override fun onDestroyView() {
        if (this::locationClient.isInitialized) {
            locationClient.removeLocationUpdates(locationCallback)
        }
        _binding = null
        super.onDestroyView()
    }


}
