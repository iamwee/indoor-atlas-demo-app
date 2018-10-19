package com.eggdigital.siamicon

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.indooratlas.android.sdk.*
import android.os.Looper
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.indooratlas.android.sdk.resources.IAFloorPlan
import com.indooratlas.android.sdk.resources.IAResourceManager
import com.indooratlas.android.sdk.resources.IATask
import com.indooratlas.android.wayfinding.IAWayfinder
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    companion object {
        private const val TAG = "MainActivity"
        private const val graphJson = "{\"nodes\":[{\"latitude\":13.764715957619712,\"longitude\":100.56771308183673,\"floor\":18},{\"latitude\":13.764434598158697,\"longitude\":100.56765407323839,\"floor\":18},{\"latitude\":13.764489306969299,\"longitude\":100.56739389896393,\"floor\":18},{\"latitude\":13.764736004487478,\"longitude\":100.5676306145506,\"floor\":18},{\"latitude\":13.76447548650195,\"longitude\":100.5675769703703,\"floor\":18},{\"latitude\":13.76450935385648,\"longitude\":100.56741067341137,\"floor\":18},{\"latitude\":13.764762056270065,\"longitude\":100.56746431759167,\"floor\":18},{\"latitude\":13.76475424073559,\"longitude\":100.56763866117765,\"floor\":18},{\"latitude\":13.764738609665862,\"longitude\":100.56771912744809,\"floor\":18},{\"latitude\":13.76480894947141,\"longitude\":100.56733288934994,\"floor\":18},{\"latitude\":13.76481937018156,\"longitude\":100.56715318134594,\"floor\":18},{\"latitude\":13.764556247108498,\"longitude\":100.56709417274762,\"floor\":18}],\"edges\":[{\"begin\":0,\"end\":1},{\"begin\":0,\"end\":8},{\"begin\":1,\"end\":2},{\"begin\":2,\"end\":11},{\"begin\":3,\"end\":4},{\"begin\":3,\"end\":6},{\"begin\":3,\"end\":7},{\"begin\":4,\"end\":5},{\"begin\":5,\"end\":6},{\"begin\":7,\"end\":8},{\"begin\":7,\"end\":9},{\"begin\":9,\"end\":10},{\"begin\":10,\"end\":11}]}"
    }

    private val locationManager by lazy {
        IALocationManager.create(this@MainActivity)
    }

    private val wayFinder by lazy {
        IAWayfinder.create(this@MainActivity, graphJson)
    }

    private val resourceManager by lazy {
        IAResourceManager.create(this@MainActivity)
    }

    private var alreadyLocationChanged = false

    private var googleMap: GoogleMap? = null

    private var marker: Marker? = null

    private var pendingAsyncResult: IATask<IAFloorPlan>? = null

    private var polyLine: Polyline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        this.googleMap = googleMap
    }

    override fun onStart() {
        super.onStart()
        checkPermission({
            fetchLocation()
        }, {
            Toast.makeText(this, "permission denied", Toast.LENGTH_SHORT).show()
        })
    }

//    override fun onStop() {
//        super.onStop()
//        //locationManager.removeLocationUpdates(locationListener)
//    }

    private fun fetchLocation() {
        locationManager.requestLocationUpdates(IALocationRequest.create().setSmallestDisplacement(0.1f), object : IALocationListener {
            override fun onLocationChanged(location: IALocation) {
                moveCamera(location)
                Log.d(TAG, "latitude : ${location.latitude}")
                Log.d(TAG, "longitude : ${location.longitude}")
            }

            override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

            }
        })

        locationManager.registerRegionListener(object : IARegion.Listener {

            override fun onExitRegion(region: IARegion) {
                //DO nothing
            }

            override fun onEnterRegion(region: IARegion) {
                fetchFloorPlan(region.id)
            }
        })
    }

    private fun moveCamera(location: IALocation) {
        if (alreadyLocationChanged) return
        val cameraPosition = CameraPosition.Builder()
                .target(LatLng(location.latitude, location.longitude))
                .zoom(18f)
                .build()
        googleMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition))
        alreadyLocationChanged = true

        performWayFinding(location)
        markCurrentLocation(location)
        informCurrentLocation(location)
    }

    private fun informCurrentLocation(location: IALocation) {
        val locationStr = """|${location.latitude}, ${location.longitude}
                |Floor : ${location.floorLevel}
                |Accuracy : ${location.accuracy}""".trimMargin()

        textView.text = locationStr
    }

    private fun markCurrentLocation(location: IALocation) {
        val markerOptions = MarkerOptions()
                .icon(BitmapDescriptorFactory.defaultMarker())
                .position(LatLng(location.latitude, location.longitude))
                .title("Current location")

        marker?.remove().run { marker = googleMap?.addMarker(markerOptions) }
    }

    private fun performWayFinding(location: IALocation) {
        wayFinder.setLocation(location.latitude, location.longitude, 18)
        wayFinder.setDestination(13.764463351701995, 100.5675242315689, 18)
        val route = wayFinder.route
        val point = route.map { LatLng(it.begin.latitude, it.begin.longitude) }
        val polyLineOptions = PolylineOptions()
                .addAll(point)
                .color(Color.RED)
                .width(12f)
        polyLine?.remove().run { polyLine = googleMap?.addPolyline(polyLineOptions) }

    }

    private fun checkPermission(granted: () -> Unit, denied: () -> Unit) {
        requestPermission {
            with(android.Manifest.permission.CHANGE_WIFI_STATE)
            with(android.Manifest.permission.ACCESS_WIFI_STATE)
            with(android.Manifest.permission.ACCESS_COARSE_LOCATION)
            with(android.Manifest.permission.ACCESS_FINE_LOCATION)

            allPermissionGranted {
                granted()
            }

            permissionDenied {
                denied()
            }
        }
    }

    private fun fetchFloorPlan(id: String) {
        pendingAsyncResult?.let { asyncResult: IATask<IAFloorPlan> ->
            if (!asyncResult.isCancelled) {
                asyncResult.cancel()
            }
        }

        pendingAsyncResult = resourceManager.fetchFloorPlanWithId(id)
        pendingAsyncResult?.let { asyncResult: IATask<IAFloorPlan> ->
            asyncResult.setCallback({ result ->
                if (result.isSuccess) {
                    handleFloorPlanChange(result.result)
                } else {
                    Log.d(TAG, "loading floor plan failed: ${result.error}")
                }
            }, Looper.getMainLooper())
        }
    }


    private fun handleFloorPlanChange(result: IAFloorPlan) {
        Glide.with(this@MainActivity)
                .asBitmap()
                .load(result.url)
                .into(object : SimpleTarget<Bitmap>() {
                    override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                        renderFloorPlan(resource, result)
                    }
                })
        Log.d(TAG, result.toString())
    }

    private fun renderFloorPlan(bitmap: Bitmap, result: IAFloorPlan) {
        val latLng = LatLng(result.center.latitude, result.center.longitude)
        googleMap?.let {
            val groundOverlayOptions = GroundOverlayOptions()
                    .image(BitmapDescriptorFactory.fromBitmap(bitmap))
                    .position(latLng, result.widthMeters, result.heightMeters)
                    .bearing(result.bearing)
            it.addGroundOverlay(groundOverlayOptions)
        }
    }
}
