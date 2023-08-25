package com.example.tipouber.fragments

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.toObject
import com.example.tipouber.R
import com.example.tipouber.activities.*
import com.example.tipouber.models.Booking
import com.example.tipouber.models.Client
import com.example.tipouber.models.Driver
import com.example.tipouber.providers.*

class ModalBottomSheetMenu: BottomSheetDialogFragment() {

    val clientProvider = ClientProvider()
    val authProvider = AuthProvider()

    var textViewUsername: TextView? = null
    var linearLayoutLogout: LinearLayout? = null
    var linearLayoutProfile: LinearLayout? = null
    var linearLayoutHistory: LinearLayout? = null
    private var mensajeLog = LibreriaFunciones()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.modal_bottom_sheet_menu, container, false)

        textViewUsername = view.findViewById(R.id.textViewUsername)
        linearLayoutLogout = view.findViewById(R.id.linearLayoutLogout)
        linearLayoutProfile = view.findViewById(R.id.linearLayoutProfile)
        linearLayoutHistory = view.findViewById(R.id.linearLayoutHistory)

        getClient()

        linearLayoutLogout?.setOnClickListener { goToMain() }
        linearLayoutProfile?.setOnClickListener { goToProfile() }
        linearLayoutHistory?.setOnClickListener { goToHistories() }
        return view
    }

    private fun goToProfile() {
      try {
          val i = Intent(activity, ProfileActivity::class.java)
          startActivity(i)
      }catch (e: Exception)
      {
          mensajeLog.LOG("Error en la funcion goToProfile (ModalBottomSheetMenu) ${e.message}")
      }
    }

    private fun goToHistories() {
        try {
            val i = Intent(activity, HistoriesActivity::class.java)
            startActivity(i)
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion goToHistories (ModalBottomSheetMenu)")
        }
    }

    private fun goToMain() {
        try {
            authProvider.logout()
            val i = Intent(activity, MainActivity::class.java)
            i.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(i)
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion goToMain (ModalBottomSheetMenu) ${e.message}")
        }

    }

    @SuppressLint("SetTextI18n")
    private fun getClient() {
        try {
            clientProvider.getClientById(authProvider.getId()).addOnSuccessListener { document ->
                if (document.exists()) {
                    val client = document.toObject(Client::class.java)
                    textViewUsername?.text = "${client?.name} ${client?.lastname}"
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getClient (ModalBottomSheetMeny) ${e.message}")
        }
    }

    companion object {
        const val TAG = "ModalBottomSheetMenu"
    }


}