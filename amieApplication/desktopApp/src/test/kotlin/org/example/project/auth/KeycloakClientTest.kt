package org.example.project.auth

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The desktop app's sign-in against a mock Keycloak: the password grant, what the token must satisfy
 * (issuer, expiry, signature against the realm's keys), and how failures are worded for the login page.
 */
class KeycloakClientTest {
    private val config = KeycloakConfig(baseUrl = "http://keycloak.test", realm = "master", clientId = "amie", clientSecret = "s3cret")
    private val issuer = config.issuer

    /** A Keycloak that answers discovery, the token endpoint through [token] and the JWKS */
    private fun keycloak(token: (Map<String, String>) -> Pair<HttpStatusCode, String>) = MockEngine { request ->
        val path = request.url.encodedPath
        val json = headersOf(HttpHeaders.ContentType, "application/json")
        when {
            path.endsWith("/.well-known/openid-configuration") -> respond("""{"issuer":"$issuer","token_endpoint":"$issuer/protocol/openid-connect/token"}""", HttpStatusCode.OK, json)
            path.endsWith("/protocol/openid-connect/certs") -> respond(TestTokens.jwks(), HttpStatusCode.OK, json)
            path.endsWith("/protocol/openid-connect/token") -> {
                // The client sends application/x-www-form-urlencoded, spaces as '+'
                val form = String(request.body.toByteArray()).split('&').filter { it.contains('=') }.associate {
                    java.net.URLDecoder.decode(it.substringBefore('='), Charsets.UTF_8) to java.net.URLDecoder.decode(it.substringAfter('='), Charsets.UTF_8)
                }
                val (status, body) = token(form)
                respond(body, status, json)
            }
            path.endsWith("/protocol/openid-connect/logout") -> respond("", HttpStatusCode.NoContent)
            else -> respond("not found", HttpStatusCode.NotFound)
        }
    }

    @Test
    fun `passwordLogin sends the password grant with the client secret and offline scope and builds the session from the token`() = runTest {
        var seen: Map<String, String> = emptyMap()
        val client = KeycloakClient(config, keycloak { form ->
            seen = form
            if (form["username"] == "sam" && form["password"] == "pw") HttpStatusCode.OK to TestTokens.tokenJson(TestTokens.accessToken(issuer, "sam"))
            else HttpStatusCode.Unauthorized to """{"error":"invalid_grant","error_description":"Invalid user credentials"}"""
        })

        val session = client.passwordLogin("sam", "pw")

        assertEquals("password", seen["grant_type"])
        assertEquals("amie", seen["client_id"])
        assertEquals("s3cret", seen["client_secret"])
        assertEquals(OFFLINE_SCOPE, seen["scope"])
        assertEquals("sam", session.username)
        assertEquals("sam@example.test", session.email)
        assertEquals("uuid-sam", session.subject)
        assertEquals(issuer, session.issuer)
        assertEquals("refresh.token.value", session.refreshToken)
        assertTrue(!session.expired(), "a fresh token is not expired")
    }

    @Test
    fun `a wrong password is worded for the login page and carries Keycloak's status`() = runTest {
        val client = KeycloakClient(config, keycloak { HttpStatusCode.Unauthorized to """{"error":"invalid_grant","error_description":"Invalid user credentials"}""" })
        val e = assertFailsWith<AuthException> { client.passwordLogin("sam", "nope") }
        assertEquals("Wrong username or password", e.message)
        assertEquals(401, e.status)
    }

    @Test
    fun `direct access grants off, a disabled account and a pending action each get their own sentence`() = runTest {
        suspend fun message(body: String): String =
            assertFailsWith<AuthException> { KeycloakClient(config, keycloak { HttpStatusCode.BadRequest to body }).passwordLogin("sam", "pw") }.message!!
        assertTrue(message("""{"error":"unauthorized_client","error_description":"Client not allowed for direct access grants"}""").contains("Direct access grants"))
        assertTrue(message("""{"error":"invalid_grant","error_description":"Account disabled"}""").contains("disabled"))
        assertTrue(message("""{"error":"invalid_grant","error_description":"Account is not fully set up"}""").contains("pending action"))
        assertTrue(message("""{"error":"invalid_client","error_description":"Invalid client credentials"}""").contains("Client 'amie' rejected"))
    }

    @Test
    fun `blank credentials never reach Keycloak`() = runTest {
        var called = false
        val client = KeycloakClient(config, keycloak { called = true; HttpStatusCode.OK to "{}" })
        assertFailsWith<AuthException> { client.passwordLogin("", "pw") }
        assertFailsWith<AuthException> { client.passwordLogin("sam", "   ") }
        assertTrue(!called)
    }

    @Test
    fun `a token from another issuer or signed with another key is refused`() = runTest {
        val otherIssuer = KeycloakClient(config, keycloak { HttpStatusCode.OK to TestTokens.tokenJson(TestTokens.accessToken("http://evil.test/realms/master")) })
        assertTrue(assertFailsWith<AuthException> { otherIssuer.passwordLogin("sam", "pw") }.message!!.contains("issuer"))

        val forged = TestTokens.accessToken(issuer).let { token ->
            // The same header and claims, the signature of some other token: must not verify against the realm's key
            val parts = token.split('.')
            parts[0] + "." + parts[1] + "." + TestTokens.accessToken(issuer, "tom").split('.')[2]
        }
        val badSignature = KeycloakClient(config, keycloak { HttpStatusCode.OK to TestTokens.tokenJson(forged) })
        assertTrue(assertFailsWith<AuthException> { badSignature.passwordLogin("sam", "pw") }.message!!.contains("signature"))

        val expired = KeycloakClient(config, keycloak { HttpStatusCode.OK to TestTokens.tokenJson(TestTokens.accessToken(issuer, expiresInSeconds = -60)) })
        assertTrue(assertFailsWith<AuthException> { expired.passwordLogin("sam", "pw") }.message!!.contains("expired"))
    }

    @Test
    fun `an unreachable Keycloak is an AuthException without a status, naming the address`() = runTest {
        val client = KeycloakClient(config, MockEngine { throw IOException("Connection refused") })
        val e = assertFailsWith<AuthException> { client.passwordLogin("sam", "pw") }
        assertNull(e.status)
        assertTrue(e.message!!.contains("http://keycloak.test"), e.message)
    }

    @Test
    fun `refresh and logout use the refresh token`() = runTest {
        val grants = mutableListOf<Map<String, String>>()
        val client = KeycloakClient(config, keycloak { form ->
            grants += form
            HttpStatusCode.OK to TestTokens.tokenJson(TestTokens.accessToken(issuer), refreshToken = "fresh.refresh.token")
        })
        val session = client.refresh("old.refresh.token")
        assertEquals("refresh_token", grants.last()["grant_type"])
        assertEquals("old.refresh.token", grants.last()["refresh_token"])
        assertEquals("fresh.refresh.token", session.refreshToken)
        client.logout(session.refreshToken!!)
    }
}
