package com.example.igen

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseSettings
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import kotlinx.coroutines.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.BeaconService.TAG
import java.lang.Runnable
import java.util.*
import kotlin.coroutines.*
import kotlin.system.*


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val PERMISSION_REQUEST_FINE_LOCATION = 1
    private val PERMISSION_REQUEST_BACKGROUND_LOCATION = 2
    var beaconsInVicinity = mutableListOf<CBeacon>()
    val content = Content()
    var uniqueID = UUID.randomUUID().toString()
    val URL = "http://130.225.57.152/api/smartphone"
    lateinit var textView: TextView
    lateinit var button1: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button1 = findViewById(R.id.button2)
        textView = findViewById(R.id.text_view)
        button1.setOnClickListener(this)

        checkForPermissions()

        //startRanging()

        initPhoneBeacon()

        //content()
    }

    private fun initPhoneBeacon() {
        try {
            var phoneBeacon = Beacon.Builder()
                .setId1("1")
                .setId2("2")
                .setId3("3")
                .setManufacturer(0x0118).setTxPower(-12).build()

            var beaconParser =
                BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19")

            var beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)

            beaconTransmitter.startAdvertising(phoneBeacon, object : AdvertiseCallback() {
                override fun onStartFailure(errorCode: Int) {
                    Log.e(TAG, "Advertisement start failed with code: $errorCode")
                }

                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    Log.i(TAG, "Advertisement start succeeded.")
                }
            })
        }
        catch (e: java.lang.Exception) {}
    }

    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        beaconsInVicinity.clear()
        for (beacon: Beacon in beacons) {
            beaconsInVicinity.add(CBeacon(beacon.id2.toString(), beacon.distance))
        }
        beaconsInVicinity.sortBy { it.distance }
    }

    private fun content() {
        //var sum = content.averageMiss * content.counter
        //content.counter++
        //content.averageMiss = (sum + abs((content.distance - content.distanceToBeacon))) / content.counter
        //var temp = "Distance based on RSSI: ${content.distance}  \n Actual distance to beacon: ${content.distanceToBeacon} \n Average miss: ${content.averageMiss}  \n Seconds: ${content.counter} \n Rssi: ${content.rssi} \n UUID: ${content.UUID}"

            CoroutineScope(Dispatchers.IO).launch {
                textView.text = postRequest()
            }

        refresh(1000) //Refreshes the screen to update the values displayed
    }

    private fun getBeaconDistances(): CharSequence? {
        var temp = ""

        for (beacon in beaconsInVicinity) {
            temp += beacon.UUID + " " + beacon.distance + "\n"
        }

        return temp
    }

    private fun postRequest(): String? {
        //StrictMode.setThreadPolicy(StrictMode.ThreadPolicy.Builder().detectAll().penaltyLog().build())
            try {
                if (beaconsInVicinity.count() >= 0) {
                    val mediaType = "application/json; charset=utf-8".toMediaType()

                    var jsonString = """{
                    "id": "Yann",
                        "distances": {
                            "1": 2,
                            "2": 2,
                            "3": 2
                            }
                        }"""

                    /*var jsonString = """{
                    "id": "Yann",
                        "distances": {
                            "${beaconsInVicinity[0].UUID}": ${beaconsInVicinity[0].distance},
                            "${beaconsInVicinity[1].UUID}": ${beaconsInVicinity[1].distance},
                            "${beaconsInVicinity[2].UUID}": ${beaconsInVicinity[2].distance}
                            }
                        }"""
                    */
                    val client = OkHttpClient()
                    val request = Request.Builder().url(URL).post(jsonString.toRequestBody(mediaType)).build()
                    val response = client.newCall(request).execute()

                    return response.toString()
                }

            } catch (e: Exception) { return e.toString()}
        return "Fejl"
    }

    private fun refresh(milliseconds: Int) {

        val handler = Handler()
        val runnable = Runnable { content() }
        handler.postDelayed(runnable, milliseconds.toLong())
    }

    private fun checkForPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
            ) {
                if (this.checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
                    != PackageManager.PERMISSION_GRANTED
                ) {
                    if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("This app needs background location access")
                        builder.setMessage("Please grant location access so this app can detect beacons in the background.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener(DialogInterface.OnDismissListener() {

                            @TargetApi(23)
                            @Override
                            fun onDismiss(dialog: DialogInterface) {
                                requestPermissions(
                                    arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                                    PERMISSION_REQUEST_BACKGROUND_LOCATION
                                )
                            }

                        })
                        builder.show()
                    } else {
                        val builder = AlertDialog.Builder(this)
                        builder.setTitle("Functionality limited")
                        builder.setMessage("Since background location access has not been granted, this app will not be able to discover beacons in the background.  Please go to Settings -> Applications -> Permissions and grant background location access to this app.")
                        builder.setPositiveButton(android.R.string.ok, null)
                        builder.setOnDismissListener(DialogInterface.OnDismissListener() {

                            @Override
                            fun onDismiss(dialog: DialogInterface) {
                            }

                        })
                        builder.show()
                    }

                }
            } else {
                if (this.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                    requestPermissions(
                        arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_BACKGROUND_LOCATION
                        ),
                        PERMISSION_REQUEST_FINE_LOCATION
                    )
                } else {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle("Functionality limited")
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons.  Please go to Settings -> Applications -> Permissions and grant location access to this app.")
                    builder.setPositiveButton(android.R.string.ok, null)
                    builder.setOnDismissListener(DialogInterface.OnDismissListener() {

                        @Override
                        fun onDismiss(dialog: DialogInterface) {
                        }

                    })
                    builder.show()
                }
            }
        }
    }

    private fun startRanging() {
        val beaconManager = BeaconManager.getInstanceForApplication(this)

        beaconManager.beaconParsers.add(BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19"))
        BeaconManager.setDebug(true)
        val region = Region("all-beacons-region", null, null, null)

        beaconManager.getRegionViewModel(region).rangedBeacons.observe(this, rangingObserver)
        beaconManager.startRangingBeacons(region)
    }

    override fun onClick(p0: View?) {
        button1.text = "Ranging"
    }
}
