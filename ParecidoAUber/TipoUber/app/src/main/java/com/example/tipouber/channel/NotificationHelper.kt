package com.example.tipouber.channel

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.example.tipouber.ClasesyFunciones.LibreriaFunciones
import com.example.tipouber.R

class NotificationHelper(base: Context): ContextWrapper(base) {

    private val CHANNEL_ID = "com.example.tipouber"
    private val CHANNEL_NAME = "Uber Clone Kotlin"
    private var manager: NotificationManager? = null
    private var mensajeLog = LibreriaFunciones()


    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createChannels()
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    fun createChannels() {

        try {
            val notificationChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.enableVibration(true)
            notificationChannel.lightColor = Color.WHITE
            notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE

            getManager().createNotificationChannel(notificationChannel)
        }catch (e: Exception){
            mensajeLog.LOG("Error en la funcion createChannels (NotificationHelper) ${e.message}")
        }
    }

    fun getManager(): NotificationManager {
        if (manager == null) {
            manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        }
        return manager as NotificationManager
    }

    fun getNotification(title: String, body: String): NotificationCompat.Builder {
        return NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setColor(Color.GRAY)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body).setBigContentTitle(title))
    }

}