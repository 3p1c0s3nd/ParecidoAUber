package com.example.uberclonekotlinconductor.activities


import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.hardware.*
import android.location.Location
import android.os.*
import androidx.appcompat.app.AppCompatActivity

import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.uberclonekotlinconductor.ClasesyFunciones.LibreriaFunciones
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.firebase.firestore.ListenerRegistration
import com.example.uberclonekotlinconductor.R
import com.example.uberclonekotlinconductor.databinding.ActivityMapBinding
import com.example.uberclonekotlinconductor.fragments.ModalBottomSheetBooking
import com.example.uberclonekotlinconductor.fragments.ModalBottomSheetMenu
import com.example.uberclonekotlinconductor.models.Booking
import com.example.uberclonekotlinconductor.models.FCMBody
import com.example.uberclonekotlinconductor.models.FCMResponse
import com.example.uberclonekotlinconductor.providers.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MapActivity : AppCompatActivity(), OnMapReadyCallback, Listener, SensorEventListener {

    private var bearing: Float = 0.0f
    private var bookingListener: ListenerRegistration? = null
    private lateinit var binding: ActivityMapBinding
    private var googleMap: GoogleMap? = null
    var easyWayLocation: EasyWayLocation? = null
    private var myLocationLatLng: LatLng? = null
    private var markerDriver: Marker? = null
    private val geoProvider = GeoProvider()
    private val authProvider = AuthProvider()
    private val bookingProvider = BookingProvider()
    private val driverProvider = DriverProvider()
    private val notificationProvider = NotificationProvider()
    private val modalBooking = ModalBottomSheetBooking()
    private val modalMenu = ModalBottomSheetMenu()

    // SENSOR CAMERA
    private var angle = 0
    private val rotationMatrix = FloatArray(16)
    private var sensorManager: SensorManager? = null
    private var vectSensor: Sensor? = null
    private var declination = 0.0f
    private var isFirstTimeOnResume = false
    private var isFirstLocation = false
    private var mensajeLog = LibreriaFunciones()

    val timer = object: CountDownTimer(30000, 1000) {
        override fun onTick(counter: Long) {
            Log.d("TIMER", "Counter: $counter")
        }

        override fun onFinish() {
            Log.d("TIMER", "ON FINISH")
            modalBooking.dismiss()
        }

    }



    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)

            binding = ActivityMapBinding.inflate(layoutInflater)
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

            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?
            vectSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

            locationPermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))

            listenerBooking()
            createToken()


            binding.btnConnect.setOnClickListener { connectDriver() }
            binding.btnDisconnect.setOnClickListener { disconnectDriver() }
            binding.imageViewMenu.setOnClickListener { showModalMenu() }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onCreate (MapActivity) ${e.message}")
        }
    }

    val locationPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permission ->

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            when {
                permission.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido")
//                    easyWayLocation?.startLocation();
                    checkIfDriverIsConnected()
                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido con limitacion")
//                    easyWayLocation?.startLocation();
                    checkIfDriverIsConnected()
                }
                else -> {
                    Log.d("LOCALIZACION", "Permiso no concedido")
                }
            }
        }

    }



    private fun createToken() {
        driverProvider.createToken(authProvider.getId())
    }

    private fun showModalMenu() {
        modalMenu.show(supportFragmentManager, ModalBottomSheetMenu.TAG)
    }

    private fun showModalBooking(booking: Booking) {

        try {
            val bundle = Bundle()
            bundle.putString("booking", booking.toJson())
            modalBooking.arguments = bundle
            modalBooking.isCancelable = false // NO PUEDA OCULTAR EL MODAL BOTTTOM SHEET
            modalBooking.show(supportFragmentManager, ModalBottomSheetBooking.TAG)
            timer.start()
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion showModalBooking (MapActivity) ${e.message}")
        }
    }

    private fun listenerBooking() {
        try {
            bookingListener = bookingProvider.getBooking().addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.d("FIRESTORE", "ERROR: ${e.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    if (snapshot.documents.size > 0) {
                        val booking = snapshot.documents[0].toObject(Booking::class.java)
                        if (booking?.status == "create") {
                            showModalBooking(booking!!)
                        }
                    }
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion listenerBooking (MapActivity) ${e.message}")
        }
    }



    private fun checkIfDriverIsConnected() {
        try {
            geoProvider.getLocation(authProvider.getId()).addOnSuccessListener { document ->
                if (document.exists()) {
                    if (document.contains("l")) {
                        connectDriver()
                    }
                    else {
                        showButtonConnect()
                    }
                }
                else {
                    showButtonConnect()
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion checkIfDriverIsConnected (MapActivity) ${e.message}")
        }
    }

    private fun saveLocation() {
       try {
           if (myLocationLatLng != null) {
               geoProvider.saveLocation(authProvider.getId(), myLocationLatLng!!)
           }
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion saveLocation (MapActivity) ${e.message}")
       }
    }

    private fun disconnectDriver() {
        try {
            easyWayLocation?.endUpdates()
            if (myLocationLatLng != null) {
                geoProvider.removeLocation(authProvider.getId())
                showButtonConnect()
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion disconnectDriver (MapActivity) ${e.message}")
        }
    }

    private fun connectDriver() {
        try {
            easyWayLocation?.endUpdates() // OTROS HILOS DE EJECUCION
            easyWayLocation?.startLocation()
            showButtonDisconnect()
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion connectDriver (MapActivity) ${e.message}")
        }
    }

    private fun showButtonConnect() {
        binding.btnDisconnect.visibility = View.GONE // OCULTANDO EL BOTON DE DESCONECTARSE
        binding.btnConnect.visibility = View.VISIBLE // MOSTRANDO EL BOTON DE CONECTARSE
    }

    private fun showButtonDisconnect() {
        binding.btnDisconnect.visibility = View.VISIBLE // MOSTRANDO EL BOTON DE DESCONECTARSE
        binding.btnConnect.visibility = View.GONE // OCULATNDO EL BOTON DE CONECTARSE
    }

    private fun addMarker() {
        try {
            val drawable = ContextCompat.getDrawable(applicationContext, R.drawable.uber_car)
            val markerIcon = getMarkerFromDrawable(drawable!!)
            if (markerDriver != null) {
                markerDriver?.remove() // NO REDIBUJAR EL ICONO
            }
            if (myLocationLatLng != null) {
                markerDriver = googleMap?.addMarker(
                    MarkerOptions()
                        .position(myLocationLatLng!!)
                        .anchor(0.5f, 0.5f)
                        .flat(true)
                        .icon(markerIcon)
                )
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion addMarker (MapActivity) ${e.message}")
        }
    }



    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            googleMap?.uiSettings?.isZoomControlsEnabled = true

//        easyWayLocation?.startLocation();
            startSensor()
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
            mensajeLog.LOG("Error en la funcion onMapReady (MapActivity) ${e.message}")
        }

    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) { // ACTUALIZACION DE LA POSICION EN TIEMPO REAL
        try {
            myLocationLatLng = LatLng(location.latitude, location.longitude) // LAT Y LONG DE LA POSICION ACTUAL

            val field = GeomagneticField(
                location.latitude.toFloat(),
                location.longitude.toFloat(),
                location.altitude.toFloat(),
                System.currentTimeMillis()
            )

            declination = field.declination

//        if (!isFirstLocation) {
//            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
//                CameraPosition.builder().target(myLocationLatLng!!).zoom(19f).build()
//            ))
//            isFirstLocation = true
//
//        }
//        val orientation = FloatArray(3)
//        val bearing = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
//        updateCamera(bearing)

            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(myLocationLatLng!!).bearing(bearing).tilt(50f).zoom(19f).build()
            ))
            addDirectionMarker(myLocationLatLng!!, angle)
            saveLocation()
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion currentLocation (MapActivity) ${e.message}")
        }
    }

    override fun locationCancelled() {

    }

    private fun updateCamera(bearing: Float) {
       try {
           val oldPos = googleMap?.cameraPosition
           val pos = CameraPosition.builder(oldPos!!).bearing(bearing).tilt(50f).build()
           googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(pos))
           if (myLocationLatLng != null) {
               addDirectionMarker(myLocationLatLng!!, angle)
           }
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion updateCamera (MapActivity) ${e.message}")
       }
    }

    private fun addDirectionMarker(latLng: LatLng, angle: Int)  {
        try {
            val circleDrawable = ContextCompat.getDrawable(applicationContext, R.drawable.ic_up_arrow_circle)
            val markerIcon = getMarkerFromDrawable(circleDrawable!!)
            if (markerDriver != null) {
                markerDriver?.remove()
            }
            markerDriver = googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .anchor(0.5f, 0.5f)
                    .rotation(angle.toFloat())
                    .flat(true)
                    .icon(markerIcon)
            )
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion addDirectionMarker (MapActivity) ${e.message}")
        }
    }

    private fun getMarkerFromDrawable(drawable: Drawable): BitmapDescriptor {
        val canvas = Canvas()
        val bitmap = Bitmap.createBitmap(
            120,
            120,
            Bitmap.Config.ARGB_8888
        )
        canvas.setBitmap(bitmap)
        drawable.setBounds(0,0,120,120)
        drawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }



    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        easyWayLocation?.endUpdates()
        bookingListener?.remove()
        stopSensor()
    }

    override fun onSensorChanged(event: SensorEvent) {
       try {
           if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
               SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
               val orientation = FloatArray(3)
               SensorManager.getOrientation(rotationMatrix, orientation)
               if (Math.abs(Math.toDegrees(orientation[0].toDouble()) - angle) > 0.8 ) {
                   bearing = Math.toDegrees(orientation[0].toDouble()).toFloat() + declination
                   updateCamera(bearing)
               }
               angle = Math.toDegrees(orientation[0].toDouble()).toInt()
           }
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion onSensorChanged (MapActivity) ${e.message}")
       }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    private fun startSensor() {
       try {
           if (sensorManager != null) {
               sensorManager?.registerListener(this, vectSensor, SensorManager.SENSOR_STATUS_ACCURACY_LOW)
           }
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion startSensor (MapActivity) ${e.message}")
       }
    }

    private fun stopSensor () {
        sensorManager?.unregisterListener(this)
    }

    override fun onResume() {
        super.onResume() // ABRIMOS LA PANTALLA ACTUAL
        if (!isFirstTimeOnResume) {
            isFirstTimeOnResume = true
        }
        else {
            startSensor()
        }
    }

    override fun onPause() {
        super.onPause()
        stopSensor()
    }

}