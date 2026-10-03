package org.example.project.data.remote.parser

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.io.files.FileNotFoundException
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json.Default.decodeFromString
import org.example.project.components.render.templates.Client
import org.example.project.components.render.templates.RestType
import org.example.project.components.render.templates.sendDeviceStatusDto
import org.example.project.components.terminal.window.controller.ResponseDto
import org.example.project.components.terminal.window.controller.logToConsole
import org.example.project.data.remote.DeviceDto
import org.example.project.data.remote.DeviceActions
import org.example.project.data.remote.DeviceRemoteService
import org.example.project.getDirectory
import org.example.project.util.sharedHttpClient
import java.time.Instant

@Serializable
data class PostResponse(val status: String)
enum class JSONACTIONS {
    WRITE, DELETE
}
@Serializable
data class PackageWrite(
    val deviceId: String,
    val pkgs: MutableList<String>
)
class DeviceManagerJson(
    private var configFile: File = File(getDirectory().toString(), "componentSettings.json"),
    private val deviceService: DeviceRemoteService = DeviceRemoteService(sharedHttpClient)
) : DeviceManager {

    override val configuredDevicesStatic: Map<String, Device>
        get() = loadStatic(configFile)

    companion object {
        private var currentUsername: String = ""
        private val logger = org.slf4j.LoggerFactory.getLogger(DeviceManagerJson::class.java)
        private val scope = CoroutineScope(Dispatchers.IO)

        private var configuredDevices by mutableStateOf<Map<String, Device>>(emptyMap())

        private val _postResult = MutableStateFlow<Result<PostResponse>?>(null)
        val postResult: StateFlow<Result<PostResponse>?> = _postResult.asStateFlow()

        private val _deviceMapState = MutableStateFlow<Map<String, DeviceDto>>(emptyMap())
        val deviceMapState: StateFlow<Map<String, DeviceDto>> = _deviceMapState.asStateFlow()

        @JvmStatic
        fun loadStatic(configFile: File = File(getDirectory().toString(), "componentSettings.json")): Map<String, Device> {
            if (!configFile.exists()) {
                println("[JSON-lib-load]: File not found ${configFile}")
                configuredDevices = emptyMap()
                return configuredDevices
            }

            try {
                val jsonContent = configFile.readText().trim()
                if (jsonContent.isEmpty()) {
                    println("[JSON-lib-load]: Empty file")
                    configuredDevices = emptyMap()
                    return configuredDevices
                }

                configuredDevices = Json.decodeFromString<Map<String, Device>>(jsonContent)
                println("[JSON-lib-load]: ${configuredDevices}")
            } catch (e: Exception) {
                println("[JSON-lib-load]: ${e.message}")
                configuredDevices = emptyMap()
            }
            return configuredDevices
        }
    }


    init {
        syncGet()
    }

    override fun load(file: File) {}
    private fun syncGet(
        client: Client = Client()
    ) {
        val effectiveUsername = currentUsername.ifEmpty { "guest-90574852af9bd745" }
        scope.launch {
            try {
                val initialBody = sendDeviceStatusDto(
                    action = DeviceActions.GET,
                    username = effectiveUsername,
                    deviceMap = emptyMap()
                )

                //var initialMap2 = client.rest<sendDeviceStatusDto>("get-device-status", restType = RestType.POST, body = initialBody)
                var initialMap3 = client.rest<DeviceDto>("get-device-status", restType = RestType.POST, body = initialBody)
                println("[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]")
                println("[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]")
                println("[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]")
                println("[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]_[=]")
                println("INITAL MAP3: $initialMap3")
                initialMap3 = initialMap3 ?: emptyMap()
                initialMap3 = initialMap3.mapNotNull { (key, value) ->
                    if (value != null) {
                        key to value
                    } else {
                        null
                    }
                }.toMap()

                var initialMap2 = emptyMap<String, DeviceDto>()
                if (!initialMap2.isNullOrEmpty()) {
                    println("initialMap2 is empty")
                    val toJsonObj = File(getDirectory().toString(), "componentSettings.json").readText()
                    println("1")
                    initialMap2 = Json.decodeFromString(toJsonObj)

                    println("HERE THE BUG: $initialMap2")
                    println("2")

                    val body = sendDeviceStatusDto(
                        action = DeviceActions.SET,
                        username = effectiveUsername,
                        deviceMap = initialMap2
                    )
                    println("4")
                    println("BODY: $body")
                    try {
                        println("5")
                        client.rest<Any>(
                            "update-device-status",
                            restType = RestType.POST,
                            body = body
                        )
                    } catch (e: Exception) {
                        println(":(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(")
                        println(":(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(")
                        println(":(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(:(")
                        logger.error(e.toString())
                    }
                }

                println(">>>>>>>>>>>>>>>>>>>>>1>>>>>>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>>>>>3>>>>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>>>>>>>4>>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>2>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>3>>>>>>>>>>>>>>>>>>>>>>>>>>")
                println("value of initMap3: ${initialMap3}")
                _deviceMapState.value = initialMap3

                try {
                    println("[JSON-WRITING-USER]: $initialMap3")

                    File(
                        getDirectory().toString(),
                        "componentSettings.json"
                    ).writeText(Json.encodeToString(initialMap3))

                    println("[JSON-WROTE-USER]")
                } catch (e: IllegalAccessError) {
                    println("[CATCH-JSON] ${e.message}")
                }
                println("Here it comes: $initialMap3")
                println("Here it comes [REST]: $initialMap3")
                println("[INIT TO]: configuredDevices: ${_deviceMapState.value}")
            } catch (e: Exception) {
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>")
                println("DEBUG: Fetch error: ${e.message}")
            }
        }
    }

    override fun setSession(username: String) {
        currentUsername = username
        syncGet()
    }

    val localScope = CoroutineScope(Dispatchers.IO)
    private inline fun <T> MutableStateFlow<T>.updateFlow(
        scope: CoroutineScope = localScope,
        client: Client = Client(),
    ) {
        var fileValues:String = ""
        try {
            fileValues = File(getDirectory().toString(), "componentSettings.json").readText()
        } catch (e: FileNotFoundException) {
            logger.error("Can't find file: ${e.message}")
        }

        val serializeDeviceDto = if(fileValues.isEmpty()) emptyMap<String, DeviceDto>() else Json.decodeFromString<Map<String, DeviceDto>>(fileValues)
        println("[Notice Of Update]: ${serializeDeviceDto}")

        val currentValue = this.value
        println("\uD83D\uDD34\uD83D\uDD34\uD83D\uDD34[STATEMAP] DEBUG: ${currentValue}")

        if (currentValue is Map<*, *>) {

            @Suppress("UNCHECKED_CAST")
            val mapValue = currentValue as Map<String, DeviceDto>

            val effectiveUsername = currentUsername.ifEmpty { "guest-90574852af9bd745" }
            val body = sendDeviceStatusDto(
                action = DeviceActions.SET,
                username = effectiveUsername,
                deviceMap = serializeDeviceDto
            )
            println("[>] DEBUG: Updating: ${body}")

            scope.launch {
                try {
                    client.rest<Any>("update-device-status", restType = RestType.POST, body = body)
                    _postResult.value = Result.success(PostResponse("Success"))
                } catch (e: Exception) {
                    _postResult.value = Result.failure(e)
                }
            }
        }
    }

    override fun load() {
        load(this.configFile)
    }

    override fun writePackage(id: String, pkgUrl: MutableList<String>, action: JSONACTIONS) {
        var jsonContent = ""
        var deviceMap: MutableMap<String, PackageWrite> = mutableMapOf()

        // 2. Read existing file content if it exists
        if (configFile.exists()) {
            jsonContent = configFile.readText().trim()
            if (jsonContent.isNotEmpty()) {
                try {
                    deviceMap = Json.decodeFromString(jsonContent)
                } catch (e: Exception) {
                    println("!!(JSON)[WRITE-PACKAGE] - Error parsing JSON: ${e.message}")
                }
            }
        }

        val toClass = PackageWrite(id, pkgUrl)
        val toStrJson = Json.encodeToString(toClass)
        println("(JSON)[Write-Package] Attempting to write: $toStrJson")

        if (!configFile.exists()) {
            println("!!(JSON)[WRITE-PACKAGE] - file not found for ${configFile.absolutePath}")
        } else {
            when (action) {
                JSONACTIONS.WRITE -> {
                    val packageEntry = deviceMap.getOrPut(id) { PackageWrite(id, mutableListOf()) }
                    for (url in pkgUrl) {
                        val cleanUrl = url.substringBefore("?token=")
                        if (!packageEntry.pkgs.any { it.substringBefore("?token=") == cleanUrl }) {
                            packageEntry.pkgs.add(cleanUrl)
                        }
                    }
                }

                JSONACTIONS.DELETE -> {
                    deviceMap[id]?.let { packageEntry ->
                        val cleanUrlsToRemove = pkgUrl.map { it.substringBefore("?token=") }
                        packageEntry.pkgs.removeIf { it.substringBefore("?token=") in cleanUrlsToRemove }
                        if (packageEntry.pkgs.isEmpty()) {
                            deviceMap.remove(id)
                        }
                    }
                }
            }

            try {
                val updatedJsonString = Json.encodeToString(deviceMap)
                configFile.writeText(updatedJsonString)
                println("(JSON)[WRITE-PACKAGE] - Updated packages for $id. Action: $action")
            } catch (e: Exception) {
                println("!!(JSON)[WRITE-PACKAGE] - Error writing JSON: ${e.message}")
            }
        }
    }
    override fun getPackages(id: String): List<String> {
        if (!configFile.exists()) return emptyList()
        return try {
            val jsonContent = configFile.readText().trim()
            if (jsonContent.isEmpty()) {
                println("!!(JSON)[GET-PACKAGES] - file empty")
                return emptyList()
            }

            val deviceMap: Map<String, PackageWrite> = Json.decodeFromString(jsonContent)
            println("(JSON) - deviceMap: $deviceMap")
            val allPkgs = deviceMap[id]?.pkgs ?: emptyList()
            println("ALL PKGS: $allPkgs")
            allPkgs
        } catch (e: Exception) {
            println("(JSON)[GET-PACKAGES] - Error parsing JSON: ${e.message}")
            emptyList()
        }
    }

    //proper usage must have generism:
    override fun writeConfig(id: String, keys: List<String>, values: List<String>, configFile: File) {
        val targetFile = configFile

        println("(JSON)[DEBUG]: WritingConfig called! ${configFile.name} ${configFile.absolutePath}")
        println("(JSON)[WRITE-CONFIG]: ${targetFile} ${targetFile.name} ${targetFile.absolutePath}")
        if (!targetFile.exists()) {
            targetFile.parentFile?.mkdirs()
            targetFile.createNewFile()
            targetFile.writeText("{}")
        }
        logger.info("id: $id, keys: $keys, values: $values")
        println("...")

        try {
            println("DEBUG: WritingConfig: 1")

            val jsonContent: String = targetFile.readText()

            val deviceMap: MutableMap<String, Device> = if (jsonContent.trim().isEmpty()) {
                println("JSON DEBUG1")
                mutableMapOf()
            } else {
                println("JSON DEBUG2")
                Json.decodeFromString<Map<String, Device>>(jsonContent).toMutableMap()
            }
            println("DEBUG: WritingConfig: 2")

            val existingDevice = deviceMap[id] ?: Device(name = "", port = "", deviceEndpoint = "")
            var updatedName = existingDevice.name
            var updatedPort = existingDevice.port
            var updatedEndpoint = existingDevice.deviceEndpoint

            println("DEBUG: WritingConfig: 3")
            for ((index, keyItem) in keys.withIndex()) {
                when (keyItem) {
                    "name" -> updatedName = values[index]
                    "port" -> updatedPort = values[index]
                    "deviceEndpoint" -> updatedEndpoint = values[index]
                }
            }

            deviceMap[id] = Device(name = updatedName, port = updatedPort, deviceEndpoint = updatedEndpoint)
            println("DEBUG: Device with ID Found: ${deviceMap[id]}")


            logger.warn("Sending delete for: (${id}): \n${_deviceMapState}")
            val updatedJsonContent = Json { prettyPrint = true }
                .encodeToString(deviceMap)

            println("DEBUG: WritingConfig: $updatedJsonContent")
            targetFile.writeText(updatedJsonContent)
            configuredDevices = deviceMap
            _deviceMapState.updateFlow()
        } catch (e: Exception) {
            println("Parser error: ${e.toString()}")
        }
    }

    override fun generateAddId(): String {
        if (!configFile.exists()) {
            return "1"
        }

        return try {
            val jsonContent: String = configFile.readText()

            if (jsonContent.trim().isEmpty()) {
                return "1"
            }

            val deviceMap: Map<String, Device> = Json.decodeFromString(jsonContent)
            val existingIds = deviceMap.keys.mapNotNull { it.toIntOrNull() }

            if (existingIds.isEmpty()) {
                "1"
            } else {
                (existingIds.maxOrNull()?.plus(1) ?: 1).toString()
            }
        } catch (e: Exception) {
            "1"
        }
    }

    fun getDevicePort(deviceId: String): String? {
        return configuredDevices[deviceId]?.port
    }

    override fun getDevice(deviceKey: String): Device? {
        return configuredDevices[deviceKey]
    }

    override fun count(): Int {
        return configuredDevices.size
    }

    override fun deleteById(id: String) {
        if (!configFile.exists()) return

        try {
            val jsonContent = configFile.readText()
            val deviceMap: MutableMap<String, Device> = Json.decodeFromString<Map<String, Device>>(jsonContent).toMutableMap()

            if (deviceMap.containsKey(id)) {
                deviceMap.remove(id)

                val updatedJson = Json.encodeToString(deviceMap)
                configFile.writeText(updatedJson)
                configuredDevices = deviceMap

                logger.warn("Sending delete for: (${id}): \n${_deviceMapState}")
                _deviceMapState.updateFlow()
            }
        } catch (e: Exception) {
            println("Failed to modify configuration: ${e.message}")
        }
    }

    override fun parseConfig(key: String): List<String> {
        if (!configFile.exists()) return emptyList()

        try {
            val jsonContent: String = configFile.readText()
            val deviceMap: Map<String, Device> = Json.decodeFromString(jsonContent)
            return deviceMap.values.map { device ->
                when (key) {
                    "name" -> device.name
                    "port" -> device.port
                    "deviceEndpoint" -> device.deviceEndpoint ?: ""
                    else -> ""
                }
            }
        } catch (e: Exception) {
            return emptyList()
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

    override fun getDevices(): Map<String, Device> {
        //val jsonString = File(configFile.toString()).toString()
        //val configuredDevices = Json.decodeFromString<Map<String, Device>>(jsonString)
        //println("DEBUG: ${configuredDevices}")
        println("[JSON-lib] DEBUG: ${configuredDevices}")
        return configuredDevices
    }

    fun getPlugins() {
        if (configFile.endsWith("corePlugins.json")) {
            val serialize = Json.decodeFromString<CorePluginObject>(configFile.readText())
            logToConsole(ResponseDto(time = Instant.now(), message = serialize.name))
        } else {
            logToConsole(ResponseDto(time = Instant.now(), message = "corePlugins.json not found"))
        }
    }

}
