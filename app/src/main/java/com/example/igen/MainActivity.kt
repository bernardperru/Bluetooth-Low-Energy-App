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
import org.altbeacon.beacon.*
import org.altbeacon.beacon.service.BeaconService.TAG
import org.altbeacon.beacon.service.RunningAverageRssiFilter
import kotlin.collections.HashMap


class MainActivity : AppCompatActivity(), View.OnClickListener {
    private val PERMISSION_REQUEST_FINE_LOCATION = 1
    private val PERMISSION_REQUEST_BACKGROUND_LOCATION = 2
    private val api = Api()
    private var instanceVariables = InstanceVariables(-51, 0, "", "", "", 0, false, false, false)

    lateinit var textView: TextView
    lateinit var textView1: TextView
    lateinit var textViewEditBaseline: TextView
    lateinit var textViewEditDescription: TextView
    lateinit var textViewEditId: TextView
    lateinit var textViewEditX: TextView
    lateinit var textViewEditY: TextView
    lateinit var textViewEditTimer: TextView
    lateinit var textViewEditAuto: TextView
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
        textViewEditBaseline = findViewById(R.id.editTextAvgRssi)
        textViewEditDescription = findViewById(R.id.editTextDescription)
        textViewEditId = findViewById(R.id.editTextId)
        textViewEditX = findViewById(R.id.editTextXCord)
        textViewEditY = findViewById(R.id.editTextYCord)
        textViewEditTimer = findViewById(R.id.editTextTimer)
        textViewEditAuto = findViewById(R.id.editTextAuto)
        //Button are linked to a click listener which is implemented in onClick()
        button.setOnClickListener(this)
        button1.setOnClickListener(this)
        button2.setOnClickListener(this)

       //Checks whether the application has the required permissions
        checkForPermissions()

        //Starts looking for beacons
        startRanging()

        content()
    }

    private fun initPhoneBeacon() {
        val phoneBeacon = Beacon.Builder()
                        .setId1("1")
                        .setId2(instanceVariables.id.toString())
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
        for ((key, beacon) in instanceVariables.beaconsInVicinityMap){
            beacon.missedUpdates += 1
        }

        for (beacon: Beacon in beacons){
            if (!instanceVariables.beaconsInVicinityMap.containsKey(beacon.id2.toString())){
                instanceVariables.beaconsInVicinityMap[beacon.id2.toString()] = CBeacon(beacon.id2.toString())
            }

            instanceVariables.beaconsInVicinityMap[beacon.id2.toString()]?.addRssiValue(beacon.rssi)
            instanceVariables.beaconsInVicinityMap[beacon.id2.toString()]?.computeDistance(instanceVariables.rssiBaseline)
            instanceVariables.beaconsInVicinityMap[beacon.id2.toString()]?.missedUpdates = 0
        }

    }

    private fun content() {

        textView.text = printBeaconInformation()

        refresh(2000) //Refreshes the screen to update the values displayed
    }

    private fun printBeaconInformation(): CharSequence? {
        var out = ""

        for ((key, beacon) in instanceVariables.beaconsInVicinityMap){
            if(beacon.UUID.length > 6){
                out += "${beacon.UUID.substring(0,6)} " +
                        "(${instanceVariables.beaconNames.get(beacon.UUID)}) " +
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

    private fun refresh(milliseconds: Int) {
        val handler = Handler()
        val runnable = Runnable { content() }
        handler.postDelayed(runnable, milliseconds.toLong())
    }

    private fun refreshPostRequest(milliseconds: Int, counter: Int) {
        val handler = Handler()
        val runnable = Runnable { autoPostRequest(counter) }
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

    override fun onClick(p0: View?) {
        try {
            if (p0!!.id == R.id.button) { //updateValue button
                if (textViewEditBaseline.text.toString() != "Baseline") {
                    instanceVariables.rssiBaseline = textViewEditBaseline.text.toString().toInt()
                }
                if (textViewEditId.text.toString() != "Id") {
                    instanceVariables.id = textViewEditId.text.toString().toInt()
                }
                if (textViewEditDescription.text.toString() != "Description") {
                    instanceVariables.description = textViewEditDescription.text.toString()
                }
                if (textViewEditAuto.text.toString() == "1") {
                    instanceVariables.autoCheck = true
                }

                instanceVariables.xCord = textViewEditX.text.toString()
                instanceVariables.yCord = textViewEditY.text.toString()
                instanceVariables.timer = textViewEditTimer.text.toString().toInt()

                updateBeaconTimers()
            } else if (p0.id == R.id.button1) { //init phone-beacon button
                if ((instanceVariables.xCord != "" && instanceVariables.yCord != "") || instanceVariables.positionCheck) {
                    initPhoneBeacon()
                    CoroutineScope(Dispatchers.IO).launch {
                        textView1.text = api.postPhoneBeacon(
                            instanceVariables.positions,
                            instanceVariables.id,
                            instanceVariables.description,
                            instanceVariables.xCord,
                            instanceVariables.yCord,
                            instanceVariables.postCheck
                        )
                    }
                    button1.text = "Running"
                } else {
                    button1.text = "No coords"
                }
            } else if (p0.id == R.id.button2) { //post request button
                if (instanceVariables.autoCheck) {
                    autoPostRequest(0)
                    button2.text = "auto posting"
                    instanceVariables.autoCheck = false
                } else {
                    CoroutineScope(Dispatchers.IO).launch {
                        val response = api.postRequest(
                            instanceVariables.beaconsInVicinityMap,
                            instanceVariables.id
                        )
                        instanceVariables.positions =
                            Gson().fromJson(response, Positions::class.java)
                        textView1.text = response
                        button1.text = "Init Phone Beacon"
                        button2.text = "Send distances"
                        //instanceVariables.postCheck = true
                        instanceVariables.positionCheck = true

                    }
                }

            }
        }    catch (e: Exception) { }
    }

    private fun updateBeaconTimers() {
        for (beacon: CBeacon in instanceVariables.beaconsInVicinityMap.values) {
            beacon.timer = instanceVariables.timer
        }
    }

    private fun autoPostRequest(counter: Int) {
        var c = counter
        if (c < 50) {
            CoroutineScope(Dispatchers.IO).launch {
                textView1.text = api.postRequestAuto(instanceVariables.beaconsInVicinityMap, instanceVariables.id)
            }
            c += 1
            refreshPostRequest(20000, c)
        }
        else {
            button2.text = "Done"
        }

    }
}
