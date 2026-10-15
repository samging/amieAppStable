package org.example.project.auth

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class JwtTest {
    private val issuer = "http://keycloak.test/realms/master"

    @Test
    fun `decode reads the header and the claims the app shows`() {
        val jwt = Jwt.decode(TestTokens.accessToken(issuer, "sam"))
        assertEquals("RS256", jwt.alg)
        assertEquals(TestTokens.KID, jwt.kid)
        assertEquals(issuer, jwt.issuer)
        assertEquals("sam", jwt.username)
        assertEquals("sam@example.test", jwt.email)
        assertEquals("Sam Uel", jwt.name)
        assertEquals("uuid-sam", jwt.subject)
        assertTrue(jwt.expiresAt > 0)
    }

    @Test
    fun `verifySignature accepts the realm's key and refuses another key or algorithm`() {
        val jwks = Json { ignoreUnknownKeys = true }.decodeFromString<Jwks>(TestTokens.jwks())
        assertTrue(Jwt.decode(TestTokens.accessToken(issuer)).verifySignature(jwks))
        assertFalse(Jwt.decode(TestTokens.accessToken(issuer)).verifySignature(Jwks(emptyList())))
        val tampered = TestTokens.accessToken(issuer).split('.').let { it[0] + "." + it[1] + "." + TestTokens.accessToken(issuer, "tom").split('.')[2] }
        assertFalse(Jwt.decode(tampered).verifySignature(jwks))
    }

    @Test
    fun `a token without three parts is refused`() {
        assertFailsWith<IllegalArgumentException> { Jwt.decode("only.two") }
    }
}
