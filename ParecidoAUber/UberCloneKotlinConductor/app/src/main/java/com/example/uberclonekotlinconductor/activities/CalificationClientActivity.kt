package com.example.uberclonekotlinconductor.activities

import android.annotation.SuppressLint
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.uberclonekotlinconductor.ClasesyFunciones.LibreriaFunciones
import com.example.uberclonekotlinconductor.databinding.ActivityCalificationClientBinding
import com.example.uberclonekotlinconductor.models.History
import com.example.uberclonekotlinconductor.providers.HistoryProvider

class CalificationClientActivity : AppCompatActivity() {

    private var history: History? = null
    private lateinit var binding: ActivityCalificationClientBinding
    private var extraPrice = 0.0
    private var historyProvider = HistoryProvider()
    private var calification = 0f
    private var mensajeLog = LibreriaFunciones()


    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityCalificationClientBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            extraPrice = intent.getDoubleExtra("price", 0.0)
            binding.textViewPrice.text = "${String.format("%.1f", extraPrice)}$"

            binding.ratingBar.setOnRatingBarChangeListener { ratingBar, value, b ->
                calification = value
            }

            getHistory()
            binding.btnCalification.setOnClickListener {
                if (history?.id != null) {
                    updateCalification(history?.id!!)
                }
                else {
                    Toast.makeText(this, "El id del historial es nulo", Toast.LENGTH_LONG).show()
                }
            }


        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onCreate (CalificationClientActivity) ${e.message}")
        }
    }

    private fun updateCalification(idDocument: String) {
        try {
            historyProvider.updateCalificationToClient(idDocument, calification).addOnCompleteListener {
                if (it.isSuccessful) {
                    goToMap()
                }
                else {
                    Toast.makeText(this@CalificationClientActivity, "Error al actualizar la calificacion", Toast.LENGTH_LONG).show()
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion updateCalification (CalificationClientActivity) ${e.message}")
        }
    }

    private fun goToMap() {
       try {
           val i = Intent(this, MapActivity::class.java)
           i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
           startActivity(i)
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion goToMap (CalificationClientActivity) ${e.message}")
       }
    }

    @SuppressLint("SetTextI18n")
    private fun getHistory() {
        try {
            Log.d("FIRESTORE", "ENTRANDO A LA FUNCION getHistory")
            historyProvider.getLastHistory().get().addOnSuccessListener { query ->
                if (query != null) {

                    if (query.documents.size > 0) {
                        history = query.documents[0].toObject(History::class.java)
                        history?.id = query.documents[0].id
                        binding.textViewOrigin.text = history?.origin
                        binding.textViewDestination.text = history?.destination
                        binding.textViewTimeAndDistance.text = "${history?.time} Min - ${String.format("%.1f", history?.km)} Km"

                        Log.d("FIRESTORE", "hISTORIAL: ${history?.toJson()}")
                    }
                    else {
                        Toast.makeText(this, "No se encontro el historial", Toast.LENGTH_LONG).show()
                    }

                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getHistory (CalificationClientActivity) ${e.message}")
        }
    }
}