package com.example.igen

class InstanceVariables()
{
    var rssiBaseline: Int = -51
    var id: Int = 0
    var description: String = ""
    var xCord: String = ""
    var yCord: String = ""
    var timer: Int = 0
    var postCheck: Boolean = false
    var positionCheck: Boolean = false
    var autoCheck: Boolean = false
    var beaconsInVicinityMap = HashMap<String, CBeacon>()

    lateinit var positions: Positions

    var beaconNames: HashMap<String, String> = hashMapOf(
        "0x66617454794a" to "SSFA",
        "0x586c48524d50" to "HPD5",
        "0x476349345762" to "SAFA",
        "0x4a70484a6267" to "FUPR",
        "0x694269596936" to "FRZ5")


}