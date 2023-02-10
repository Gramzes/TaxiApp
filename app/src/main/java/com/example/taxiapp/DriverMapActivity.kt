package com.example.taxiapp

import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taxiapp.databinding.ActivityDriverMapBinding
import com.example.taxiapp.model.User
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.*
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.*
import com.yandex.mapkit.directions.driving.DrivingSession.DrivingRouteListener
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.layers.ObjectEvent
import com.yandex.mapkit.location.*
import com.yandex.mapkit.map.*
import com.yandex.mapkit.mapview.MapView
import com.yandex.mapkit.user_location.UserLocationLayer
import com.yandex.mapkit.user_location.UserLocationObjectListener
import com.yandex.mapkit.user_location.UserLocationView
import com.yandex.runtime.Error
import com.yandex.runtime.image.ImageProvider


class DriverMapActivity : AppCompatActivity(), UserLocationObjectListener, LocationListener,
    DrivingRouteListener{
    private var mapview: MapView? = null
    private var userLocationLayer: UserLocationLayer? = null
    private val database = Firebase.database("https://taxiapp-758ff-default-rtdb.firebaseio.com")
    val driversRef = database.getReference("drivers")
    val usersRef = database.getReference("users")
    lateinit var mapKit: MapKit
    lateinit var locationManager: LocationManager
    lateinit var drivingRouter: DrivingRouter
    var drivingSession: DrivingSession? = null

    private lateinit var mapObjects: MapObjectCollection
    private var mapObjectsRoute: MapObjectCollection? = null
    var driversLocationRef = FirebaseDatabase.getInstance().getReference("drivers_location")

    lateinit var binding: ActivityDriverMapBinding
    var clientId: String? = null

    var currentMode = DEFAULT_MODE

    companion object {
        const val DEFAULT_MODE = 0
        const val SEARCH_MODE = 1
        const val USER_FOUND_MODE = 2
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(
                android.Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted.
            }
             else -> {
                 Toast.makeText(this,
                     "Чтобы пассажир мог найти вас на карте, необходимо разрешение",
                     Toast.LENGTH_LONG
                 ).show()
        }
        }
    }

    private val locationListener = object : LocationListener {
        override fun onLocationUpdated(p0: Location) {
            val uid = Firebase.auth.uid.toString()
            val latitude = p0.position.latitude
            val longitude = p0.position.longitude
            driversRef.child(Firebase.auth.uid.toString()).child("latitude").setValue(latitude)
            driversRef.child(Firebase.auth.uid.toString()).child("longitude").setValue(longitude)
            mapview!!.map.move(
                CameraPosition(Point(latitude, longitude), 11.0f, 0.0f, 0.0f),
                Animation(Animation.Type.SMOOTH, 2F),
                null
            )
        }

        override fun onLocationStatusUpdated(p0: LocationStatus) {
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        MapKitFactory.setApiKey("e11f6550-1396-46c9-8c52-771d3b6b412f");
        MapKitFactory.initialize(this);

        binding = ActivityDriverMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val permissionStatus =
            ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }

        mapview = findViewById<View>(R.id.mapview) as MapView
        mapview!!.map.move(
            CameraPosition(Point(55.751574, 37.573856), 11.0f, 0.0f, 0.0f),
            Animation(Animation.Type.SMOOTH, 0F),
            null
        )

        mapKit = MapKitFactory.getInstance()
        mapKit.resetLocationManagerToDefault()
        locationManager = mapKit.createLocationManager()
        userLocationLayer = mapKit.createUserLocationLayer(mapview!!.mapWindow)
        userLocationLayer!!.isVisible = true
        userLocationLayer!!.isHeadingEnabled = true

        userLocationLayer!!.setObjectListener(this)

        mapObjects = mapview!!.map.mapObjects.addCollection()
        mapObjectsRoute = mapview!!.map.mapObjects.addCollection()
        drivingRouter = DirectionsFactory.getInstance().createDrivingRouter()

        binding.work.setOnClickListener {
            toogleWorkBtn()
        }
    }

    override fun onStop() {
        mapview!!.onStop()
        MapKitFactory.getInstance().onStop()
        locationManager.unsubscribe(this);
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        MapKitFactory.getInstance().onStart()
        mapview!!.onStart()
        subscribeToLocationUpdate()
        locationManager.requestSingleUpdate(locationListener)
    }

    fun toogleWorkBtn(){
        with(binding){
            when (currentMode) {
                DEFAULT_MODE -> {
                    work.text = "Customer search"
                    currentMode = SEARCH_MODE
                    mapObjectsRoute?.clear()
                    var geoFire = GeoFire(driversLocationRef)
                    geoFire.removeLocation(Firebase.auth.uid);
                }
                else -> {
                    work.text = "Start working"
                    currentMode = DEFAULT_MODE
                }
            }
        }
    }

    private fun subscribeToLocationUpdate() {
        locationManager.subscribeForLocationUpdates(0.0, 10000, 0.0, false, FilteringMode.OFF, this)
    }

    override fun onObjectAdded(userLocationView: UserLocationView) {
        /*userLocationLayer!!.setAnchor(
            PointF((mapview!!.width * 0.5).toFloat(), (mapview!!.height * 0.5).toFloat()),
            PointF((mapview!!.width * 0.5).toFloat(), (mapview!!.height * 0.83).toFloat())
        )*/
        val arrow = ImageProvider.fromResource(this, R.drawable.user_arrow)
        val style = IconStyle()
        style.scale = 0.1f
        userLocationView.arrow.setIcon(arrow, style)

        val pinIcon: CompositeIcon = userLocationView.pin.useCompositeIcon()

        val icon = ImageProvider.fromResource(this, R.drawable.im_here)
        pinIcon.setIcon(
            "icon",
            icon,
            IconStyle().setAnchor(PointF(0.5f, 0.5f))
                .setRotationType(RotationType.NO_ROTATION)
                .setZIndex(0f)
                .setScale(0.6f)
        )


        //userLocationView.accuracyCircle.fillColor = Color.BLUE and -0x66000001
    }

    override fun onObjectRemoved(p0: UserLocationView) {
    }

    override fun onObjectUpdated(p0: UserLocationView, p1: ObjectEvent) {
    }

    override fun onLocationUpdated(p0: Location) {
        val uid = Firebase.auth.uid.toString()
        val latitude = p0.position.latitude
        val longitude = p0.position.longitude
        driversRef.child(uid).child("latitude").setValue(latitude)
        driversRef.child(uid).child("longitude").setValue(longitude)
        if (currentMode == USER_FOUND_MODE) {
            usersRef.child(clientId!!).get().addOnSuccessListener {
                val user = it.getValue<User>()
                val point2 = Point(user!!.latitude!!, user!!.longitude!!)
                submitRequest(Point(latitude, longitude), point2)
                createUserObject(user)
            }
        }
        if (currentMode == SEARCH_MODE) {
            var geoFire = GeoFire(driversLocationRef)
            geoFire.setLocation(uid, GeoLocation(latitude, longitude))
            driversRef.child(Firebase.auth.uid.toString()).get().addOnSuccessListener {
                val driver = it.getValue<User>()
                if (driver!!.clientId != null){
                    clientId = driver!!.clientId
                    currentMode = USER_FOUND_MODE
                    geoFire.removeLocation(uid)
                }
                else{
                    geoFire.setLocation(uid, GeoLocation(latitude, longitude))
                }
            }

        }
    }

    override fun onLocationStatusUpdated(p0: LocationStatus) {
    }

    private fun createUserObject(user: User) {
        mapObjects.clear()
        val center = Point(user!!.latitude!!, user!!.longitude!!)

        val mark = mapObjects.addPlacemark(center)
        mark.opacity = 0.5f
        mark.setIcon(ImageProvider.fromResource(this, R.drawable.im_here))
        mark.isDraggable = false

    }

    override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
        mapObjectsRoute?.clear()
        if (currentMode == USER_FOUND_MODE) {
            for (route in routes) {
                mapObjectsRoute?.addPolyline(route.geometry)
                route.metadata.weight.timeWithTraffic
                route.metadata.weight.distance
            }
        }
    }

    override fun onDrivingRoutesError(p0: Error) {

    }

    private fun submitRequest(startLocation: Point, endLocation: Point) {
        drivingSession?.cancel()
        val drivingOptions = DrivingOptions()
        val vehicleOptions = VehicleOptions()
        val requestPoints = ArrayList<RequestPoint>()
        requestPoints.add(
            RequestPoint(
                startLocation,
                RequestPointType.WAYPOINT,
                null
            )
        )
        requestPoints.add(
            RequestPoint(
                endLocation,
                RequestPointType.WAYPOINT,
                null
            )
        )
        drivingSession =
            drivingRouter.requestRoutes(requestPoints, drivingOptions, vehicleOptions, this)
    }
}