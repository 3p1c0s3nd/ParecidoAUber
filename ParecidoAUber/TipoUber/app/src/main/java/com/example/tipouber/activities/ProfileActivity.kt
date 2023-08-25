package com.example.tipouber.activities

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.bumptech.glide.Glide
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.protobuf.Empty
import com.example.tipouber.databinding.ActivityProfileBinding
import com.example.tipouber.models.Client
import com.example.tipouber.providers.AuthProvider
import com.example.tipouber.providers.ClientProvider
import java.io.File

class ProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityProfileBinding
    val clientProvider = ClientProvider()
    val authProvider = AuthProvider()

    private var imageFile: File? = null
    private var mensajeLog = LibreriaFunciones()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityProfileBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

            getClient()
            binding.imageViewBack.setOnClickListener { finish() }
            binding.btnUpdate.setOnClickListener { updateInfo() }
            binding.circleImageProfile.setOnClickListener { selectImage() }
        }catch (e: Exception){
            mensajeLog.LOG("Error en la funcion onCreate (ProfileActivity) ${e.message}")
        }

    }


    private fun updateInfo() {

        try {
            val name = binding.textFieldName.text.toString()
            val lastname = binding.textFieldLastname.text.toString()
            val phone = binding.textFieldPhone.text.toString()



            if (imageFile != null) {
                val client = Client(
                    id = authProvider.getId(),
                    name = name,
                    lastname = lastname,
                    phone = phone,
                )

                clientProvider.uploadImage(authProvider.getId(), imageFile!!).addOnSuccessListener { taskSnapshot ->
                        clientProvider.getImageUrl().addOnSuccessListener { url ->
                        val imageUrl = url.toString()
                        client.image = imageUrl
                        clientProvider.update(client).addOnCompleteListener {
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
            }
            else {
                val client = Client(
                    id = authProvider.getId(),
                    name = name,
                    lastname = lastname,
                    phone = phone,
                    image = "null"
                )
                clientProvider.update(client).addOnCompleteListener {
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

    private fun getClient() {
        try {
            clientProvider.getClientById(authProvider.getId()).addOnSuccessListener { document ->
                if (document.exists()) {
                    val client = document.toObject(Client::class.java)
                    binding.textViewEmail.text = client?.email
                    binding.textFieldName.setText(client?.name)
                    binding.textFieldLastname.setText(client?.lastname)
                    binding.textFieldPhone.setText(client?.phone)

                    if (client?.image != null) {
                        if (client.image != "") {
                            Glide.with(this).load(client.image).into(binding.circleImageProfile)
                        }
                    }
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion getClient (ProfileActivity) ${e.message}")
        }
    }

    private val startImageForResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->

        try {
            val resultCode = result.resultCode
            val data = result.data

            if (resultCode == Activity.RESULT_OK) {
                val fileUri = data?.data
                imageFile = File(fileUri?.path)
                binding.circleImageProfile.setImageURI(fileUri)
            }
            else if (resultCode == ImagePicker.RESULT_ERROR) {
                Toast.makeText(this, ImagePicker.getError(data), Toast.LENGTH_LONG).show()
            }
            else {
                Toast.makeText(this, "Tarea cancelada", Toast.LENGTH_LONG).show()
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion startImageForResult (ProfileActivity) ${e.message}")
        }

    }

    private fun selectImage() {
      try {
          ImagePicker.with(this)
              .crop()
              .compress(1024)
              .maxResultSize(1080,1080)
              .createIntent { intent ->
                  startImageForResult.launch(intent)
              }
      }catch (e: Exception)
      {
          mensajeLog.LOG("Error en la funcion selectImage (ProfileActivity) ${e.message}")
      }
    }

}