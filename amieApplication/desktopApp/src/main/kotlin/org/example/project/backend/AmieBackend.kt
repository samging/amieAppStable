package org.example.project.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.java.Java
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.delete
import io.ktor.client.request.forms.MultiPartFormDataContent
import io.ktor.client.request.forms.formData
import io.ktor.client.request.get
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.example.project.auth.AuthState
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.slf4j.LoggerFactory
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

/** One file the backend stored for a user in the S3 bucket (`uploads/<user>/<lang>/<file>`, listed by `/user-packages`) */
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

/** One line of the user's error log on the backend (`GET /errors`): when the app reported what */
@Serializable
data class BackendError(val time: String = "", val error: String = "", val message: String = "")

class BackendException(message: String, val status: Int? = null, cause: Throwable? = null) : Exception(message, cause)

/**
 * Typed calls to the Amie backend for the signed-in Keycloak user. Every call sends the Keycloak access
 * token; the backend (with its Keycloak verifier) maps it to the same username the website shows.
 */
object AmieBackend {
    val config: BackendConfig get() = BackendConfig.load()

    private val json = Json { ignoreUnknownKeys = true }
    // Only for failures that must stay out of the console (reporting an error must not produce another one)
    private val logger = LoggerFactory.getLogger("AmieBackend")
    private val stateNamePattern = Regex("[A-Za-z0-9._-]{1,64}")
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

    /**
     * Runs one backend call and turns a connection failure (the JDK's "Host is down", "Connection refused",
     * an unknown host or a timeout) into a [BackendException] without a status, naming the address that was
     * tried and where to change it. Being signed in cannot help with these, so the message says what can.
     */
    private suspend fun <T> reach(what: String, block: suspend () -> T): T = try {
        block()
    } catch (e: BackendException) {
        throw e
    } catch (e: java.io.IOException) {
        throw BackendException(unreachable(what, e), cause = e)
    } catch (e: io.ktor.client.plugins.HttpRequestTimeoutException) {
        throw BackendException(unreachable(what, e), cause = e)
    }

    private fun unreachable(what: String, e: Exception): String {
        val reason = (e.message ?: e.javaClass.simpleName).substringAfterLast(": ").ifBlank { e.javaClass.simpleName }
        return "Couldn't $what: the backend at ${config.base} is unreachable ($reason). " +
            "Check the address in ${BackendConfig.file.path} (AMIE_BACKEND_URL overrides it); " +
            "the Amie backend listens on 8080, or 8090 when Keycloak holds 8080"
    }

    /** Who the backend says the current token belongs to; creates the backend user on first call */
    suspend fun me(): BackendProfile = reach("reach the backend") {
        val response = http.get(config.url("me")) { bearerAuth(token()) }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        json.decodeFromString(body)
    }

    /** The files the backend stored for [username] (what the website's dashboard lists) */
    suspend fun packages(username: String): List<BackendPackage> = reach("list the packages") {
        val response = http.get(config.url("user-packages")) { parameter("username", username) }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        runCatching { json.decodeFromString<List<BackendPackage>>(body) }.getOrDefault(emptyList())
    }

    /**
     * Uploads a file to the signed-in user's package folder (`uploads/<user>/<language>/<name>` in the
     * backend's S3 bucket) through `POST /upload`. The website's dashboard lists it afterwards.
     */
    suspend fun upload(file: File, language: String, username: String): String = reach("publish ${file.name}") {
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
        // The backend answers 200 "File uploaded successfully", 400 with the reasons a jar was refused and 502 when
        // the store is unreachable; an "Error ..." body on a 2xx is kept as a failure in case an older backend answers
        if (body.trimStart().startsWith("Error", ignoreCase = true)) throw BackendException("The backend couldn't store ${file.name}: ${body.take(200)}", response.status.value)
        logInternal("backend: uploaded ${file.name} (${file.length()} bytes) for $username as $language")
        body
    }

    /**
     * What publishing a module came to: the files the backend took, whether the website lists them on
     * `/dashboard?username=` (null when the site could not be asked) and that page's address.
     */
    data class Published(val files: List<String>, val onWebsite: Boolean?, val dashboardUrl: String)

    /**
     * Publishes a trained module: its ONNX model, manifest and guide, so it shows on the website under `onnx`.
     * Afterwards the website is told to forget its cached listing for [username] (as its own upload page does)
     * and asked whether it now lists the files, so the caller can say where they are, or that they are not there yet.
     */
    suspend fun publishModule(name: String, dir: File, username: String): Published {
        val files = listOf(File(dir, "module.json"), File(dir, "model.onnx"), File(dir, "guide.md")).filter { it.isFile }
        if (files.isEmpty()) throw BackendException("Module '$name' has no module.json or model.onnx in ${dir.path}")
        files.forEach { upload(it, "onnx", username) }
        val names = files.map { it.name }
        val listed = websiteLists(username, names)
        return Published(names, listed, config.siteDashboardUrl(username))
    }

    /**
     * Asks the website (not the backend) for [username]'s packages with its cache refreshed, and tells whether
     * every name in [files] is among them. Null when the site is down or answered something else: publishing
     * went through the backend already, so a silent website is reported, never treated as a failure.
     */
    suspend fun websiteLists(username: String, files: List<String>): Boolean? {
        val url = config.siteUserPackagesUrl(username, refresh = true)
        return try {
            val response = http.get(url)
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) {
                logInternal("website: $url answered ${response.status.value}", LogLevel.ERROR)
                return null
            }
            val listed = runCatching { json.decodeFromString<List<BackendPackage>>(body) }.getOrNull() ?: return null
            val names = listed.map { it.name }.toSet()
            // Site listings carry the stored name; a file may be listed by its full key as well
            files.all { f -> names.contains(f) || listed.any { it.path.endsWith("/$f") } }
        } catch (e: Exception) {
            logInternal("website: ${config.site} unreachable: ${e.message}", LogLevel.ERROR)
            null
        }
    }

    /** Removes a published module's files from the website again (`DELETE /upload`); returns how many were there */
    suspend fun unpublishModule(name: String, username: String): Int = reach("remove '$name' from the website") {
        var removed = 0
        listOf("module.json", "model.onnx", "guide.md").forEach { fileName ->
            val response = http.delete(config.url("upload")) {
                bearerAuth(token())
                parameter("filename", fileName)
                parameter("progLanguage", "onnx")
                parameter("username", username)
            }
            val body = response.bodyAsText()
            if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
            if (body.trim() == "removed") removed++
        }
        logInternal("backend: '$name' removed from the website for $username ($removed files)")
        // The website caches an author's listing for a minute; make it look again so the dashboard is current
        if (removed > 0) runCatching { http.get(config.siteUserPackagesUrl(username, refresh = true)) }
        removed
    }

    /**
     * Adds an entry to the signed-in user's error log (`POST /errors`; the website's dashboard shows the log under
     * "Errors from your app"). A guest's report goes to `POST /handle-login-error` as before. Never throws and
     * never writes to the console: a report that fails only reaches the debug log, so it cannot loop.
     */
    suspend fun reportError(kind: String, message: String) {
        try {
            val token = AuthState.accessToken()
            val response = if (token != null) {
                http.post(config.url("errors")) {
                    bearerAuth(token)
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { put("error", kind); put("message", message) }.toString())
                }
            } else {
                http.post(config.url("handle-login-error")) {
                    contentType(ContentType.Application.Json)
                    setBody(buildJsonObject { put("error", message); put("username", "guest") }.toString())
                }
            }
            if (!response.status.isSuccess()) logger.debug("error report '{}' answered {}", kind, response.status.value)
        } catch (e: Exception) {
            logger.debug("couldn't report error '{}': {}", kind, e.message)
        }
    }

    /** The signed-in user's error log, newest first (`GET /errors?username=`) */
    suspend fun errors(): List<BackendError> {
        val response = http.get(config.url("errors")) {
            bearerAuth(token())
            parameter("username", AuthState.user)
        }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        return runCatching { json.decodeFromString<List<BackendError>>(body) }.getOrDefault(emptyList())
    }

    /** `state/<name>` for a valid state name; the backend keeps it as `uploads/<user>/state/<name>.json` */
    private fun stateUrl(name: String): String {
        if (!stateNamePattern.matches(name)) throw BackendException("State name '$name' must match [A-Za-z0-9._-]{1,64}")
        return config.url("state/$name")
    }

    /** Names of the signed-in user's saved states (`GET /state`), for example `devices` */
    suspend fun listStates(): List<String> {
        val response = http.get(config.url("state")) { bearerAuth(token()) }
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        return runCatching { json.decodeFromString<List<String>>(body) }.getOrDefault(emptyList())
    }

    /** The saved state [name] as the JSON text the backend stores (`GET /state/{name}`); null when there is none */
    suspend fun loadState(name: String): String? {
        val response = http.get(stateUrl(name)) { bearerAuth(token()) }
        if (response.status == HttpStatusCode.NotFound) return null
        val body = response.bodyAsText()
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, body), response.status.value)
        return body
    }

    /** Stores [json] (any JSON document, at most 1 MiB) as the saved state [name] (`PUT /state/{name}`) */
    suspend fun saveState(name: String, json: String) {
        val response = http.put(stateUrl(name)) {
            bearerAuth(token())
            contentType(ContentType.Application.Json)
            setBody(json)
        }
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, response.bodyAsText()), response.status.value)
    }

    /** Removes the saved state [name] (`DELETE /state/{name}`); false when there was none */
    suspend fun deleteState(name: String): Boolean {
        val response = http.delete(stateUrl(name)) { bearerAuth(token()) }
        if (response.status == HttpStatusCode.NotFound) return false
        if (!response.status.isSuccess()) throw BackendException(describe(response.status, response.bodyAsText()), response.status.value)
        return true
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
        404 -> "The backend at ${config.base} has no such endpoint (404); is it the Amie packageRepository? When Keycloak holds 8080 the backend listens on 8090"
        else -> "The backend answered ${status.value}: ${body.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim().take(200)}"
    }
}
