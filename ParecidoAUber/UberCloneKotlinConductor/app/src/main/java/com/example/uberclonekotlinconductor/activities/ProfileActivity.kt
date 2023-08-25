package com.example.uberclonekotlinconductor.activities

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.uberclonekotlinconductor.ClasesyFunciones.LibreriaFunciones
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.protobuf.Empty
import com.example.uberclonekotlinconductor.databinding.ActivityProfileBinding
import com.example.uberclonekotlinconductor.models.Driver
import com.example.uberclonekotlinconductor.providers.AuthProvider
import com.example.uberclonekotlinconductor.providers.DriverProvider
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    val driverProvider = DriverProvider()
    val authProvider = AuthProvider()

    private var imageFile: File? = null
    private var mensajeLog = LibreriaFunciones()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            getDriver()
            binding.imageViewBack.setOnClickListener { finish() }
            binding.btnUpdate.setOnClickListener { updateInfo() }
            binding.circleImageProfile.setOnClickListener { selectImage() }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion  onCreate (ProfileActivity) ${e.message}")
        }
    }


    private fun updateInfo() {

        try {
            val name = binding.textFieldName.text.toString()
            val lastname = binding.textFieldLastname.text.toString()
            val phone = binding.textFieldPhone.text.toString()
            val carBrand = binding.textFieldCarBrand.text.toString()
            val carColor = binding.textFieldCarColor.text.toString()
            val carPlate = binding.textFieldCarPlate.text.toString()

            if (imageFile != null) {
                Log.i("ERRORES", authProvider.getId())
                val driver = Driver(
                    id = authProvider.getId(),
                    name = name,
                    lastname = lastname,
                    phone = phone,
                    colorCar = carColor,
                    brandCar = carBrand,
                    plateNumber = carPlate
                )
                Log.i("ERRORES", authProvider.getId())
                Log.i("ERRORES", name)
                Log.i("ERRORES", lastname)
                Log.i("ERRORES", phone)
                Log.i("ERRORES", carColor)
                Log.i("ERRORES", carBrand)
                Log.i("ERRORES", carPlate)
                driverProvider.uploadImage(authProvider.getId(), imageFile!!).addOnSuccessListener { taskSnapshot ->
                    driverProvider.getImageUrl().addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        driver.image = imageUrl
                        driverProvider.update(driver).addOnCompleteListener {
                            if (it.isSuccessful) {
                                Toast.makeText(this@ProfileActivity, "Datos actualizados correctamente", Toast.LENGTH_LONG).show()
                            }
                            else {
                                Toast.makeText(this@ProfileActivity, "No se pudo actualizar la informacion", Toast.LENGTH_LONG).show()
                            }
                        }
                        Log.d("STORAGE", "$imageUrl")
                    }
                }
            } else {
                Log.i("ERRORES", authProvider.getId())
                val driver = Driver(
                    id = authProvider.getId(),
                    name = name,
                    lastname = lastname,
                    phone = phone,
                    colorCar = carColor,
                    brandCar = carBrand,
                    plateNumber = carPlate,
                    image = "https://www.shutterstock.com/image-photo/artistic-image-magic-tree-600w-2272706835.jpg"
                )
                Log.i("ERRORES", authProvider.getId())
                Log.i("ERRORES", name)
                Log.i("ERRORES", lastname)
                Log.i("ERRORES", phone)
                Log.i("ERRORES", carColor)
                Log.i("ERRORES", carBrand)
                Log.i("ERRORES", carPlate)
                driverProvider.update(driver).addOnCompleteListener {
                    if (it.isSuccessful) {
                        Toast.makeText(this@ProfileActivity, "Datos actualizados correctamente", Toast.LENGTH_LONG).show()
                    }
                    else {
                        Toast.makeText(this@ProfileActivity, "No se pudo actualizar la informacion", Toast.LENGTH_LONG).show()
                    }
                }
            }

        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion updateInfo (ProfileActivity) ${e.message}")
        }

    }

    private fun getDriver() {
        try {
            driverProvider.getDriver(authProvider.getId()).addOnSuccessListener { document ->
                if (document.exists()) {
                    val driver = document.toObject(Driver::class.java)
                    binding.textViewEmail.text = driver?.email
                    binding.textFieldName.setText(driver?.name)
                    binding.textFieldLastname.setText(driver?.lastname)
                    binding.textFieldPhone.setText(driver?.phone)
                    binding.textFieldCarBrand.setText(driver?.brandCar)
                    binding.textFieldCarColor.setText(driver?.colorCar)
                    binding.textFieldCarPlate.setText(driver?.plateNumber)

                    if (driver?.image != null) {
                        if (driver.image != "") {
                            Glide.with(this).load(driver.image).into(binding.circleImageProfile)
                        }
                    }
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getDriver (ProfileActivity) ${e.message}")
        }
    }

    private val startImageForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

        val resultCode = result.resultCode
        val data = result.data

        if (resultCode == Activity.RESULT_OK) {
            val fileUri = data?.data
            imageFile = File(fileUri?.path.toString())
            binding.circleImageProfile.setImageURI(fileUri)
        }
        else if (resultCode == ImagePicker.RESULT_ERROR) {
            Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_LONG).show()
        }
        else {
            Toast.makeText(this, "Tarea cancelada", Toast.LENGTH_LONG).show()
        }

    }

    private fun selectImage() {
        ImagePicker.with(this)
            .crop()
            .compress(1024)
            .maxResultSize(1080,1080)
            .createIntent { intent ->
                startImageForResult.launch(intent)
            }
    }

}