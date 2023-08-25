package com.example.uberclonekotlinconductor.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import com.example.uberclonekotlinconductor.ClasesyFunciones.LibreriaFunciones
import com.example.uberclonekotlinconductor.R
import com.example.uberclonekotlinconductor.activities.MapActivity
import com.example.uberclonekotlinconductor.activities.RegisterActivity
import com.example.uberclonekotlinconductor.databinding.ActivityMainBinding
import com.example.uberclonekotlinconductor.providers.AuthProvider

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    val authProvider = AuthProvider()
    private var mensajeLog = LibreriaFunciones()

    override fun onCreate(savedInstanceState: Bundle?) {
        try {
            super.onCreate(savedInstanceState)
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)
            window.setFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS, WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)


            binding.btnRegister.setOnClickListener { goToRegister() }
            binding.btnLogin.setOnClickListener { login() }
        }catch (e: Exception){
            mensajeLog.LOG("Error en la funcion onCreate (MainActivity) ${e.message}")
        }

    }

    private fun login() {
        try {
            val email = binding.textFieldEmail.text.toString()
            val password = binding.textFieldPassword.text.toString()

            if (isValidForm(email, password)) {
                authProvider.login(email, password).addOnCompleteListener {
                    if (it.isSuccessful) {
                        goToMap()
                    }
                    else {
                        Toast.makeText(this@MainActivity, "Error iniciando sesion", Toast.LENGTH_SHORT).show()
                        Log.d("FIREBASE", "ERROR: ${it.exception.toString()}")
                    }
                }
            }
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion login (MainActivity) ${e.message}")
        }
    }

    private fun goToMap() {
       try {
           val i = Intent(this, MapActivity::class.java)
           i.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
           startActivity(i)
       }catch (e: Exception)
       {
           mensajeLog.LOG("Error en la funcion goToMap (MainActivity) ${e.message}")
       }
    }

    private fun isValidForm(email: String, password: String): Boolean {

        if (email.isEmpty()) {
            Toast.makeText(this, "Ingresa tu correo electronico", Toast.LENGTH_SHORT).show()
            return false
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Ingresa tu contraseña", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    private fun goToRegister() {
        try {
            val i = Intent(this, RegisterActivity::class.java)
            startActivity(i)
        }catch (e: Exception)
        {
            mensajeLog.LOG("Error en la funcion goToRegister (MainActivity) ${e.message}")
        }
    }

    override fun onStart() {
        super.onStart()
        if (authProvider.existSession()) {
            goToMap()
        }
    }

}