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
import io.ktor.client.plugins.timeout
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.example.project.auth.AuthState
import org.example.project.backend.BackendConfig
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

/**
 * The app's client for the Amie backend (Spring Boot `packageRepository`).
 *
 * - The backend address comes from [BackendConfig] (`AMP/backend.json` or `AMIE_BACKEND_URL`).
 * - Every request carries the Keycloak access token of the signed-in user as a bearer token, refreshed when
 *   needed, so the backend endpoints that check `Authorization` (`/dashboard`, `/me`, `/profile`, `/upload`,
 *   `/edit`) accept the app the same way they accept the website. Guests send no token.
 * - Failures end up in [lastErrorMessage] and the dialog flags; a request that reached the server but was
 *   refused is also reported to the backend's `/handle-login-error`.
 */
class Client(val config: BackendConfig = BackendConfig.load()) {
    var isLoading by mutableStateOf(false)
    val logger = org.slf4j.LoggerFactory.getLogger(Client::class.java)
    var validateResponse by mutableStateOf(false)
    var dialogResponse by mutableStateOf(false)
    var fatalDialogResponse by mutableStateOf(false)
    var lastErrorMessage by mutableStateOf<String?>(null)
    val hostIp: String get() = config.base

    val client = HttpClient(Java) {
        install(HttpTimeout)
        install(ContentNegotiation) {
            json(Json { ignoreUnknownKeys = true })
        }
    }

    /** The signed-in user's token for the Authorization header, or null for a guest */
    @PublishedApi
    internal suspend fun bearer(): String? = runCatching { AuthState.accessToken() }.getOrNull()

    @PublishedApi
    internal suspend fun HttpRequestBuilder.prepare(body: Any?) {
        timeout {
            requestTimeoutMillis = 15_000L
            connectTimeoutMillis = 15_000L
            socketTimeoutMillis = 15_000L
        }
        contentType(ContentType.Application.Json)
        bearer()?.let { bearerAuth(it) }
        body?.let { setBody(it) }
    }

    /**
     * Calls the backend at `slug` (joined with `/`; an absolute URL is used as is) and decodes the JSON answer
     * as a map, or as a list of [FileItem] keyed by name for [FallbackTypes.FILE_ITEM_LIST]. On any failure
     * the error is recorded and an empty map comes back.
     */
    suspend inline fun <reified T> rest(
        vararg slug: String,
        restType: RestType,
        body: Any? = null,
        fallbackType: FallbackTypes = FallbackTypes.ANY
    ): Map<String, T?>? {
        isLoading = true
        lastErrorMessage = null
        val url = config.url(slug.joinToString("/"))
        logger.debug("{} {}", restType, url)

        try {
            val response: HttpResponse = when (restType) {
                RestType.GET -> client.get(url) { prepare(null) }
                RestType.POST -> client.post(url) { prepare(body) }
                RestType.PUT -> client.put(url) { prepare(body) }
            }

            if (!response.status.isSuccess()) {
                val errorBody = runCatching { response.bodyAsText() }.getOrNull()?.take(300)
                val errorMessage = when (response.status.value) {
                    401 -> "Unauthorized: sign in again, the backend rejected the token."
                    403 -> "Forbidden: the backend doesn't allow this for your account."
                    404 -> "Not Found: the backend has no $url."
                    500 -> "Server Error: something went wrong on the server."
                    else -> errorBody?.takeIf { it.isNotBlank() } ?: "Error code: ${response.status}"
                }
                logger.warn("{} {} failed: {} {}", restType, url, response.status, errorBody ?: "")
                handleLoginError(errorMessage, report = true)
                return emptyMap()
            }

            validateResponse = true
            return try {
                when (fallbackType) {
                    FallbackTypes.FILE_ITEM_LIST -> {
                        val items: List<FileItem> = fetchList<FileItem>(response) ?: emptyList()
                        val byName: Map<String, T?> = items.associate { it.name to (it as T?) }
                        byName
                    }
                    FallbackTypes.ANY -> fetchMap<T>(response)
                }
            } catch (e: SerializationException) {
                logger.warn("{} answered {} but the body didn't decode: {}", url, response.status, e.message)
                mapOf("status" to ("success" as? T))
            } catch (e: Exception) {
                logger.warn("{} answered {} but the body didn't decode: {}", url, response.status, e.message)
                mapOf("status" to ("success" as? T))
            }
        } catch (e: HttpRequestTimeoutException) {
            handleLoginError("Request timed out: the backend at ${config.base} is taking too long to respond.")
        } catch (e: ConnectException) {
            handleLoginError("Connection refused: ensure the Amie backend is running at ${config.base} (AMP/backend.json).")
        } catch (e: IOException) {
            handleLoginError("Network error: ${e.message ?: "couldn't reach ${config.base}"}")
        } catch (e: Exception) {
            logger.error("{} {} threw", restType, url, e)
            handleLoginError("Unexpected error: ${e.message}")
        } finally {
            isLoading = false
        }
        return emptyMap()
    }

    @PublishedApi
    internal suspend inline fun <reified T> fetchList(response: HttpResponse): List<T>? {
        val jsonString = response.body<String>()
        return Json { ignoreUnknownKeys = true }.decodeFromString(
            kotlinx.serialization.builtins.ListSerializer(kotlinx.serialization.serializer<T>()),
            jsonString
        )
    }

    @PublishedApi
    internal suspend inline fun <reified T> fetchMap(response: HttpResponse): Map<String, T?>? {
        val jsonString = response.body<String>()
        return Json { ignoreUnknownKeys = true }.decodeFromString(
            kotlinx.serialization.builtins.MapSerializer(
                kotlinx.serialization.serializer<String>(),
                kotlinx.serialization.serializer<T?>()
            ),
            jsonString
        )
    }

    /**
     * Records a failure for the UI. With [report] the backend's `/handle-login-error` is told as well, which
     * only makes sense when the server answered (not when it couldn't be reached at all).
     */
    suspend fun handleLoginError(errorMessage: String?, report: Boolean = false) {
        lastErrorMessage = errorMessage
        dialogResponse = true
        if (!report) return
        try {
            val username = AuthState.user.ifEmpty { "guest" }
            val tweakResponse = client.post(config.url("handle-login-error")) {
                timeout {
                    requestTimeoutMillis = 9_000L
                    connectTimeoutMillis = 9_000L
                    socketTimeoutMillis = 9_000L
                }
                contentType(ContentType.Application.Json)
                setBody(mapOf("error" to (errorMessage ?: "Unknown"), "username" to username))
            }
            if (!tweakResponse.status.isSuccess()) {
                fatalDialogResponse = true
            }
        } catch (e: Exception) {
            logger.debug("couldn't report the error to the backend: {}", e.message)
        }
    }
}
