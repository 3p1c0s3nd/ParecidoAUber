package com.example.tipouber.adapters

import android.app.Activity
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.example.tipouber.R
import com.example.tipouber.activities.HistoriesDetailActivity
import com.example.tipouber.models.History
import com.example.tipouber.utils.RelativeTime

class HistoriesAdapter(val context: Activity, val histories: ArrayList<History>): RecyclerView.Adapter<HistoriesAdapter.HistoriesAdapterViewHolder>() {

    private var mensajeLog = LibreriaFunciones()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoriesAdapterViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.cardview_history, parent, false)
        return HistoriesAdapterViewHolder(view)
    }

    // ESTABLECER LA INFORMACION
    override fun onBindViewHolder(holder: HistoriesAdapterViewHolder, position: Int) {

        try {
            val history =  histories[position] // UN SOLO HISTORIAL
            holder.textViewOrigin.text = history.origin
            holder.textViewDestination.text = history.destination
            if (history.timestamp != null) {
                holder.textViewDate.text = RelativeTime.getTimeAgo(history.timestamp!!, context)
            }

            holder.itemView.setOnClickListener { goToDetail(history?.id!!) }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion onBindViewHolder (HistoriesAdapter) ${e.message}")
        }
    }

    private fun goToDetail(idHistory: String) {
       try {
           val i = Intent(context, HistoriesDetailActivity::class.java)
           i.putExtra("id", idHistory)
           context.startActivity(i)
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion goToDetail (HistoriesAdapter) ${e.message}")
       }
    }

    // EL TAMAñO DE LA LISTA QUE VAMOS A MOSTRAR
    override fun getItemCount(): Int {
        return histories.size
    }


    class HistoriesAdapterViewHolder(view: View): RecyclerView.ViewHolder(view) {

        val textViewOrigin: TextView
        val textViewDestination: TextView
        val textViewDate: TextView

        init {
            textViewOrigin = view.findViewById(R.id.textViewOrigin)
            textViewDestination = view.findViewById(R.id.textViewDestination)
            textViewDate = view.findViewById(R.id.textViewDate)
        }

    }


}