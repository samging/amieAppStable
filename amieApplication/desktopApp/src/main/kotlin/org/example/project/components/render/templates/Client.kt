package org.example.project.components.render.templates

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.example.project.data.remote.DeviceActions
import org.example.project.data.remote.DeviceDto
import java.io.IOException
import java.net.ConnectException

enum class RestType {
    GET, POST, PUT
}

enum class FallbackTypes { ANY, FILE_ITEM_LIST }

@Serializable
data class sendDeviceStatusDto(
    val action: DeviceActions,
    val username: String,
    val deviceMap: Map<String, DeviceDto>
    )
@Serializable
data class FileItem(
    val name: String,
    val downloadUrl: String,
    val id: String?,
    val type: String
)

class Client {
    var isLoading by mutableStateOf(false)
    val logger = org.slf4j.LoggerFactory.getLogger(Client::class.java)
    var validateResponse by mutableStateOf(false)
    var dialogResponse by mutableStateOf(false)
    var fatalDialogResponse by mutableStateOf(false)
    var lastErrorMessage by mutableStateOf<String?>(null)
    val hostIp = "192.168.1.114"

    val client = HttpClient(Java) {
        install(HttpTimeout)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
        install(Logging) {
            level = LogLevel.ALL
        }
    }

    suspend inline fun<reified T> rest(
        vararg slug: String,
        restType: RestType,
        body: Any? = null,
        fallbackType: FallbackTypes = FallbackTypes.ANY
    ): Map<String, T?>? {
        isLoading = true
        val hostIp = "192.168.1.114"
        val slugPath = slug.joinToString("/")

        println("DEBUG: Testing internet connectivity via google.com...")
        try {
            client.get("https://www.google.com")
            println("DEBUG: Internet check successful")
        } catch (e: Exception) {
            println("DEBUG: Internet check FAILED: ${e.message}")
        }

        try {
            val testResponse = client.get("http://$hostIp:8080/list-disk")
            println("DEBUG: Local server check successful: ${testResponse.status}")
        } catch (e: Exception) {
            println("DEBUG: Local server check FAILED: ${e.message}")
        }

        println("DEBUG: Starting request for slug: $slugPath at http://$hostIp:8080/")

        try {
            val response: HttpResponse = when (restType) {
                RestType.GET -> {
                    client.get("http://$hostIp:8080/$slugPath") {
                        timeout {
                            requestTimeoutMillis = 15_000L
                            connectTimeoutMillis = 15_000L
                            socketTimeoutMillis = 15_000L
                        }
                        contentType(ContentType.Application.Json)
                    }
                }

                RestType.POST -> {
                    client.post("http://$hostIp:8080/$slugPath") {
                        timeout {
                            requestTimeoutMillis = 15_000L
                            connectTimeoutMillis = 15_000L
                            socketTimeoutMillis = 15_000L
                        }
                        contentType(ContentType.Application.Json)
                        body?.let { setBody(it) }
                    }
                }

                RestType.PUT -> {
                    client.put("http://$hostIp:8080/$slugPath") {
                        timeout {
                            requestTimeoutMillis = 15_000L
                            connectTimeoutMillis = 15_000L
                            socketTimeoutMillis = 15_000L
                        }
                        contentType(ContentType.Application.Json)
                        body?.let { setBody(it) }
                    }
                }
            }

            println("DEBUG: Received response with status: ${response.status}")

            if (!response.status.isSuccess()) {
                val errorBody = try {
                    response.body<String>()
                } catch (e: Exception) {
                    null
                }
                val errorMessage = when (response.status.value) {
                    401 -> "Unauthorized: Please check your credentials."
                    404 -> "Not Found: The requested service could not be found."
                    500 -> "Server Error: Something went wrong on the server."
                    else -> errorBody ?: "Error code: ${response.status}"
                }
                println("DEBUG: Request failed: $errorMessage")
                handleLoginError(errorMessage)
                return emptyMap()
            } else {
                println("DEBUG: status ${response.status}")
                validateResponse = true
                try {
                    return when (fallbackType) {
                        FallbackTypes.FILE_ITEM_LIST -> {
                            println("!!!!!!!!!!!")
                            logger.info("Running fetchList")
                            val list = fetchList<FileItem>(response)
                            list?.associate { item ->
                                val fileItem = item as FileItem
                                fileItem.name to (fileItem as T)
                            }
                        }
                        FallbackTypes.ANY -> {
                            println("EVALUATED ANY!!!!")
                            println("EVALUATED ANY!!!!")
                            println("EVALUATED ANY!!!!")
                            println("EVALUATED ANY!!!!")
                            println("something")
                            fetchMap<T>(response)
                        }
                        else -> {
                            logger.info("DEBUG: Running fetchMap")
                            println("EVALUATED WRONG!!!!")
                            println("EVALUATED WRONG!!!!")
                            println("EVALUATED WRONG!!!!")
                            println("EVALUATED WRONG!!!!")
                            fetchMap<T>(response)
                        }
                    }
                } catch (e: SerializationException) {
                    println("DEBUG: Serialization failed, but status was success")
                    mapOf("status" to "success")
                } catch (e: Exception) {
                    mapOf("status" to "success")
                }
            }
        } catch (e: HttpRequestTimeoutException) {
            handleLoginError("Request timed out: The server is taking too long to respond.")
            return emptyMap()
        } catch (e: ConnectException) {
            handleLoginError("Connection refused: Ensure the server is running at http://$hostIp:8080")
            return emptyMap()
        } catch (e: IOException) {
            handleLoginError("Network error: Please check your internet connection.")
            return emptyMap()
        } catch (e: Exception) {
            println("DEBUG: Exception during request: ${e.message}")
            e.printStackTrace()
            handleLoginError("Unexpected error: ${e.message}")
            return emptyMap()
        } finally {
            isLoading = false
            println("DEBUG: Request finished")
        }
        return emptyMap()
    }

    @PublishedApi internal suspend inline fun <reified T> fetchList(
        response: HttpResponse
    ): List<T>? {
        println("Iniside: fetchList")
        val res = runCatching {
            val jsonString = response.body<String>()
            Json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<T>()),
                jsonString
            )
        }.getOrNull()
        println("RESULT: ${res}")
        return res ?: emptyList<T>()
    }

    @PublishedApi
    internal suspend inline fun <reified T> fetchMap(
        response: HttpResponse
    ): Map<String, T?>? {
        println("Inside fetchMap")
        val res = runCatching {
            val jsonString = response.body<String>()
            Json.decodeFromString(
                kotlinx.serialization.builtins.MapSerializer(
                    kotlinx.serialization.serializer<String>(),
                    kotlinx.serialization.serializer<T?>()
                ),
                jsonString
            )
        }.getOrNull()
        println("RESULT: ${res}")
        return res ?: emptyMap<String, T>()
    }

    suspend fun handleLoginError(errorMessage: String?) {
        lastErrorMessage = errorMessage
        dialogResponse = true
        val hostIp = "192.168.1.114"

        try {
            val tweakResponse = client.post("http://$hostIp:8080/handle-login-error") {
                timeout {
                    requestTimeoutMillis = 9_000L
                    connectTimeoutMillis = 9_000L
                    socketTimeoutMillis = 9_000L
                }
                contentType(ContentType.Application.Json)
                setBody(mapOf("error" to (errorMessage ?: "Unknown"), "username" to "unknown_user"))
            }

            if (!tweakResponse.status.isSuccess()) {
                fatalDialogResponse = true
            }
        } catch (e: Exception) {
            println("Failed to log error to server: ${e.message}")
        }
    }
}
