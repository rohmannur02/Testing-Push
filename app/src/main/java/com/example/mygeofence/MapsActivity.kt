package com.example.mygeofence

import android.annotation.SuppressLint
import android.annotation.TargetApi
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.mygeofence.databinding.ActivityMapsBinding
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CircleOptions

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Toast.makeText(this, "Notifications permission granted", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Notifications permission rejected", Toast.LENGTH_SHORT).show()
            }

        }
    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(this, MyReceiver::class.java)
        intent.action = MyReceiver.ACTION_GEOFENCE_EVENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_MUTABLE)
        } else {
            PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (Build.VERSION.SDK_INT >= 33) {
            requestNotificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    private val centerLat = 37.4274745
    private val centerLng = -122.169719
    private val geofenceRadius = 400.0

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        getMyLocation()
        addGeofence()

        mMap.uiSettings.isZoomControlsEnabled = true

        val standford = LatLng(centerLat, centerLng)
        mMap.addMarker(MarkerOptions().position(standford).title("Standford University"))
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(standford, 15f))

        mMap.addCircle(
            CircleOptions()
                .center(standford)
                .radius(geofenceRadius)
                .fillColor(0x22FF0000)
                .strokeWidth(3f)
        )
    }

    @SuppressLint("MissingPermission")
    private fun addGeofence() {
        val geofencingClient = LocationServices.getGeofencingClient(this)

        val geofence = Geofence.Builder()
            .setRequestId("kampus")
            .setCircularRegion(
                centerLat,
                centerLng,
                geofenceRadius.toFloat()
            )
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_DWELL or Geofence.GEOFENCE_TRANSITION_ENTER)
            .setLoiteringDelay(5000)
            .build()
        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()
        geofencingClient.removeGeofences(geofencePendingIntent).run {
            addOnCompleteListener {
                geofencingClient.addGeofences(geofencingRequest, geofencePendingIntent).run {
                    addOnSuccessListener {
                        showToast("Geofencing added")
                    }
                    addOnFailureListener {
                        showToast("Geofencing not added : ${it.message}")
                    }
                }
            }
        }
    }

    private fun showToast(text: String) {
        Toast.makeText(this@MapsActivity, text, Toast.LENGTH_SHORT).show()
    }

    private val runningQOrLater = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

    @TargetApi(Build.VERSION_CODES.Q)
    private val requestLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                if (runningQOrLater) {
                    requestBackgroundLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                } else {
                    getMyLocation()
                }
            }
        }
    private val requestBackgroundLocationPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    private fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            permission
        ) == PackageManager.PERMISSION_GRANTED
    }

    @TargetApi(Build.VERSION_CODES.Q)
    private fun checkForegroundAndBackgroundLocationPermission(): Boolean {
        val foregroundLocationApproved =
            checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val backgroundPermissionApproved =
            if (runningQOrLater) {
                checkPermission(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            } else {
                true
            }
        return foregroundLocationApproved && backgroundPermissionApproved
    }

    @SuppressLint("MissingPermission")
    private fun getMyLocation() {
        if (checkForegroundAndBackgroundLocationPermission()) {
            mMap.isMyLocationEnabled = true
        } else {
            requestLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }
}