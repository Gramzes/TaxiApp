package com.example.taxiapp

import android.content.pm.PackageManager
import android.graphics.PointF
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.taxiapp.databinding.ActivityUserMapBinding
import com.example.taxiapp.model.User
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.firebase.geofire.GeoQuery
import com.firebase.geofire.GeoQueryEventListener
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.yandex.mapkit.*
import com.yandex.mapkit.directions.DirectionsFactory
import com.yandex.mapkit.directions.driving.*
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


class UserMapActivity : AppCompatActivity(), UserLocationObjectListener, LocationListener,
    DrivingSession.DrivingRouteListener {

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
    var usersLocationRef = FirebaseDatabase.getInstance().getReference("users_location")


    lateinit var binding: ActivityUserMapBinding
    var currentLocation: Location? = null
    var driverKey: String? = null
    var geoQuery: GeoQuery? = null

    var currentMode = DEFAULT_MODE
    val defaultSearchRadius = 5.0
    var extendedSearchRadius = 5.0

    companion object {
        const val DEFAULT_MODE = 0
        const val SEARCH_MODE = 1
        const val DRIVER_FOUND_MODE = 2
    }

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(
                android.Manifest.permission.ACCESS_FINE_LOCATION, false
            ) -> {
                // Precise location access granted.
            }
            else -> {
                Toast.makeText(
                    this,
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
            currentLocation = p0

            driversRef.child(uid).child("latitude").setValue(latitude)
            driversRef.child(uid).child("longitude").setValue(longitude)

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

        binding = ActivityUserMapBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val permissionStatus =
            ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )

        if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
            locationPermissionRequest.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
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

        binding.findTaxi.setOnClickListener {
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

    fun toogleWorkBtn() {
        with(binding) {
            when (currentMode) {
                DEFAULT_MODE -> {
                    if (currentLocation != null) {
                        findTaxi.text = "Driver search"
                        currentMode = SEARCH_MODE
                        mapObjectsRoute?.clear()
                        findNearestTaxi(defaultSearchRadius)
                    }
                }
                SEARCH_MODE -> {
                    findTaxi.text = "Find a taxi"
                    currentMode = DEFAULT_MODE
                }
                else -> {
                    findTaxi.text = "Find a taxi"
                    currentMode = DEFAULT_MODE
                }
            }
        }
    }

    private fun findNearestTaxi(searchRadius: Double) {
        val geoFire = GeoFire(driversLocationRef)
        currentLocation?.let {
            geoQuery = geoFire.queryAtLocation(
                GeoLocation(it.position.latitude, it.position.longitude), searchRadius)
            geoQuery!!.removeAllListeners()
            geoQuery!!.addGeoQueryEventListener(geoQueryListener)
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
        currentLocation = p0
        val uid = Firebase.auth.uid.toString()
        val latitude = p0.position.latitude
        val longitude = p0.position.longitude
        usersRef.child(uid).child("latitude").setValue(
            latitude)
        usersRef.child(uid).child("longitude").setValue(longitude)

        if (currentMode == DRIVER_FOUND_MODE) {
            driversRef.child(driverKey!!).get().addOnSuccessListener {
                val driver = it.getValue<User>()
                mapObjects.clear()
                createDriverObject(driver!!)
                val point2 = Point(driver!!.latitude!!, driver!!.longitude!!)
                createRoute(Point(latitude, longitude), point2)
            }
        }
    }

    override fun onLocationStatusUpdated(p0: LocationStatus) {
    }

    private fun createDriverObject(driver: User) {
        val center = Point(driver!!.latitude!!, driver!!.longitude!!)
        val mark = mapObjects.addPlacemark(center)
        mark.opacity = 0.5f
        mark.setIcon(ImageProvider.fromResource(this, R.drawable.im_here))
        mark.isDraggable = false
    }


    override fun onDrivingRoutes(routes: MutableList<DrivingRoute>) {
        mapObjectsRoute?.clear()
        if (currentMode == DRIVER_FOUND_MODE) {
            for (route in routes) {
                mapObjectsRoute?.addPolyline(route.geometry)
                route.metadata.weight.timeWithTraffic
                route.metadata.weight.distance
            }
        }
    }

    override fun onDrivingRoutesError(p0: Error) {

    }

    private fun createRoute(startLocation: Point, endLocation: Point) {
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

    var geoQueryListener = object: GeoQueryEventListener{
        override fun onKeyEntered(key: String?, location: GeoLocation?) {
            if(currentMode != DRIVER_FOUND_MODE) {
                extendedSearchRadius = defaultSearchRadius
                transferOrderToDriver(key!!)
                currentMode = DRIVER_FOUND_MODE
                setDriverFoundUI()
                driverKey = key
            }
        }

        override fun onKeyExited(key: String?) {
            TODO("Not yet implemented")
        }

        override fun onKeyMoved(key: String?, location: GeoLocation?) {
            TODO("Not yet implemented")
        }

        override fun onGeoQueryReady() {
            extendedSearchRadius*=2
            findNearestTaxi(extendedSearchRadius)
        }

        override fun onGeoQueryError(error: DatabaseError?) {
            TODO("Not yet implemented")
        }

    }


    fun setDriverFoundUI(){

    }

    fun transferOrderToDriver(driverId: String){
        driversRef.child(driverId).child("client").setValue(Firebase.auth.uid)
    }
}