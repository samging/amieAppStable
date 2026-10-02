package org.example.project.data.remote.parser

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import org.example.project.getDirectory
import java.io.File

class DeviceManagerCsv(private var configFile: File = File("componentSettings.csv")) : DeviceManager {
    
    private var configuredDevices by mutableStateOf<Map<String, Device>>(emptyMap())

    override val configuredDevicesStatic: Map<String, Device>
        get() = emptyMap()

    override fun load(configFile: File) {
        this.configFile = configFile
        if (!configFile.exists()) {
            configuredDevices = emptyMap()
            return
        }

        try {
            val lines = configFile.readLines()
            val map = mutableMapOf<String, Device>()
            for (line in lines) {
                if (line.isBlank()) continue
                val parts = line.split(",")
                if (parts.size >= 3) {
                    val id = parts[0].trim()
                    val name = parts[1].trim()
                    val port = parts[2].trim()
                    val endpoint = if (parts.size > 3) {
                        parts[3].trim().takeIf { it.isNotEmpty() && it != "null" }
                    } else null
                    
                    map[id] = Device(name, port, endpoint)
                }
            }
            configuredDevices = map
        } catch (e: Exception) {
            configuredDevices = emptyMap()
        }
    }

    override fun load() {
        load(this.configFile)
    }

    override fun count(): Int = configuredDevices.size

    override fun getDevice(deviceKey: String): Device? = configuredDevices[deviceKey]

    override fun getDevices(): Map<String, Device> = configuredDevices

    override fun generateAddId(): String {
        if (!configFile.exists() || configuredDevices.isEmpty()) return "1"
        val existingIds = configuredDevices.keys.mapNotNull { it.toIntOrNull() }
        return if (existingIds.isEmpty()) "1" else (existingIds.max() + 1).toString()
    }

    override fun parseConfig(key: String): List<String> {
        return configuredDevices.values.map { device ->
            when (key) {
                "name" -> device.name
                "port" -> device.port
                "deviceEndpoint" -> device.deviceEndpoint ?: ""
                else -> ""
            }
        }
    }

    override fun parseConfigByTargetId(key: String, id: String): List<String> {
        val device = configuredDevices[id] ?: return emptyList()
        return listOf(
            when (key) {
                "name" -> device.name
                "port" -> device.port
                "deviceEndpoint" -> device.deviceEndpoint ?: ""
                else -> ""
            }
        )
    }

    override fun deleteById(id: String) {
        val mutableMap = configuredDevices.toMutableMap()
        if (mutableMap.containsKey(id)) {
            mutableMap.remove(id)
            configuredDevices = mutableMap
            saveToDisk()
        }
    }

    override fun setSession(username: String) {
    }
    override fun writePackage(id: String, pkgUrl: MutableList<String>, action: JSONACTIONS) {}

    override fun getPackages(id: String): List<String> = emptyList()

    override fun writeConfig(id: String, keys: List<String>, values: List<String>, configFile: File) {
        val mutableMap = configuredDevices.toMutableMap()
        val existingDevice = mutableMap[id] ?: Device("", "")
        
        var updatedName = existingDevice.name
        var updatedPort = existingDevice.port
        var updatedEndpoint = existingDevice.deviceEndpoint

        keys.forEachIndexed { index, key ->
            when (key) {
                "name" -> updatedName = values[index]
                "port" -> updatedPort = values[index]
                "deviceEndpoint" -> updatedEndpoint = values[index]
            }
        }

        mutableMap[id] = Device(updatedName, updatedPort, updatedEndpoint)
        configuredDevices = mutableMap
        
        this.configFile = configFile
        saveToDisk()
    }

    private fun saveToDisk() {
        try {
            val csvContent = configuredDevices.map { (id, device) ->
                "$id,${device.name},${device.port},${device.deviceEndpoint ?: ""}"
            }.joinToString("\n")
            configFile.writeText(csvContent)
        } catch (e: Exception) {
            println("Failed to save CSV: ${e.message}")
        }
    }
}
