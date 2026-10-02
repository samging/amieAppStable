package org.example.project.data.remote.parser

import java.io.File

interface DeviceManager {
    val configuredDevicesStatic: Map<String, Device>
    fun load(configFile: File)
    fun load()
    fun count(): Int
    fun getDevice(deviceKey: String): Device?
    fun getDevices(): Map<String, Device>
    fun generateAddId(): String
    fun parseConfig(key: String): List<String>
    fun parseConfigByTargetId(key: String, id: String): List<String>
    fun writeConfig(id: String, keys: List<String>, values: List<String>, configFile: File)
    fun deleteById(id: String)
    fun writePackage(id: String, pkgUrl: MutableList<String>, action: JSONACTIONS)
    fun getPackages(id: String): List<String>
    fun setSession(username: String)
}
