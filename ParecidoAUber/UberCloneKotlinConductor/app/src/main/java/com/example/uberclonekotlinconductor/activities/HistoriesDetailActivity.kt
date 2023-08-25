package com.example.uberclonekotlinconductor.activities

import android.annotation.SuppressLint
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.WindowManager
import com.bumptech.glide.Glide
import com.example.uberclonekotlinconductor.ClasesyFunciones.LibreriaFunciones
import com.example.uberclonekotlinconductor.databinding.ActivityHistoryDetailBinding
import com.example.uberclonekotlinconductor.models.Client
import com.example.uberclonekotlinconductor.models.History
import com.example.uberclonekotlinconductor.providers.ClientProvider
import com.example.uberclonekotlinconductor.providers.HistoryProvider
import com.example.uberclonekotlinconductor.utils.RelativeTime

class HistoriesDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityHistoryDetailBinding
    private var historyProvider = HistoryProvider()
    private var clientProvider = ClientProvider()
    private var extraId = ""
    private var mensajeLog = LibreriaFunciones()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityHistoryDetailBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            extraId = intent.getStringExtra("id")!!
            getHistory()

            binding.imageViewBack.setOnClickListener { finish() }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onCreate (HistoriesDetailActivity) ${e.message}")
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
                    binding.textViewPrice.text = "${String.format("%.1f", history?.price)}$"
                    binding.textViewMyCalification.text = "${history?.calificationToDriver}"
                    binding.textViewClientCalification.text = "${history?.calificationToClient}"
                    binding.textViewTimeAndDistance.text = "${history?.time} Min - ${String.format("%.1f", history?.km)} Km"
                    getClientInfo(history?.idClient!!)
                }

            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getHistory (HistoriesDetailActivity) ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun getClientInfo(id: String) {
        try{
            clientProvider.getClientById(id).addOnSuccessListener { document ->
                if (document.exists()) {
                    val client = document.toObject(Client::class.java)
                    binding.textViewEmail.text = client?.email
                    binding.textViewName.text = "${client?.name} ${client?.lastname}"
                    if (client?.image != null) {
                        if (client?.image != "") {
                            Glide.with(this).load(client?.image).into(binding.circleImageProfile)
                        }
                    }
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getClientInfo (HistoriesDetailActivity) ${e.message}")
        }
    }
}