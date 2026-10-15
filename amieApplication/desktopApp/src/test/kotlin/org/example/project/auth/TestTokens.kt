package org.example.project.auth

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.Signature
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import java.util.Base64

/** RS256 tokens signed with a key pair made for the test, and the JWKS document Keycloak would publish for it */
object TestTokens {
    const val KID = "test-key-1"
    val keys: KeyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()

    private fun b64(bytes: ByteArray) = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

    fun accessToken(issuer: String, username: String = "sam", expiresInSeconds: Long = 300, extra: JsonObject.() -> Unit = {}): String {
        val header = b64("""{"alg":"RS256","typ":"JWT","kid":"$KID"}""".toByteArray())
        val now = Instant.now().epochSecond
        val claims = buildJsonObject {
            put("iss", issuer)
            put("sub", "uuid-$username")
            put("preferred_username", username)
            put("email", "$username@example.test")
            put("name", "Sam Uel")
            put("exp", now + expiresInSeconds)
            put("iat", now)
        }.toString()
        val payload = b64(claims.toByteArray())
        val signature = Signature.getInstance("SHA256withRSA").run {
            initSign(keys.private)
            update("$header.$payload".toByteArray())
            sign()
        }
        return "$header.$payload.${b64(signature)}"
    }

    fun jwks(): String {
        val public = keys.public as RSAPublicKey
        fun unsigned(value: java.math.BigInteger) = value.toByteArray().let { if (it[0] == 0.toByte()) it.copyOfRange(1, it.size) else it }
        return """{"keys":[{"kid":"$KID","kty":"RSA","alg":"RS256","use":"sig","n":"${b64(unsigned(public.modulus))}","e":"${b64(unsigned(public.publicExponent))}"}]}"""
    }

    fun tokenJson(accessToken: String, refreshToken: String = "refresh.token.value", expiresIn: Long = 300): String =
        """{"access_token":"$accessToken","refresh_token":"$refreshToken","id_token":null,"expires_in":$expiresIn,"refresh_expires_in":1800,"token_type":"Bearer"}"""
}
