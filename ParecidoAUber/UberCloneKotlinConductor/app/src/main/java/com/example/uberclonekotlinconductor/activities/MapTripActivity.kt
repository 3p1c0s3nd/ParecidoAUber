package com.example.uberclonekotlinconductor.activities

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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
import com.bumptech.glide.Glide
import com.example.easywaylocation.EasyWayLocation
import com.example.easywaylocation.Listener
import com.example.easywaylocation.draw_path.DirectionUtil
import com.example.easywaylocation.draw_path.PolyLineDataBean
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
import com.example.uberclonekotlinconductor.databinding.ActivityMapTripBinding
import com.example.uberclonekotlinconductor.fragments.ModalBottomSheetBooking
import com.example.uberclonekotlinconductor.fragments.ModalBottomSheetTripInfo
import com.example.uberclonekotlinconductor.models.*
import com.example.uberclonekotlinconductor.providers.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
/*import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response*/
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

class MapTripActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack, SensorEventListener {

    private var bearing: Float = 0.0f
    private var totalPrice = 0.0
    private val configProvider = ConfigProvider()
    private var markerDestination: Marker? = null
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null
    private var booking: Booking? = null
    private var client: Client? = null
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
    private val historyProvider = HistoryProvider()
    private val notificationProvider = NotificationProvider()
    private val clientProvider = ClientProvider()

    private var wayPoints: ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil: DirectionUtil

    private var isLocationEnabled = false
    private var isCloseToOrigin = false
    private var mensajeLog = LibreriaFunciones()



    // DISTANCIA
    private var meters = 0.0
    private var km = 0.0
    private var currentLocation = Location("")
    private var previusLocation = Location("")
    private var isStartedTrip = false

    // MODAL
    private var modalTrip = ModalBottomSheetTripInfo()

    // SENSOR CAMERA
    private var angle = 0
    private val rotationMatrix = FloatArray(16)
    private var sensorManager: SensorManager? = null
    private var vectSensor: Sensor? = null
    private var declination = 0.0f
    private var isFirstTimeOnResume = false
    private var isFirstLocation = false

    // TEMPORIZADOR
    private var counter = 0
    private var min = 0
    private var handler = Handler(Looper.myLooper()!!)
    @SuppressLint("SetTextI18n")
    private var runnable = Runnable {
        kotlin.run {
            counter++

            if (min == 0) {
                binding.textViewTimer.text = "$counter Seg"
            }
            else {
                binding.textViewTimer.text = "$min Min $counter Seg"
            }

            if (counter == 60) {
                min = min + (counter / 60)
                counter = 0
                binding.textViewTimer.text = "$min Min $counter Seg"
            }

            startTimer()
        }
    }

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

            sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager?
            vectSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

            easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

            locationPermissions.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))

            binding.btnStartTrip.setOnClickListener { updateToStarted() }
            binding.btnFinishTrip.setOnClickListener { updateToFinish() }
            binding.imageViewInfo.setOnClickListener { showModalInfo() }
//        binding.btnConnect.setOnClickListener { connectDriver() }
//        binding.btnDisconnect.setOnClickListener { disconnectDriver() }
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
                    easyWayLocation?.startLocation()

                }
                permission.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                    Log.d("LOCALIZACION", "Permiso concedido con limitacion")
                    easyWayLocation?.startLocation()
                }
                else -> {
                    Log.d("LOCALIZACION", "Permiso no concedido")
                }
            }
        }

    }

    private fun getClientInfo() {
        try {
            clientProvider.getClientById(booking?.idClient!!).addOnSuccessListener { document ->
                if (document.exists()) {
                    client = document.toObject(Client::class.java)
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getClientInfo (MapTripActivity) ${e.message}")
        }
    }

    private fun sendNotification(status: String) {

        val map = HashMap<String, String>()
        map.put("title", "ESTADO DEL VIAJE")
        map.put("body", status)

         val body = FCMBody(
             to = client?.token!!,
             priority = "high",
             ttl = "4500s",
             data = map
         )

         notificationProvider.sendNotification(body).enqueue(object: Callback<FCMResponse> {
             override fun onResponse(call: Call<FCMResponse>, response: Response<FCMResponse>) {
                 if (response.body() != null) {

                     if (response.body()!!.success == 1) {
                         Toast.makeText(this@MapTripActivity, "Se envio la notificacion", Toast.LENGTH_LONG).show()
                     }
                     else {
                         Toast.makeText(this@MapTripActivity, "No se pudo enviar la notificacion", Toast.LENGTH_LONG).show()
                     }

                 }
                 else {
                     Toast.makeText(this@MapTripActivity, "hubo un error enviando la notificacion", Toast.LENGTH_LONG).show()
                 }
             }

             override fun onFailure(call: Call<FCMResponse>, t: Throwable) {
                 Log.d("NOTIFICATION", "ERROR: ${t.message}")
             }

         })
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

    private fun startTimer() {
        handler.postDelayed(runnable, 1000) // INICIALIZAR EL CONTADOR
    }

    private fun getDistanceBetween(originLatLng: LatLng, destinationLatLng: LatLng): Float {
        var distance = 0.0f
        val originLocation = Location("")
        val destinationLocation = Location("")

        originLocation.latitude = originLatLng.latitude
        originLocation.longitude = originLatLng.longitude

        destinationLocation.latitude = destinationLatLng.latitude
        destinationLocation.longitude = destinationLatLng.longitude

        distance = originLocation.distanceTo(destinationLocation)
        return distance
    }

    private fun getBooking() {
        try {
            bookingProvider.getBooking().get().addOnSuccessListener { query ->
                if (query != null) {

                    if (query.size() > 0) {
                        booking = query.documents[0].toObject(Booking::class.java)
                        Log.d("FIRESTORE", "BOOKING ${booking?.toJson()}")
                        originLatLng = LatLng(booking?.originLat!!, booking?.originLng!!)
                        destinationLatLng = LatLng(booking?.destinationLat!!, booking?.destinationLng!!)
                        easyDrawRoute(originLatLng!!)
                        addOriginMarker(originLatLng!!)
                        getClientInfo()
                    }

                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getBooking (MapTripActivity) ${e.message}")
        }
    }

    private fun easyDrawRoute(position: LatLng) {
        try {
            wayPoints.clear()
            wayPoints.add(myLocationLatLng!!)
            wayPoints.add(position)
            directionUtil = DirectionUtil.Builder()
                .setDirectionKey(resources.getString(R.string.google_maps_key))
                .setOrigin(myLocationLatLng!!)
                .setWayPoints(wayPoints)
                .setGoogleMap(googleMap!!)
                .setPolyLinePrimaryColor(R.color.black)
                .setPolyLineWidth(12)
                .setPathAnimation(true)
                .setCallback(this)
                .setDestination(position)
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


    private fun saveLocation() {
        try {
            if (myLocationLatLng != null) {
                geoProvider.saveLocationWorking(authProvider.getId(), myLocationLatLng!!)
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion saveLocation (MapTripActivity) ${e.message}")
        }
    }

    private fun disconnectDriver() {
        try {
            easyWayLocation?.endUpdates()
            if (myLocationLatLng != null) {
                geoProvider.removeLocation(authProvider.getId())
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion disconnectDriver (MapTripActivity) ${e.message}")
        }
    }

    private fun showButtonFinish() {
        binding.btnStartTrip.visibility = View.GONE
        binding.btnFinishTrip.visibility = View.VISIBLE
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
            mensajeLog.LOG("Error en la funcion addMarker (MapTripActivity) ${e.message}")
        }
    }



    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        easyWayLocation?.endUpdates()
        handler.removeCallbacks(runnable)
        stopSensor()
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            googleMap?.uiSettings?.isZoomControlsEnabled = true
            startSensor()
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
            mensajeLog.LOG("Error en la funcion onMapReady (MapTripActivity) ${e.message}")
        }

    }



    private fun updateToStarted() {
        try {
            if (isCloseToOrigin) {
                bookingProvider.updateStatus(booking?.idClient!!, "started").addOnCompleteListener {
                    if (it.isSuccessful) {
                        if (destinationLatLng != null) {
                            isStartedTrip = true
                            googleMap?.clear()
                            addDirectionMarker(myLocationLatLng!!, angle)
                            easyDrawRoute(destinationLatLng!!)
                            markerOrigin?.remove()
                            addDestinationMarker()
                            startTimer()
                        //sendNotification("Viaje iniciado")
                        }
                        showButtonFinish()
                    }
                }
            }
            else {
                Toast.makeText(this, "Debes estar mas cerca a la posicion de recogida", Toast.LENGTH_LONG).show()
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion updateToStarted (MapTripActivity) ${e.message}")
        }
    }

    private fun updateToFinish() {

       try {
           handler.removeCallbacks(runnable) // DETENER CONTADOR
           isStartedTrip = false
           easyWayLocation?.endUpdates()
           geoProvider.removeLocationWorking(authProvider.getId())
           if (min == 0) {
               min = 1
           }
           getPrices(km, min.toDouble())
           mensajeLog.LOG("entrando a la funcion updateToFinish")


           //bookingProvider.updateStatus(booking?.idClient!!, "finished")
           //goToCalificationClient()
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion updateToFinish (MapTripActivity) ${e.message}")
       }
    }

    private fun createHistory() {
        try {
            val history = History(
                idDriver = authProvider.getId(),//Id del conductor
                idClient = booking?.idClient,//Id del cliente
                origin = booking?.origin,//el origen
                destination = booking?.destination,//el destino
                originLat = booking?.originLat,//latitud
                originLng = booking?.originLng,//longitud
                destinationLat = booking?.destinationLat,//destino latitud
                destinationLng = booking?.destinationLng,//destino longitud
                time = min,//tiempo que tomo el viaje
                km = km,//los kilometros recorridos
                price = totalPrice,//el precio
                timestamp = Date().time
            )
            historyProvider.create(history).addOnCompleteListener {
                if (it.isSuccessful) {

                    bookingProvider.updateStatus(booking?.idClient!!, "finished").addOnCompleteListener {
                        if (it.isSuccessful) {
                            //sendNotification("Viaje terminado")
                            goToCalificationClient()//Pantalla de calificacion
                        }
                    }

                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion createHistory (MapTripActivity) ${e.message}")
        }
    }

    private fun getPrices(distance: Double, time: Double) {

        try {
            configProvider.getPrices().addOnSuccessListener { document ->
                if (document.exists()) {
                    val prices = document.toObject(Prices::class.java) // DOCUMENTO CON LA INFORMACION

                    val totalDistance = distance * prices?.km!! // VALOR POR KM
                    Log.d("PRICES", "totalDistance: $totalDistance")
                    val totalTime = time * prices?.minuto!! // VALOR POR MIN
                    Log.d("PRICES", "totalTime: $totalTime")
                    totalPrice =  totalDistance + totalTime // TOTAL
                    Log.d("PRICES", "total: $totalPrice")

                    totalPrice = if (totalPrice < 5.0) prices?.minvalue!! else totalPrice
                    createHistory()
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getPrices (MapTripActivity) ${e.message}")
        }

    }

    private fun goToCalificationClient() {
       try {
           val i = Intent(this, CalificationClientActivity::class.java)
           i.putExtra("price", totalPrice)
           i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
           startActivity(i)
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion goToCalificationClient")
       }
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location) { // ACTUALIZACION DE LA POSICION EN TIEMPO REAL
        try {
            myLocationLatLng = LatLng(location.latitude, location.longitude) // LAT Y LONG DE LA POSICION ACTUAL
            currentLocation = location

            if (isStartedTrip) {
                meters = meters + previusLocation.distanceTo(currentLocation)
                km = meters / 1000
                binding.textViewDistance.text = "${String.format("%.1f", km)} km"
            }

            previusLocation = location

//        if (!isFirstLocation) {
//            isFirstLocation = true
//            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
//                CameraPosition.builder().target(myLocationLatLng!!).zoom(19f).build()
//            ))
//        }

            googleMap?.moveCamera(CameraUpdateFactory.newCameraPosition(
                CameraPosition.builder().target(myLocationLatLng!!).bearing(bearing).tilt(50f).zoom(19f).build()
            ))
            addDirectionMarker(myLocationLatLng!!, angle)
            saveLocation()

            if (booking != null && originLatLng != null) {
                val distance = getDistanceBetween(myLocationLatLng!!, originLatLng!!)
                if (distance <= 300) {
                    isCloseToOrigin = true
                }
                Log.d("LOCATION", "Distance: ${distance}")
            }

            if (!isLocationEnabled) {
                isLocationEnabled = true
                getBooking()
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion currentLocation (MapTripActivity) ${e.message}")
        }

    }

    override fun locationCancelled() {

    }

    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        directionUtil.drawPath(WAY_POINT_TAG)
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
            mensajeLog.LOG("Error en la funcion updateCamera (MapTripActivity) ${e.message}")
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
            mensajeLog.LOG("Error en la funcion addDirectionMarker (MapTripActivity) ${e.message}")
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
            mensajeLog.LOG("Error en la funcion onSensorChanged (MapTripActivity) ${e.message}")
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
            mensajeLog.LOG("Error en la funcion startSensor (MapTripActivity) ${e.message}")
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