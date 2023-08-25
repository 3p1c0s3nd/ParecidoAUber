package com.example.tipouber.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer

import android.os.Handler
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ListenerRegistration
import com.example.tipouber.R
import com.example.tipouber.databinding.ActivityMapBinding
import com.example.tipouber.databinding.ActivityMapTripBinding
import com.example.tipouber.fragments.ModalBottomSheetTripInfo
import com.example.tipouber.models.Booking
import com.example.tipouber.providers.AuthProvider
import com.example.tipouber.providers.BookingProvider
import com.example.tipouber.providers.GeoProvider
import com.example.tipouber.utils.CarMoveAnim
import org.imperiumlabs.geofirestore.extension.getLocation

class MapTripActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {


    private var mensajeLog = LibreriaFunciones()
    private var listenerDriverLocation: ListenerRegistration? = null
    private var driverLocation: LatLng? = null
    private var endLatLng: LatLng? = null
    private var startLatLng: LatLng? = null

    private var listenerBooking: ListenerRegistration? = null
    private var markerDestination: Marker? = null
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var booking: Booking? = null
    private var markerOrigin: Marker? = null
    private var bookingListener: ListenerRegistration? = null
    private lateinit var binding: ActivityMapTripBinding
    private var googleMap: GoogleMap? = null
    var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    private var markerDriver: Marker? = null
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()


    private var wayPoints: ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil: DirectionUtil


    private var isDriverLocationFound = false
    private var isBookingLoaded = false

    private var modalTrip = ModalBottomSheetTripInfo()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityMapTripBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            val locationRequest = LocationRequest.create().apply {
                interval = 0
                fastestInterval = 0
                priority = Priority.PRIORITY_HIGH_ACCURACY
                smallestDisplacement = 1f
            }

            easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

            binding.imageViewInfo.setOnClickListener { showModalInfo() }


            locationPermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onCreate (MapTripActivity) ${e.message}")
        }

    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido")


                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido con limitacion")

                }
                else -> {
                    Log.d("LOCALIZACION", "Permiso no concedido")
                }
            }
        }

    }

    private fun showModalInfo() {
        try {
            if (booking != null) {
                val bundle = Bundle()
                bundle.putString("booking", booking?.toJson())
                modalTrip.arguments = bundle
                modalTrip.show(supportFragmentManager, ModalBottomSheetTripInfo.TAG)
            }
            else {
                Toast.makeText(this, "No se pudo cargar la informacion", Toast.LENGTH_SHORT).show()
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion showModalInfo (MapTripActivity) ${e.message}")
        }
    }


    private fun getLocationDriver() {
        try {
            if (booking != null) {
                listenerDriverLocation = geoProvider.getLocationWorking(booking?.idDriver!!).addSnapshotListener { document, e ->
                    if (e != null) {
                        Log.d("FIRESTORE", "ERROR: ${e.message}")
                        return@addSnapshotListener
                    }

                    if (driverLocation != null) {
                        endLatLng = driverLocation
                    }

                    if (document?.exists()!!) {
                        val l = document.get("l") as List<*>
                        val lat = l[0] as Double
                        val lng = l[1] as Double

                        driverLocation = LatLng(lat, lng)

                        if (!isDriverLocationFound && driverLocation != null) {
                            isDriverLocationFound = true
                            addDriverMarker(driverLocation!!)
                            easyDrawRoute(driverLocation!!, originLatLng!!)
                        }

                        if (endLatLng != null) {
                            CarMoveAnim.carAnim(markerDriver!!, endLatLng!!, driverLocation!!)
                        }

                        Log.d("FIRESTORE", "LOCATION: $l")
                    }

                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getLocationDriver (MapTripActivity) ${e.message}")
        }

    }


    @SuppressLint("SetTextI18n")
    private fun getBooking() {
        try {
            listenerBooking = bookingProvider.getBooking().addSnapshotListener { document, e ->

                if (e != null) {
                    Log.d("FIRESTORE", "ERROR: ${e.message}")
                    return@addSnapshotListener
                }

                booking = document?.toObject(Booking::class.java)

                if (!isBookingLoaded) {
                    isBookingLoaded = true
                    originLatLng = LatLng(booking?.originLat!!, booking?.originLng!!)
                    destinationLatLng = LatLng(booking?.destinationLat!!, booking?.destinationLng!!)
                    googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                        CameraPosition.builder().target(originLatLng!!).zoom(17f).build()
                    ))
                    getLocationDriver()
                    addOriginMarker(originLatLng!!)
                }

                if (booking?.status == "accept") {
                    binding.textViewStatus.text = "Aceptado"
                }
                else if (booking?.status == "started") {
                    startTrip()
                }
                else if (booking?.status == "finished") {
                    finishTrip()
                }

            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getBooking (MapTripActivity) ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun finishTrip() {
       try {
           listenerDriverLocation?.remove()
           binding.textViewStatus.text = "Finalizado"
           val i = Intent(this, CalificationActivity::class.java)
           i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
           startActivity(i)
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion finishTrip (MapTripActivity) ${e.message}")
       }
    }

    @SuppressLint("SetTextI18n")
    private fun startTrip() {
        try {
            binding.textViewStatus.text = "Iniciado"
            googleMap?.clear()
            if (driverLocation != null) {
                addDriverMarker(driverLocation!!)
                addDestinationMarker()
                easyDrawRoute(driverLocation!!, destinationLatLng!!)
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion startTrip (MapTripActivity) ${e.message}")
        }
    }

    private fun easyDrawRoute(originLatLng: LatLng, destinationLatLng: LatLng) {
        try {
            wayPoints.clear()
            wayPoints.add(originLatLng)
            wayPoints.add(destinationLatLng)
            directionUtil = DirectionUtil.Builder()
                .setDirectionKey(resources.getString(R.string.google_maps_key))
                .setOrigin(originLatLng)
                .setWayPoints(wayPoints)
                .setGoogleMap(googleMap!!)
                .setPolyLinePrimaryColor(R.color.black)
                .setPolyLineWidth(12)
                .setPathAnimation(true)
                .setCallback(this)
                .setDestination(destinationLatLng)
                .build()

            directionUtil.initPath()
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion easyDrawRoute (MapTripActivity) ${e.message}")
        }
    }

    private fun addOriginMarker(position: LatLng) {
        try {
            markerOrigin = googleMap?.addMarker(MarkerOptions().position(position).title("Recoger aqui")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person)))
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion addOriginMarker (MapTripActivity) ${e.message}")
        }
    }

    private fun addDriverMarker(position: LatLng) {
        try {
            markerDriver = googleMap?.addMarker(MarkerOptions().position(position).title("Tu conductor")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.uber_car)))
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion addDriverMarker (MapTripActivity) ${e.message}")
        }

    }

    private fun addDestinationMarker() {
       try {
           if (destinationLatLng != null) {
               markerDestination = googleMap?.addMarker(MarkerOptions().position(destinationLatLng!!).title("Recoger aqui")
                   .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin)))
           }
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion addDestinationMarker (MapTripActivity) ${e.message}")
       }
    }


    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            70,
            150,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0,0,70,150)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        listenerBooking?.remove()
        listenerDriverLocation?.remove()
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            googleMap?.uiSettings?.isZoomControlsEnabled = true

            getBooking()

//        easyWayLocation?.startLocation();

            if (ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {

                return
            }
            googleMap?.isMyLocationEnabled = false

            try {
                val success = googleMap?.setMapStyle(
                    MapStyleOptions.loadRawResourceStyle(this, R.raw.style)
                )
                if (!success!!) {
                    Log.d("MAPAS", "No se pudo encontrar el estilo")
                }

            } catch (e: Resources.NotFoundException) {
                Log.d("MAPAS", "Error: ${e.toString()}")
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onMapReady")
        }

    }


    override fun locationOn() {

    }

    override fun currentLocation(location: Location) { // ACTUALIZACION DE LA POSICION EN TIEMPO REAL

    }

    override fun locationCancelled() {

    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        directionUtil.drawPath(WAY_POINT_TAG)
    }


}