package org.example.project.backend

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.getDirectory
import java.io.File

/**
 * Where the Amie backend (Spring Boot `packageRepository`) and the Amie website (Kobweb site) are.
 *
 * Saved in `AMP/backend.json`; the environment variables `AMIE_BACKEND_URL`, `AMIE_SITE_URL` and `AMIE_S3_URL`
 * override the file. The defaults are the local setup of the Amie project (`run.sh`): the website on 8081, the
 * S3 service on 8088 and the backend on 8080, or 8090 on a machine where Keycloak holds 8080.
 */
@Serializable
data class BackendConfig(
    val baseUrl: String = "http://localhost:8080",
    val siteUrl: String = "http://localhost:8081",
    /** The S3 service that keeps the package jars; asked directly only when the website cannot serve one */
    val s3ServiceUrl: String = "http://localhost:8088",
) {
    val base: String get() = baseUrl.trimEnd('/')
    val site: String get() = siteUrl.trimEnd('/')
    val s3: String get() = s3ServiceUrl.trimEnd('/')

    /**
     * The website page that lists what [username] uploaded. The site's `/dashboard` shows nothing without
     * `?username=` (only a hint to pick an author), so the parameter is sent whenever the user is known.
     */
    fun siteDashboardUrl(username: String?): String =
        if (username.isNullOrBlank()) "$site/dashboard" else "$site/dashboard?username=${enc(username)}"

    /** The website's upload page (`/navigateto`), where a signed-in user publishes a package jar by hand */
    fun siteUploadUrl(username: String?): String =
        if (username.isNullOrBlank()) "$site/navigateto" else "$site/navigateto?username=${enc(username)}"

    val siteProfileUrl: String get() = "$site/profile"

    /** The site's own route listing one author's packages; `refresh=1` makes the site ask the backend again */
    fun siteUserPackagesUrl(username: String, refresh: Boolean = false): String =
        "$site/api/repo-by-username?username=${enc(username)}" + if (refresh) "&refresh=1" else ""

    private fun enc(value: String): String = java.net.URLEncoder.encode(value, Charsets.UTF_8)

    /** A backend URL for a path, or [slug] itself when it is already absolute */
    fun url(slug: String): String =
        if (slug.startsWith("http://") || slug.startsWith("https://")) slug else "$base/${slug.trimStart('/')}"

    companion object {
        private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
        val file: File get() = File(getDirectory().toString(), "backend.json")

        @Volatile private var cached: BackendConfig? = null

        /** The saved config with environment overrides applied; writes the defaults when there is no file yet */
        fun load(): BackendConfig {
            cached?.let { return it }
            val saved = runCatching { json.decodeFromString<BackendConfig>(file.readText()) }.getOrNull()
                ?: BackendConfig().also { defaults ->
                    runCatching {
                        file.parentFile?.mkdirs()
                        file.writeText(json.encodeToString(defaults))
                    }
                }
            fun env(name: String) = System.getenv(name)?.trim()?.takeIf { it.isNotEmpty() }
            return saved.copy(
                baseUrl = env("AMIE_BACKEND_URL") ?: saved.baseUrl,
                siteUrl = env("AMIE_SITE_URL") ?: saved.siteUrl,
                s3ServiceUrl = env("AMIE_S3_URL") ?: saved.s3ServiceUrl,
            ).also { cached = it }
        }

        fun save(config: BackendConfig) {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(config))
            cached = config
        }
    }
}
