package com.example.tipouber.activities

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.example.tipouber.databinding.ActivityHistoriesDetailBinding
import com.example.tipouber.models.Driver
import com.example.tipouber.models.History
import com.example.tipouber.providers.DriverProvider
//import com.example.tipouber.providers.DriverProvider
import com.example.tipouber.providers.HistoryProvider
import com.example.tipouber.utils.RelativeTime

class HistoriesDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoriesDetailBinding
    private var historyProvider = HistoryProvider()
    private var driverProvider = DriverProvider()
    private var extraId = ""
    private var mensajeLog = LibreriaFunciones()


    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityHistoriesDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            extraId = intent.getStringExtra("id")!!
            getHistory()

            binding.imageViewBack.setOnClickListener { finish() }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onCreate (HistoriesDetailActivity)")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getHistory() {
        try {
            historyProvider.getHistoryById(extraId).addOnSuccessListener { document ->

                if (document.exists()) {
                    val history = document.toObject(History::class.java)
                    binding.textViewOrigin.text = history?.origin
                    binding.textViewDestination.text = history?.destination
                    binding.textViewDate.text = RelativeTime.getTimeAgo(history?.timestamp!!, this@HistoriesDetailActivity)
                    binding.textViewPrice.text = "${String.format("%.1f", history.price)}$"
                    binding.textViewMyCalification.text = "${history.calificationToDriver}"
                    binding.textViewClientCalification.text = "${history.calificationToClient}"
                    binding.textViewTimeAndDistance.text = "${history.time} Min - ${String.format("%.1f", history.km
                    )} Km"
                    getDriverInfo(history?.idDriver!!)
                }

            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getHistory (HistoriesDetailActivity) ${e.message}")
        }
    }

    private fun getDriverInfo(id: String) {
        driverProvider.getDriver(id).addOnSuccessListener { document ->
            if (document.exists()) {
                val driver = document.toObject(Driver::class.java)
                binding.textViewEmail.text = driver?.email
                binding.textViewName.text = "${driver?.name} ${driver?.lastname}"
                if (driver?.image != null) {
                    if (driver?.image != "") {
                        Glide.with(this).load(driver?.image).into(binding.circleImageProfile)
                    }
                }
            }
        }
    }
}