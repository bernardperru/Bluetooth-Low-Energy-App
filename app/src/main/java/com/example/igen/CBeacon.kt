package com.example.igen

import kotlin.math.pow


data class CBeacon (var UUID: String){
    var distance = 0.0
    var missedUpdates = 0

    fun computeDistance(averageRssi: Double) {
        distance = (10.0).pow((-54-(averageRssi))/(10 * 2))
    }

}



//Math.pow(10, ((-64 - AVERAGE_RSSI) / (10 * 2)))