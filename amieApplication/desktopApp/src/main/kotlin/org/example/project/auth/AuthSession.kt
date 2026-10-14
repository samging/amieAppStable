package org.example.project.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.example.project.components.terminal.window.controller.LogLevel
import org.example.project.components.terminal.window.controller.logInternal
import org.example.project.getDirectory
import java.io.File
import java.time.Instant

/** A signed-in Keycloak user: the tokens and the claims the UI shows */
@Serializable
data class AuthSession(
    val accessToken: String,
    val refreshToken: String? = null,
    val idToken: String? = null,
    // Epoch seconds
    val expiresAt: Long,
    val refreshExpiresAt: Long = 0,
    val subject: String = "",
    val username: String,
    val email: String = "",
    val name: String = "",
    // Issuer claim of the tokens, and the realm URL the app was configured with when they were issued
    val issuer: String = "",
    val realmUrl: String = "",
) {
    fun expired(leewaySeconds: Long = 30): Boolean = Instant.now().epochSecond + leewaySeconds >= expiresAt
    fun refreshExpired(): Boolean = refreshToken == null || (refreshExpiresAt > 0 && Instant.now().epochSecond >= refreshExpiresAt)
}

/**
 * The app's sign-in state. Pages read [session]; the login and register pages set it through [signIn].
 * The session is kept in AMP/session.json so a restart signs the same user back in with the refresh token.
 */
object AuthState {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }
    private val file: File get() = File(getDirectory().toString(), "session.json")

    private val _session = MutableStateFlow<AuthSession?>(null)
    val session: StateFlow<AuthSession?> = _session.asStateFlow()

    val user: String get() = _session.value?.username ?: ""
    val signedIn: Boolean get() = _session.value != null

    val client: KeycloakClient by lazy { KeycloakClient(KeycloakConfig.load()) }

    fun signIn(session: AuthSession) {
        _session.value = session
        runCatching {
            file.parentFile?.mkdirs()
            file.writeText(json.encodeToString(session))
        }.onFailure { logInternal("auth: couldn't save the session: ${it.message}", LogLevel.ERROR) }
        logInternal("auth: signed in as ${session.username} (${session.issuer})")
    }

    /** Brings back the saved session, refreshing it when the access token has expired */
    suspend fun restore(): AuthSession? {
        val saved = runCatching { json.decodeFromString<AuthSession>(file.readText()) }.getOrNull() ?: return null
        if (saved.realmUrl != client.config.issuer) {
            logInternal("auth: saved session is for ${saved.realmUrl}, not ${client.config.issuer}; signed out")
            forget()
            return null
        }
        if (!saved.expired()) {
            _session.value = saved
            logInternal("auth: restored session of ${saved.username}")
            return saved
        }
        return refresh(saved)
    }

    /** A fresh access token, refreshing when the current one is about to expire; null when signed out */
    suspend fun accessToken(): String? {
        val current = _session.value ?: return null
        if (!current.expired()) return current.accessToken
        return refresh(current)?.accessToken
    }

    private suspend fun refresh(session: AuthSession): AuthSession? {
        if (session.refreshExpired()) {
            logInternal("auth: session of ${session.username} expired, sign in again")
            forget()
            return null
        }
        return try {
            client.refresh(session.refreshToken!!).also { signIn(it) }
        } catch (e: Exception) {
            logInternal("auth: refresh failed: ${e.message}", LogLevel.ERROR)
            forget()
            null
        }
    }

    /** Ends the Keycloak session and forgets the local one */
    suspend fun signOut() {
        val current = _session.value
        forget()
        if (current?.refreshToken != null) {
            runCatching { client.logout(current.refreshToken) }
                .onFailure { logInternal("auth: logout at Keycloak failed: ${it.message}", LogLevel.ERROR) }
        }
        logInternal("auth: signed out")
    }

    private fun forget() {
        _session.value = null
        file.delete()
    }
}
