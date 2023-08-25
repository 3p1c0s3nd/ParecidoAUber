package com.example.tipouber.models

import com.beust.klaxon.*

private val klaxon = Klaxon()

data class Prices (
    val km: Double? = null,
    val minuto: Double? = null,
    val minvalue: Double? = null,
    val diferencia: Double? = null
) {
    public fun toJson() = klaxon.toJsonString(this)

    companion object {
        public fun fromJson(json: String) = klaxon.parse<Prices>(json)
    }
}
