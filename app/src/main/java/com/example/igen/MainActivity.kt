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
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.BeaconService.TAG
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import java.util.*
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val PERMISSION_REQUEST_FINE_LOCATION = 1
    private val PERMISSION_REQUEST_BACKGROUND_LOCATION = 2
    var beaconsInVicinity = mutableListOf<CBeacon>()
    val content = Content()
    var uniqueID = UUID.randomUUID().toString()
    val URL = "http://130.225.57.152/api/smartphone"
    var position = ""
    var averageRssi = AverageRssi("0x4a70484a6267")
    lateinit var textView: TextView
    lateinit var textView1: TextView
    lateinit var button: Button
    lateinit var button2: Button
    lateinit var button1: Button
    var BeaconNames = HashMap<String, String>()


    var beaconsInVicinityMap = HashMap<String, CBeacon>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        button = findViewById(R.id.button)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        textView = findViewById(R.id.text_view)
        textView1 = findViewById(R.id.textView1)
        button.setOnClickListener(this)
        button1.setOnClickListener(this)
        button2.setOnClickListener(this)

        //Set up Beacon names
        BeaconNames.put("0x66617454794a", "SSFA")
        BeaconNames.put("0x586c48524d50", "HPD5")
        BeaconNames.put("0x476349345762", "SAFA")
        BeaconNames.put("0x4a70484a6267", "FUPR")

        checkForPermissions()

        startRanging()

        //initPhoneBeacon()

        content()
    }

    private fun initPhoneBeacon() {
        var phoneBeacon = Beacon.Builder()
                        .setId1("1")
                        .setId2("2")
                        .setManufacturer(0x0118).setTxPower(-12).build()

        var beaconParser = BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19")

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

    private val rangingObserver = Observer<Collection<Beacon>> { beacons ->
        for ((key, beacon) in beaconsInVicinityMap){
            beacon.missedUpdates += 1
        }

        for (beacon: Beacon in beacons){
            if (!beaconsInVicinityMap.containsKey(beacon.id2.toString())){
                beaconsInVicinityMap.put(beacon.id2.toString(), CBeacon(beacon.id2.toString()))
            }

            beaconsInVicinityMap[beacon.id2.toString()]?.computeDistance(beacon.runningAverageRssi)
            beaconsInVicinityMap[beacon.id2.toString()]?.missedUpdates = 0
        }

    }

    private fun content() {

        textView.text = printBeaconInformation()

        refresh(5000) //Refreshes the screen to update the values displayed
    }

    private fun printBeaconInformation(): CharSequence? {
        var out = ""

        for ((key, beacon) in beaconsInVicinityMap){
            if(beacon.UUID.length > 6){
                out += "${beacon.UUID.substring(0,6)} " +
                        "(${BeaconNames.get(beacon.UUID)}) " +
                        "- \t${String.format("%.2f",beacon.distance)} m " +
                        "(${beacon.missedUpdates})\n"
            }
            else{
                out += "${beacon.UUID} is having a problem\n"
            }
        }

        return out
    }


    private fun postRequest(): String? {
        try {
            var beaconDistances = HashMap<String, Double>()
            val mediaType = "application/json; charset=utf-8".toMediaType()

            for (value in beaconsInVicinityMap.values) {
                beaconDistances[value.UUID] = value.distance
            }

            var jsonString = """{
            "id": "Yann",
                "distances": ${Gson().toJson(beaconDistances)}
                }"""

            val client = OkHttpClient()
            val request = Request.Builder().url(URL).post(jsonString.toRequestBody(mediaType)).build()
            val response = client.newCall(request).execute()

            return response.body!!.string()

        } catch (e: Exception) { return e.toString()}
        return "Fejl"
    }

    private fun getRequest(): String? {
        try {
            val client = OkHttpClient()
            val request = Request.Builder().url("http://130.225.57.152/api/smartphone/Yann").build()
            val response = client.newCall(request).execute()

            return response.body!!.string()
        } catch (e: Exception) {
            e.toString()
        }

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
        BeaconManager.setRssiFilterImplClass(RunningAverageRssiFilter::class.java)
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(20000L)
        val region = Region("all-beacons-region", null, null, null)

        beaconManager.getRegionViewModel(region).rangedBeacons.observe(this, rangingObserver)
        beaconManager.startRangingBeacons(region)
    }

    override fun onClick(p0: View?) {
        if (p0!!.id == R.id.button){
            textView1.text = printBeaconInformation()
        }
        else if(p0.id == R.id.button1) {
            initPhoneBeacon()
            button1.text = "phone-beacon :)"
        }
        else if(p0.id == R.id.button2){
            button2.text = "Sent!"
            CoroutineScope(Dispatchers.IO).launch {
                textView1.text = postRequest()
                button2.text = "Send distances"
        }
        }

    }

}
