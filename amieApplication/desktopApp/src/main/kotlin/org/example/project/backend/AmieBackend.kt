package org.example.project.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.example.project.auth.AuthState
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import java.io.File
import java.time.Instant

/** The backend's answer to `/me` and `/profile`: how it names the signed-in user (no password hash) */
@Serializable
data class BackendProfile(
    val id: Long = -1,
    val username: String = "",
    val fullName: String = "",
    val email: String = "",
    val bio: String = "",
)

/** One file the backend stored for a user in the GitHub package repository (`/user-packages`) */
@Serializable
data class BackendPackage(
    val name: String = "",
    val path: String = "",
    val size: Long = 0,
    val type: String = "file",
    val download_url: String? = null,
    val html_url: String? = null,
)

/** What the backend thinks of this app right now, shown on the Models page */
data class BackendStatus(
    val checkedAt: Instant,
    val reachable: Boolean,
    val profile: BackendProfile? = null,
    val packages: List<BackendPackage> = emptyList(),
    val problem: String? = null,
)

class BackendException(message: String, val status: Int? = null) : Exception(message)

/**
 * Typed calls to the Amie backend for the signed-in Keycloak user. Every call sends the Keycloak access
 * token; the backend (with its Keycloak verifier) maps it to the same username the website shows.
 */
object AmieBackend {
    val config: BackendConfig get() = BackendConfig.load()

    private val json = Json { ignoreUnknownKeys = true }
    private val http = HttpClient(Java) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000
            connectTimeoutMillis = 5_000
            socketTimeoutMillis = 120_000
        }
        expectSuccess = false
    }

    private suspend fun token(): String =
        AuthState.accessToken() ?: throw BackendException("Sign in with Keycloak first")

    /** Who the backend says the current token belongs to; creates the backend user on first call */
    suspend fun me(): BackendProfile {
        val response = http.get(config.url("me")) { bearerAuth(token()) }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        return json.decodeFromString(body)
    }

    /** The files the backend stored for [username] (what the website's dashboard lists) */
    suspend fun packages(username: String): List<BackendPackage> {
        val response = http.get(config.url("user-packages")) { parameter("username", username) }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        return runCatching { json.decodeFromString<List<BackendPackage>>(body) }.getOrDefault(emptyList())
    }

    /**
     * Uploads a file to the signed-in user's package folder (`uploads/<user>/<language>/<name>` in the
     * backend's GitHub repository) through `POST /upload`. The website's dashboard lists it afterwards.
     */
    suspend fun upload(file: File, language: String, username: String): String {
        if (!file.isFile) throw BackendException("${file.name} is not a file")
        val response = http.post(config.url("upload")) {
            bearerAuth(token())
            setBody(
                MultiPartFormDataContent(
                    formData {
                        append("progLanguage", language)
                        append("username", username)
                        append(
                            "file", file.readBytes(),
                            Headers.build {
                                append(HttpHeaders.ContentType, "application/octet-stream")
                                append(HttpHeaders.ContentDisposition, "filename=\"${file.name}\"")
                            }
                        )
                    }
                )
            )
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        // The backend answers 200 with "Error uploading file: ..." when GitHub refuses (for example no GITHUB_TOKEN)
        if (body.trimStart().startsWith("Error", ignoreCase = true)) throw BackendException("The backend couldn't store ${file.name}: ${body.take(200)}", response.status.value)
        logInternal("backend: uploaded ${file.name} (${file.length()} bytes) for $username as $language")
        return body
    }

    /** Publishes a trained module: its ONNX model and manifest, so it shows on the website under `onnx` */
    suspend fun publishModule(name: String, dir: File, username: String): List<String> {
        val files = listOf(File(dir, "module.json"), File(dir, "model.onnx")).filter { it.isFile }
        if (files.isEmpty()) throw BackendException("Module '$name' has no module.json or model.onnx in ${dir.path}")
        return files.map { upload(it, "onnx", username) }
    }

    /** One round trip that tells the Models page whether the backend is up and knows this user */
    suspend fun status(): BackendStatus {
        val now = Instant.now()
        return try {
            val profile = me()
            val packages = runCatching { packages(profile.username) }.getOrDefault(emptyList())
            BackendStatus(now, reachable = true, profile = profile, packages = packages)
        } catch (e: BackendException) {
            // 401/403 still mean the server answered
            BackendStatus(now, reachable = e.status != null, problem = e.message)
        } catch (e: Exception) {
            logInternal("backend: ${config.base} unreachable: ${e.message}", LogLevel.ERROR)
            BackendStatus(now, reachable = false, problem = e.message ?: e.javaClass.simpleName)
        }
    }

    private fun describe(status: HttpStatusCode, body: String): String = when (status.value) {
        401 -> "The backend at ${config.base} rejected the Keycloak token (401). It needs the Keycloak verifier and the realm issuer in its application.properties"
        403 -> "The backend refused this account (403)"
        404 -> "The backend at ${config.base} has no such endpoint (404); is it the Amie packageRepository?"
        else -> "The backend answered ${status.value}: ${body.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim().take(200)}"
    }
}
