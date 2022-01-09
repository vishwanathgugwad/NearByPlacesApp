package com.example.mymaps.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.example.mymaps.R

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.example.mymaps.databinding.ActivityMapsBinding
import com.example.mymaps.models.Places
import com.example.mymaps.remote.IGoogleApiService
import com.example.mymaps.remote.RetrofitClient
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.*
import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.lang.StringBuilder

class MapsActivity : AppCompatActivity() , OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding


    //location
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    var currentLat: Double = 0.0
    var currentLong: Double = 0.0

    private var marker: Marker? = null
    lateinit var mLastLocation: Location

    private lateinit var mService: IGoogleApiService
    internal lateinit var currentPlace: Places


    companion object {
        private const val REQUEST_CODE = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mService = RetrofitClient.getClient()!!
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.google_map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val placeTypeList =
            arrayListOf("movie_theater" , "atm" , "restaurant" , "market" , "bank")



        binding.spType.adapter =
            ArrayAdapter(this , android.R.layout.simple_spinner_dropdown_item , placeTypeList)




        binding.btFind.setOnClickListener {
            val i = binding.spType.selectedItem.toString()
            nearByPlaces(i)
            //nearByPlaces(i)
            //build url request based on location
//            URL = getUrl(currentLat , currentLong , i)
//            mServiceFetch(URL)
        }

        //request runtime permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkLocationPermission()) {
                buildLocationRequest()
                buildCallBack()
                fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
                fusedLocationProviderClient.requestLocationUpdates(
                    locationRequest ,
                    locationCallback ,
                    Looper.myLooper()
                )
            } else
                buildLocationRequest()
            buildCallBack()
            fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
            fusedLocationProviderClient.requestLocationUpdates(
                locationRequest ,
                locationCallback ,
                Looper.myLooper()
            )

        }

    }

    private fun buildCallBack() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult?) {
                mLastLocation = p0!!.locations[p0.locations.size - 1]//get last location
                marker?.remove()
                currentLat = mLastLocation.latitude
                currentLong = mLastLocation.longitude

                val latlng = LatLng(currentLat , currentLong)
                val markerOptions = MarkerOptions().position(latlng).title(" your position")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                marker = mMap.addMarker(markerOptions)

                mMap.moveCamera(CameraUpdateFactory.newLatLng(latlng))
                mMap.animateCamera(CameraUpdateFactory.zoomTo(11F))
            }

        }
    }

    private fun buildLocationRequest() {
        locationRequest = LocationRequest()
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        locationRequest.interval = 5000
        locationRequest.fastestInterval = 3000
        locationRequest.smallestDisplacement = 10F
    }

    private fun checkLocationPermission(): Boolean {

        if (ActivityCompat.checkSelfPermission(
                this ,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this ,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                    this ,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
                ActivityCompat.requestPermissions(
                    this , arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) ,
                    REQUEST_CODE
                )
            else
            //when permission denied
            //request permission
                ActivityCompat.requestPermissions(
                    this , arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) ,
                    REQUEST_CODE
                )
            return false
        } else
            return true

    }


    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(
        requestCode: Int ,
        permissions: Array<out String> ,
        grantResults: IntArray ,
    ) {
        super.onRequestPermissionsResult(requestCode , permissions , grantResults)

        when (requestCode) {
            REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ActivityCompat.checkSelfPermission(
                            this ,
                            Manifest.permission.ACCESS_FINE_LOCATION
                        ) == PackageManager.PERMISSION_GRANTED
                    )
                        if (checkLocationPermission())
                            buildLocationRequest()
                    buildCallBack()
                    fusedLocationProviderClient =
                        LocationServices.getFusedLocationProviderClient(this)
                    fusedLocationProviderClient.requestLocationUpdates(
                        locationRequest ,
                        locationCallback ,
                        Looper.myLooper()
                    )

                    mMap.isIndoorEnabled = true
                } else
                    Toast.makeText(this , "permission denied" , Toast.LENGTH_SHORT).show()
            }
        }
    }


    private fun nearByPlaces(typePlaces: String) {
        //clear all markers
        mMap.clear()
        //build url request based on location
        val url = getUrl(currentLat , currentLong , typePlaces)

        mService.getNearByPlaces(url).enqueue(object : Callback<Places> {
            override fun onResponse(call: Call<Places> , response: Response<Places>) {

                currentPlace = response.body()!!

                if (response.isSuccessful) {

                    for (element in response.body()!!.results.indices) {
                        val markerOptions = MarkerOptions()
                        val googlePlace = response.body()!!.results[element]
                        val lat = googlePlace.geometry.location.lat
                        val lng = googlePlace.geometry.location.lng
                        val placeName = googlePlace.name
                        val latLng = LatLng(lat , lng)

                        markerOptions.position(latLng)
                        markerOptions.title(placeName)
                        when (typePlaces) {
                            "hospital" -> markerOptions.icon(bitmapDescriptorFromVector(this@MapsActivity ,
                                R.drawable.ic_hospital))
                            "market" -> markerOptions.icon(bitmapDescriptorFromVector(this@MapsActivity ,
                                R.drawable.ic_market))
                            "school" -> markerOptions.icon(bitmapDescriptorFromVector(this@MapsActivity ,
                                R.drawable.ic_school))
                            "restaurant" -> markerOptions.icon(bitmapDescriptorFromVector(this@MapsActivity ,
                                R.drawable.ic_restaurant))
                            else -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(
                                BitmapDescriptorFactory.HUE_BLUE))
                        }

                        markerOptions.snippet(element.toString()) // assign index for market

                        //add marker to map
                        mMap.addMarker(markerOptions)
                        //move camera
                        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(12F))


                    }

                }
            }

            override fun onFailure(call: Call<Places> , t: Throwable) {
                Toast.makeText(this@MapsActivity , "" + t.message , Toast.LENGTH_SHORT).show()
            }

        }

        )
    }

    //converter
    private fun bitmapDescriptorFromVector(
        context: Context ,
        vectorResId: Int ,
    ): BitmapDescriptor? {
        return ActivityCompat.getDrawable(context , vectorResId)?.run {
            setBounds(0 , 0 , intrinsicWidth , intrinsicHeight)
            val bitmap = Bitmap.createBitmap(intrinsicWidth ,
                intrinsicHeight ,
                Bitmap.Config.ARGB_8888)
            draw(Canvas(bitmap))
            BitmapDescriptorFactory.fromBitmap(bitmap)
        }
    }

    private fun getUrl(currentLat: Double , currentLong: Double , typePlaces: String): String {
        val googlePlaceUrl =
            StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json")
        googlePlaceUrl.append("?keyword=cruise&location=$currentLat,$currentLong")
        googlePlaceUrl.append("&radius=10000")
        googlePlaceUrl.append("&types=$typePlaces")
        googlePlaceUrl.append("&key=ADD_YOUR_KEY_HERE")

        Log.d("URL_DEBUG" , googlePlaceUrl.toString())
        return googlePlaceUrl.toString()


    }

    override fun onStop() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
        super.onStop()
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        //init google play services
        if (ActivityCompat.checkSelfPermission(
                this ,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else
            mMap.isMyLocationEnabled = true

        //enable zoom control
        mMap.uiSettings.isZoomControlsEnabled = true
    }


}
