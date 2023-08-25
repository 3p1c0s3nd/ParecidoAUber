package com.example.tipouber.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.bumptech.glide.load.engine.executor.GlideExecutor.UncaughtThrowableStrategy.LOG
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
import com.example.tipouber.R
import com.example.tipouber.databinding.ActivityTripInfoBinding
import com.example.tipouber.models.Prices
import com.example.tipouber.providers.ConfigProvider

class TripInfoActivity : AppCompatActivity(), OnMapReadyCallback, Listener, DirectionUtil.DirectionCallBack {

    private var totalPrice: String? = null
    private lateinit var binding: ActivityTripInfoBinding
    private var googleMap: GoogleMap? = null
    private var easyWayLocation: EasyWayLocation? = null

    private var extraOriginName = ""
    private var extraDestinationName = ""
    private var extraOriginLat = 0.0
    private var extraOriginLng = 0.0
    private var extraDestinationLat = 0.0
    private var extraDestinationLng = 0.0
    private var originLatLng: LatLng? = null
    private var destinationLatLng: LatLng? = null

    private var wayPoints: ArrayList<LatLng> = ArrayList()
    private val WAY_POINT_TAG = "way_point_tag"
    private lateinit var directionUtil: DirectionUtil

    private var markerOrigin: Marker? = null
    private var markerDestination: Marker? = null

    var distance = 0.0
    var time = 0.0

    private var configProvider = ConfigProvider()
    private var mensajeLog = LibreriaFunciones()


    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityTripInfoBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            // EXTRAS
            extraOriginName = intent.getStringExtra("origin")!!
            extraDestinationName = intent.getStringExtra("destination")!!
            extraOriginLat = intent.getDoubleExtra("origin_lat", 0.0)
            extraOriginLng = intent.getDoubleExtra("origin_lng", 0.0)
            extraDestinationLat = intent.getDoubleExtra("destination_lat", 0.0)
            extraDestinationLng = intent.getDoubleExtra("destination_lng", 0.0)
            originLatLng = LatLng(extraOriginLat, extraOriginLng)
            destinationLatLng = LatLng(extraDestinationLat, extraDestinationLng)

            val mapFragment = supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
            mapFragment.getMapAsync(this)

            val locationRequest = LocationRequest.create().apply {
                interval = 0
                fastestInterval = 0
                priority = Priority.PRIORITY_HIGH_ACCURACY
                smallestDisplacement = 1f
            }

            easyWayLocation = EasyWayLocation(this, locationRequest, false, false, this)

            binding.textViewOrigin.text = extraOriginName
            binding.textViewDestination.text = extraDestinationName

            Log.d("LOCALIZACION", "Origin lat: ${originLatLng?.latitude}")
            Log.d("LOCALIZACION", "Origin lng: ${originLatLng?.longitude}")
            Log.d("LOCALIZACION", "Destination lat: ${destinationLatLng?.latitude}")
            Log.d("LOCALIZACION", "Destination lng: ${destinationLatLng?.longitude}")

            binding.imageViewBack.setOnClickListener { finish() }
            binding.btnConfirmRequest.setOnClickListener { goToSearchDriver() }
        } catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onCreate (TripInfoActivity) ${e.message}")
        }
    }

    private fun goToSearchDriver() {

        try {
            if (originLatLng != null && destinationLatLng != null) {
                val i = Intent(this, SearchActivity::class.java)
                i.putExtra("origin", extraOriginName)
                i.putExtra("destination", extraDestinationName)
                i.putExtra("origin_lat", originLatLng?.latitude)
                i.putExtra("origin_lng", originLatLng?.longitude)
                i.putExtra("destination_lat", destinationLatLng?.latitude)
                i.putExtra("destination_lng", destinationLatLng?.longitude)
                i.putExtra("time", time)
                i.putExtra("distance", distance)
                i.putExtra("price", totalPrice)
                startActivity(i)
            }
            else {
                Toast.makeText(this, "Debes seleccionar el origin y el destino", Toast.LENGTH_LONG).show()
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion goToSearchDriver (TripInfoActivity) ${e.message}")
        }

    }

    @SuppressLint("SetTextI18n")
    private fun getPrices(distance: Double, time: Double) {

        try {
            Log.i("DOSDOS","${distance}")
            configProvider.getPrices().addOnSuccessListener { document ->
                if (document.exists()) {
                    val prices = document.toObject(Prices::class.java) // DOCUMENTO CON LA INFORMACION

                    val totalDistance = distance * prices?.km!! // VALOR POR KM
                    Log.d("PRICES", "totalDistance: $totalDistance")
                    val totalTime = time * prices.minuto!! // VALOR POR MIN
                    Log.d("PRICES", "totalTime: $totalTime")
                    var total =  totalDistance + totalTime // TOTAL
                    Log.d("PRICES", "total: $total")

                    total = if (total < 5.0) prices.minvalue!! else total
                    Log.d("PRICES", "new total: $total")

                    val minTotal = total - prices.diferencia!! // TOTAL - 2USD
                    Log.d("PRICES", "minTotal: $minTotal")

                    val maxTotal = total + prices.diferencia // TOTAL + 2USD
                    Log.d("PRICES", "maxTotal: $maxTotal")


                    val minTotalString = String.format("%.1f", minTotal)
                    val maxTotalString = String.format("%.1f", maxTotal)
                    totalPrice = "$minTotalString - $maxTotalString$"
                    binding.textViewPrice.text = "$minTotalString - $maxTotalString$"


                }else{
                    Log.i("DOSDOS","documento no existe")
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getPrices (TripInfoActivity) ${e.message}")
        }

    }

    private fun addOriginMarker() {
        try {
            markerOrigin = googleMap?.addMarker(MarkerOptions().position(originLatLng!!).title("Mi posicion")
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_location_person)))
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion addOriginMarker (TripInfoActivity) ${e.message}")
        }
    }

    private fun addDestinationMarker() {
       try {
           markerDestination = googleMap?.addMarker(MarkerOptions().position(destinationLatLng!!).title("LLegada")
               .icon(BitmapDescriptorFactory.fromResource(R.drawable.icons_pin)))
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion addDestinationMarker (TripInfoActivity) ${e.message}")
       }
    }

    private fun easyDrawRoute() {
       try {
           wayPoints.add(originLatLng!!)
           wayPoints.add(destinationLatLng!!)
           directionUtil = DirectionUtil.Builder()
               .setDirectionKey(resources.getString(R.string.google_maps_key))
               .setOrigin(originLatLng!!)
               .setWayPoints(wayPoints)
               .setGoogleMap(googleMap!!)
               .setPolyLinePrimaryColor(R.color.black)
               .setPolyLineWidth(25)
               .setPathAnimation(true)
               .setCallback(this)
               .setDestination(destinationLatLng!!)
               .build()

           directionUtil.initPath()
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion easyDrawRoute (TripInfoActivity) ${e.message}")
       }
    }

    override fun onMapReady(map: GoogleMap) {
        try {
            googleMap = map
            googleMap?.uiSettings?.isZoomControlsEnabled = true

            googleMap?.moveCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.builder().target(originLatLng!!).zoom(13f).build()
                ))
            easyDrawRoute()
            addOriginMarker()
            addDestinationMarker()

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
            mensajeLog.LOG("Error en la funcion onMapReady (TripInfoActivity) ${e.message}")
        }
    }

    override fun locationOn() {

    }

    override fun currentLocation(location: Location?) {

    }

    override fun locationCancelled() {

    }

    override fun onDestroy() { // CIERRA APLICACION O PASAMOS A OTRA ACTIVITY
        super.onDestroy()
        easyWayLocation?.endUpdates()
    }

    @SuppressLint("SetTextI18n")
    override fun pathFindFinish(
        polyLineDetailsMap: HashMap<String, PolyLineDataBean>,
        polyLineDetailsArray: ArrayList<PolyLineDataBean>
    ) {
        try {
            distance = polyLineDetailsArray[1].distance.toDouble() // METROS
            time = polyLineDetailsArray[1].time.toDouble() // SEGUNDOS
            distance = if (distance < 1000.0) 1000.0 else distance // SI ES MENOS DE 1000 METROS EN 1 KM
            time = if (time < 60.0) 60.0 else time

            distance = distance / 1000 // KM
            time = time / 60 // MIN

            val timeString = String.format("%.2f", time)
            val distanceString = String.format("%.2f", distance)

            getPrices(distance, time)
            binding.textViewTimeAndDistance.text = "$timeString mins - $distanceString km"

            directionUtil.drawPath(WAY_POINT_TAG)
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion pathFindFinish (TripInfoActivity) ${e.message}")
        }
    }
}