package com.example.igen

import android.Manifest
import android.annotation.TargetApi
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.altbeacon.beacon.Beacon
import org.altbeacon.beacon.BeaconManager
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Region
import org.altbeacon.beacon.service.BeaconService
import org.json.JSONObject
import kotlin.math.abs


class MainActivity : AppCompatActivity() {
    private val PERMISSION_REQUEST_FINE_LOCATION = 1
    private val PERMISSION_REQUEST_BACKGROUND_LOCATION = 2
    var beaconsInVicinity = mutableListOf<CBeacon>()
    val content = Content()
    val URL = "130.225.52.157"
    lateinit var textView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        textView = findViewById(R.id.text_view)

        checkForPermissions()

        startRanging()

        content()
    }

    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        Log.d(BeaconService.TAG, "Ranged: ${beacons.count()} beacons")
        for (beacon: Beacon in beacons) {
            beaconsInVicinity.add(CBeacon(beacon.id2.toString(), beacon.distance))
        }
    }

    private fun content() {
        //var sum = content.averageMiss * content.counter
        //content.counter++
        //content.averageMiss = (sum + abs((content.distance - content.distanceToBeacon))) / content.counter
        //var temp = "Distance based on RSSI: ${content.distance}  \n Actual distance to beacon: ${content.distanceToBeacon} \n Average miss: ${content.averageMiss}  \n Seconds: ${content.counter} \n Rssi: ${content.rssi} \n UUID: ${content.UUID}"
        beaconsInVicinity.sortByDescending { it.distance }

        if (beaconsInVicinity.count() >= 3) {
            val mediaType = "application/json; charset=utf-8".toMediaType()
            var jsonString = """{
                    "id": "temp",
                        "distances": {
                            "${beaconsInVicinity[0].UUID}": ${beaconsInVicinity[0].distance},
                            "${beaconsInVicinity[1].UUID}": ${beaconsInVicinity[1].distance},
                            "${beaconsInVicinity[2].UUID}": ${beaconsInVicinity[2].distance}
                            }
                        }"""

            val client = OkHttpClient()
            val request = Request.Builder().url(URL).post(jsonString.toRequestBody(mediaType)).build()
            val response = client.newCall(request).execute()
        }

        var temp = ""

        for (beacon in beaconsInVicinity) {
            temp += beacon.UUID + " " + beacon.distance + "\n"
        }

        textView.text = temp
        beaconsInVicinity.clear()

        refresh(1000) //Refreshes the screen to update the values displayed on screen
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
}
