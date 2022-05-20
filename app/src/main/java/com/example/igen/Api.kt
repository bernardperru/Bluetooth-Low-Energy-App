package com.example.igen

import android.widget.TextView
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

//Class for API requests
class Api {
    private val client = OkHttpClient()
    private val mediaType = "application/json; charset=utf-8".toMediaType()
    private val urlPostDistances = "http://130.225.57.152/api/smartphone"
    private val urlPostBeacon = "http://130.225.57.152/api/beacon"
    private val urlPostAuto = "http://130.225.57.152/api/data"

    // Sends POST request
    fun postRequest(beaconsInVicinityMap: HashMap<String, CBeacon>, id: Int): String {
        try {
            val beaconDistances = HashMap<String, Double>()

            for (value in beaconsInVicinityMap.values) {
                beaconDistances[value.UUID] = value.distance
            }

            val sortedMap = beaconDistances.toList().sortedBy { (k,v) -> v }.toMap()

            val jsonString = """{
            "id": "${makeHex(id)}",
                "distances": ${Gson().toJson(sortedMap)}
                }"""

            val request = Request.Builder().url(urlPostDistances).post(jsonString.toRequestBody(mediaType)).build()
            val response = client.newCall(request).execute()

            return response.body!!.string()

        } catch (e: Exception) { return e.toString()}
    }

    // same as the function above...
    fun postRequestAuto(beaconsInVicinityMap: HashMap<String, CBeacon>, id: Int): String {
        try {
            val beaconDistances = HashMap<String, Double>()

            for (value in beaconsInVicinityMap.values) {
                beaconDistances[value.UUID] = value.distance
            }

            val sortedMap = beaconDistances.toList().sortedBy { (k,v) -> v }.toMap()

            val jsonString = """{
            "id": "${makeHex(id)}",
                "distances": ${Gson().toJson(sortedMap)}
                }"""

            val request = Request.Builder().url(urlPostAuto).post(jsonString.toRequestBody(mediaType)).build()
            val response = client.newCall(request).execute()

            return response.body!!.string()

        } catch (e: Exception) { return e.toString()}
    }

    //Adds the phone beacon to the API
    fun postPhoneBeacon(positions: Positions, id: Int, description: String, xCord: String, yCord: String, postCheck: Boolean): String {
        try {
            var jsonString = ""
            if (xCord != "X" && yCord != "Y") {
                jsonString = """{
                              "description": "$description",
                              "id": "${makeHex(id)}",
                              "position": {
                                "x": $xCord,
                                "y": $yCord
                              } 
                            }"""
            }
            else if (postCheck){
                jsonString = """{
                              "description": "$description",
                              "id": "${makeHex(id)}",
                              "position": {
                                "x": ${positions.position.x},
                                "y": ${positions.position.y}
                              } 
                            }"""
            }

            val request = Request.Builder().url(urlPostBeacon).post(jsonString.toRequestBody(mediaType)).build()
            val response = client.newCall(request).execute()

            return response.body!!.string()

        } catch (e: Exception) {}
        return "error"
    }

    //For deleting phone beacons from the API after they are done being used
    fun deletePhoneBeacon(id: Int) {
        try {
            val request = Request.Builder().url("$urlPostBeacon/${makeHex(id)}").delete().build()
            client.newCall(request).execute()
        } catch (e: Exception) {}
    }

    //All beacon id's are in hex, so we follow that standard
    private fun makeHex(id: Int): String {
        var idHex = "0x"

        for (i in 1..(12-(id.toString().length))) {
            idHex += "0"
        }

        idHex += id.toString()

        return idHex
    }

}