package com.example.igen

data class InstanceVariables(var rssiBaseline: Int, var id: Int, var description: String,
                             var xCord: String, var yCord: String, var timer: Int,
                             var postCheck: Boolean, var positionCheck: Boolean, var autoCheck: Boolean)
{
    var beaconsInVicinityMap = HashMap<String, CBeacon>()
    lateinit var positions: Positions
    var beaconNames: HashMap<String, String> = hashMapOf(
        "0x66617454794a" to "SSFA",
        "0x586c48524d50" to "HPD5",
        "0x476349345762" to "SAFA",
        "0x4a70484a6267" to "FUPR",
        "0x694269596936" to "FRZ5")


}