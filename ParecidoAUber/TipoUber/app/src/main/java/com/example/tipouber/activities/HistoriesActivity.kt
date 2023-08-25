package com.example.tipouber.activities

import android.graphics.Color
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.example.tipouber.adapters.HistoriesAdapter
import com.example.tipouber.databinding.ActivityHistoriesBinding
import com.example.tipouber.models.History
import com.example.tipouber.providers.HistoryProvider

class HistoriesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHistoriesBinding
    private var historyProvider = HistoryProvider()
    private var histories = ArrayList<History>()
    private lateinit var adapter: HistoriesAdapter
    private var mensajeLog = LibreriaFunciones()

    override fun onCreate(savedInstanceState: Bundle?) {
       try {
           super.onCreate(savedInstanceState)
           binding = ActivityHistoriesBinding.inflate(layoutInflater)
           setContentView(binding.root)

           val linearLayoutManager = LinearLayoutManager(this)
           binding.recyclerViewHistories.layoutManager = linearLayoutManager

           setSupportActionBar(binding.toolbar)
           supportActionBar?.title = "Historial de viajes"
           supportActionBar?.setDisplayHomeAsUpEnabled(true)
           binding.toolbar.setTitleTextColor(Color.WHITE)

           getHistories()
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion onCreate (HistoriesActivity) ${e.message}")
       }
    }

    private fun getHistories() {
        try {
            histories.clear()

            historyProvider.getHistories().get().addOnSuccessListener { query ->

                if (query != null) {
                    if (query.documents.size > 0) {
                        val documents = query.documents

                        for (d in documents) {
                            val history = d.toObject(History::class.java)
                            history?.id = d.id
                            histories.add(history!!)
                        }

                        adapter = HistoriesAdapter(this@HistoriesActivity, histories)
                        binding.recyclerViewHistories.adapter = adapter
                    }
                }

            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getHistories (HistoriesActivity) ${e.message}")
        }
    }
}