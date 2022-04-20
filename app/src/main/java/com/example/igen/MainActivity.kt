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
import kotlinx.coroutines.delay
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
    private val urlPostDistances = "http://130.225.57.152/api/smartphone"
    private val urlPostBeacon = "http://130.225.57.152/api/beacon"
    private var uniqueID = UUID.randomUUID().toString()
    private var rssiBaseline = -51
    private var beaconNames = HashMap<String, String>()
    private var beaconsInVicinityMap = HashMap<String, CBeacon>()
    private lateinit var positions: Positions
    lateinit var textView: TextView
    lateinit var textView1: TextView
    lateinit var textViewEdit: TextView
    lateinit var button: Button
    lateinit var button1: Button
    lateinit var button2: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Assign TextView and Button variables to their corresponding xml elements
        button = findViewById(R.id.button)
        button1 = findViewById(R.id.button1)
        button2 = findViewById(R.id.button2)
        textView = findViewById(R.id.text_view)
        textView1 = findViewById(R.id.textView1)
        textViewEdit = findViewById(R.id.editTextAvgRssi)
        //Button are linked to a click listener which is implemented in onClick()
        button.setOnClickListener(this)
        button1.setOnClickListener(this)
        button2.setOnClickListener(this)

        //Set up Beacon names
        beaconNames.put("0x66617454794a", "SSFA")
        beaconNames.put("0x586c48524d50", "HPD5")
        beaconNames.put("0x476349345762", "SAFA")
        beaconNames.put("0x4a70484a6267", "FUPR")

        //Checks whether the application has the required permissions
        checkForPermissions()

        //Starts looking for beacons
        startRanging()

        content()
    }

    private fun initPhoneBeacon() {
        val phoneBeacon = Beacon.Builder()
                        .setId1("1")
                        .setId2("2")
                        .setManufacturer(0x0118).setTxPower(4).build()

        val beaconParser = BeaconParser().setBeaconLayout("s:0-1=feaa,m:2-2=00,p:3-3:-41,i:4-13,i:14-19")

        val beaconTransmitter = BeaconTransmitter(applicationContext, beaconParser)

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
                beaconsInVicinityMap[beacon.id2.toString()] = CBeacon(beacon.id2.toString())
            }

            beaconsInVicinityMap[beacon.id2.toString()]?.computeDistance(beacon.runningAverageRssi, rssiBaseline)
            beaconsInVicinityMap[beacon.id2.toString()]?.missedUpdates = 0
            beaconsInVicinityMap[beacon.id2.toString()]?.averageRssi = beacon.runningAverageRssi
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
                        "(${beaconNames.get(beacon.UUID)}) " +
                        "- \t${String.format("%.2f",beacon.distance)} m " +
                        "(${beacon.missedUpdates}) " +
                        "rssi: ${beacon.averageRssi.toInt()}\n"
            }
            else{
                out += "${beacon.UUID} is having a problem\n"
            }
        }

        return out
    }

    private fun postRequest(): String? {
        try {
            val beaconDistances = HashMap<String, Double>()
            val mediaType = "application/json; charset=utf-8".toMediaType()

            for (value in beaconsInVicinityMap.values) {
                beaconDistances[value.UUID] = value.distance
            }

            val sortedMap = beaconDistances.toList().sortedBy { (k,v) -> v }.toMap()

            val jsonString = """{
            "id": "Yann",
                "distances": ${Gson().toJson(sortedMap)}
                }"""

            val client = OkHttpClient()
            val request = Request.Builder().url(urlPostDistances).post(jsonString.toRequestBody(mediaType)).build()
            val response = client.newCall(request).execute()

            return response.body!!.string()

        } catch (e: Exception) { return e.toString()}
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
        RunningAverageRssiFilter.setSampleExpirationMilliseconds(60000L)
        val region = Region("all-beacons-region", null, null, null)

        beaconManager.getRegionViewModel(region).rangedBeacons.observe(this, rangingObserver)
        beaconManager.startRangingBeacons(region)
    }

    private fun postPhoneBeacon() {
            try {
                val mediaType = "application/json; charset=utf-8".toMediaType()
                val jsonString = """{
                                      "id": "12345",
                                      "position": {
                                        "x": ${positions.oldPosition.x},
                                        "y": ${positions.oldPosition.y}
                                      } 
                                    }"""

                val client = OkHttpClient()
                val request = Request.Builder().url(urlPostBeacon).post(jsonString.toRequestBody(mediaType)).build()
                val response = client.newCall(request).execute()

                textView1.text = response.body!!.string()

                //putPhoneBeacon()
            } catch (e: Exception) {}
    }

    private fun putPhoneBeacon() {
        try {
            Thread.sleep(60000L)
            val mediaType = "application/json; charset=utf-8".toMediaType()
            val jsonString = """{
                                      "id": "12345",
                                      "position": {
                                        "x": ${positions.oldPosition.x},
                                        "y": ${positions.oldPosition.y}
                                      } 
                                    }"""

            val client = OkHttpClient()
            val request = Request.Builder().url("$urlPostBeacon/12345").put(jsonString.toRequestBody(mediaType)).build()
            val response = client.newCall(request).execute()

            textView1.text = response.body!!.string()

            putPhoneBeacon()
        } catch (e: Exception) {}
    }

    override fun onClick(p0: View?) {
        if (p0!!.id == R.id.button){ //set avg button
            rssiBaseline = textViewEdit.text.toString().toInt()
        }
        else if(p0.id == R.id.button1) { //init phone-beacon button
            initPhoneBeacon()
            CoroutineScope(Dispatchers.IO).launch { postPhoneBeacon() }
            button1.text = "Phone Beacon Running"
        }
        else if(p0.id == R.id.button2){ //post request button
            button2.text = "Sent!"
            CoroutineScope(Dispatchers.IO).launch {
                val funky = postRequest()
                positions = Gson().fromJson(funky, Positions::class.java)
                textView1.text = positions.oldPosition.x.toString()
                button1.text = "Init Phone Beacon"
                button2.text = "Send distances"
            }
        }

    }

}
