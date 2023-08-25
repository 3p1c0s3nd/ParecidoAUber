package com.example.uberclonekotlinconductor.providers


import com.example.uberclonekotlinconductor.api.IFCMApi
import com.example.uberclonekotlinconductor.api.RetrofitClient
import com.example.uberclonekotlinconductor.models.FCMBody
import com.example.uberclonekotlinconductor.models.FCMResponse
import retrofit2.Call

class NotificationProvider {

    private val URL = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody): Call<FCMResponse> {
        return RetrofitClient.getClient(URL).create(IFCMApi::class.java).send(body)
    }

}