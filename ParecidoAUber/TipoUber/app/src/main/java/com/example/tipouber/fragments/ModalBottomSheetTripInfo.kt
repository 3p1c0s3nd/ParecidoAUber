package com.example.tipouber.fragments

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.firestore.ktx.toObject
import com.example.tipouber.R
import com.example.tipouber.activities.*
import com.example.tipouber.models.Booking
import com.example.tipouber.models.Client
import com.example.tipouber.models.Driver
import com.example.tipouber.providers.*
//import de.hdodenhof.circleimageview.CircleImageView

class ModalBottomSheetTripInfo: BottomSheetDialogFragment() {

    private var driver: Driver? = null
    private lateinit var booking: Booking
    val driverProvider = DriverProvider()
    val authProvider = AuthProvider()
    var textViewClientName: TextView? = null
    var textViewOrigin: TextView? = null
    var textViewDestination: TextView? = null
    var imageViewPhone: ImageView? = null
    private var mensajeLog = LibreriaFunciones()
    //var circleImageClient: CircleImageView? = null

    val REQUEST_PHONE_CALL = 30


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.modal_bottom_sheet_trip_info, container, false)

        textViewClientName = view.findViewById(R.id.textViewClientName)
        textViewOrigin = view.findViewById(R.id.textViewOrigin)
        textViewDestination = view.findViewById(R.id.textViewDestination)
        imageViewPhone = view.findViewById(R.id.imageViewPhone)
        //circleImageClient = view.findViewById(R.id.circleImageClient)

//        getDriver()
        val data = arguments?.getString("booking")
        booking = Booking.fromJson(data!!)!!

        textViewOrigin?.text = booking.origin
        textViewDestination?.text = booking.destination
        imageViewPhone?.setOnClickListener {
            if (driver?.phone != null) {

                if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CALL_PHONE), REQUEST_PHONE_CALL)
                }

                call(driver?.phone!!)
            }

        }

        getDriverInfo()
        return view
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_PHONE_CALL) {
            if (driver?.phone != null) {
                call(driver?.phone!!)
            }
        }

    }

    private fun call(phone: String) {

        val i = Intent(Intent.ACTION_CALL)
        i.data = Uri.parse("tel:$phone")

        if (ActivityCompat.checkSelfPermission(requireActivity(), Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return
        }

        requireActivity().startActivity(i)
    }

    private fun getDriverInfo() {
        driverProvider.getDriver(booking.idDriver!!).addOnSuccessListener { document ->
            if (document.exists()) {
                driver = document.toObject(Driver::class.java)
                textViewClientName?.text = "${driver?.name} ${driver?.lastname}"

                if (driver?.image != null) {
                    if (driver?.image != "") {
                        //Glide.with(requireActivity()).load(driver?.image).into(circleImageClient!!)
                    }
                }
//                textViewUsername?.text = "${driver?.name} ${driver?.lastname}"
            }
        }
    }

    companion object {
        const val TAG = "ModalBottomSheetTripInfo"
    }


}