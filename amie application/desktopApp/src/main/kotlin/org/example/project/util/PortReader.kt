package org.example.project.util

import com.fazecast.jSerialComm.SerialPort

/**
 * Probes the system to scan, identify, and map connected hardware serial devices.
 * Desktop implementation using jSerialComm.
 */
fun readSerialPorts(): Map<Int, String> {
    val comPorts = SerialPort.getCommPorts()
    
    if (comPorts.isEmpty()) {
        return mapOf(0 to "No Serial Ports Detected")
    }

    val portMap = mutableMapOf<Int, String>()

    for ((index, port) in comPorts.withIndex()) {
        val displayName = "${port.systemPortName} - ${port.descriptivePortName}"
        portMap[index] = displayName
    }

    return portMap
}
