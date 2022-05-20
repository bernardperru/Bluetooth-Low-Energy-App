package com.example.igen

import java.util.*
import kotlin.math.pow
import kotlin.math.*

//Beacon class
data class CBeacon (val UUID: String){
    var distance = 0.0
    var missedUpdates = 0
    var averageRssi = 0.0
    var queue: Queue<Int> = LinkedList<Int>()
    var timer = 20

    fun computeDistance(rssiBaseLine: Int) {
        computeAverageRSSI()
        distance = (10.0).pow((rssiBaseLine-(averageRssi))/(10 * 2))
    }

    //Add rssi value to the queue, and removes one or more values if necessary
    fun addRssiValue(rssi: Int) {
        if (queue.size == timer) {
            queue.remove()
            queue.add(rssi)
        }
        else if(queue.size > timer) {
            removeExcessValues()
        }
        else{
            queue.add(rssi)
        }
    }

    //If the queue size is changed and there are too many values in the queue they are removed
    private fun removeExcessValues() {
        while (queue.size > timer) {
            queue.remove()
        }
    }

    //Removes outliers and computes average rssi
    private fun computeAverageRSSI() {
        val rssiValuesCut = removeOutliers()
        var sum = 0.0

        for (rssi: Int in rssiValuesCut) {
            sum += rssi
        }
        averageRssi = sum/queue.size
    }

    //Removes top and bottom 10% of measurements
    private fun removeOutliers(): MutableList<Int> {
        val temp = queue.toList().sortedBy { v -> v }
        val startIndex = floor(temp.size * 0.1).toInt()
        val endIndex = ceil(temp.size * 0.9).toInt() - 1
        var rssiValuesCut: MutableList<Int> = mutableListOf()

        for (i in startIndex..endIndex) {
            rssiValuesCut.add(temp[i])
        }

        return rssiValuesCut
    }



}
