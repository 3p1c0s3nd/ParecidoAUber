package com.example.tipouber.providers

import com.example.tipouber.api.IFCMApi
import com.example.tipouber.api.RetrofitClient
import com.example.tipouber.models.FCMBody
import com.example.tipouber.models.FCMResponse
import retrofit2.Call

class NotificationProvider {

    private val URL = "https://fcm.googleapis.com"

    fun sendNotification(body: FCMBody): Call<FCMResponse> {
        return RetrofitClient.getClient(URL).create(IFCMApi::class.java).send(body)
    }

}