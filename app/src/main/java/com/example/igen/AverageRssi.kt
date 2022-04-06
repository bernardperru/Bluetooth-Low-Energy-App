package com.example.igen

import java.util.Queue

class AverageRssi(id: String) {
    var rssiValues = ArrayDeque<Int>(60)
    var averageRssi: Double = 0.0

    fun addRssiValue(rssi: Int){
        if (rssiValues.count() < 60) {
            rssiValues.add(rssi)
        }
    }

    fun getAverage(): Double{
        if (rssiValues.count() == 60) {
            return (sumRssiValues() / 60).toDouble()
        }
        return 0.0
    }

    fun sumRssiValues(): Int{
        var sum = 0
        for (rssi in rssiValues) {
            sum += rssi
        }

        return sum
    }

}

//var sum = content.averageMiss * content.counter
//content.counter++
//content.averageMiss = (sum + abs((content.distance - content.distanceToBeacon))) / content.counter
//var temp = "Distance based on RSSI: ${content.distance}
// \n Actual distance to beacon: ${content.distanceToBeacon} \n Average miss: ${content.averageMiss}
// \n Seconds: ${content.counter} \n Rssi: ${content.rssi} \n UUID: ${content.UUID}"