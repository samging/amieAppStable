package org.example.project.backend

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import org.example.project.auth.AuthSession
import org.example.project.auth.AuthState
import java.io.File
import java.io.IOException
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * [AmieBackend] against a mock backend and site (the addresses come from the test task's environment):
 * what each call sends, how the answers and the failures come back to the Models page.
 */
class AmieBackendTest {
    private val json = headersOf(HttpHeaders.ContentType, "application/json")
    private val requests = mutableListOf<HttpRequestData>()

    private fun backend(handler: suspend MockRequestHandleScope.(HttpRequestData) -> HttpResponseData) {
        requests.clear()
        AmieBackend.http = AmieBackend.client(MockEngine { request -> requests += request; handler(request) })
    }

    @BeforeTest
    fun signIn() {
        // A session with a token that is not about to expire, so no refresh is attempted
        AuthState.signIn(AuthSession(accessToken = "access.token.x", refreshToken = null, expiresAt = Instant.now().epochSecond + 3600, username = "sam"))
    }

    @Test
    fun `me sends the bearer token and reads the profile`() = runTest {
        backend { request ->
            assertEquals("http://backend.test/me", request.url.toString())
            assertEquals("Bearer access.token.x", request.headers[HttpHeaders.Authorization])
            respond("""{"id":3,"username":"sam","fullName":"Sam","email":"s@x","bio":""}""", HttpStatusCode.OK, json)
        }
        val me = AmieBackend.me()
        assertEquals("sam", me.username)
        assertEquals(3, me.id)
    }

    @Test
    fun `a 401 and a 404 are explained with the backend's address`() = runTest {
        backend { respond("nope", HttpStatusCode.Unauthorized, json) }
        val unauthorized = assertFailsWith<BackendException> { AmieBackend.me() }
        assertEquals(401, unauthorized.status)
        assertTrue(unauthorized.message!!.contains("rejected the Keycloak token"), unauthorized.message)

        backend { respond("", HttpStatusCode.NotFound) }
        val missing = assertFailsWith<BackendException> { AmieBackend.me() }
        assertTrue(missing.message!!.contains("http://backend.test") && missing.message!!.contains("8090"), missing.message)
    }

    @Test
    fun `an unreachable backend is a BackendException without a status that names the config file`() = runTest {
        backend { throw IOException("Connection refused") }
        val e = assertFailsWith<BackendException> { AmieBackend.me() }
        assertNull(e.status)
        assertTrue(e.message!!.contains("backend.json") && e.message!!.contains("Connection refused"), e.message)
        val status = AmieBackend.status()
        assertFalse(status.reachable)
    }

    @Test
    fun `upload posts the file as multipart with its language and user, and reports a refusal`() = runTest {
        val file = File.createTempFile("echo", ".jar").apply { writeBytes(byteArrayOf(0x50, 0x4B, 1, 2)); deleteOnExit() }
        backend { request ->
            assertEquals(HttpMethod.Post, request.method)
            assertEquals("http://backend.test/upload", request.url.toString())
            val body = String(request.body.toByteArray(), Charsets.ISO_8859_1)
            assertTrue(Regex("name=\"?progLanguage\"?\\s+(Content-Length: \\d+\\s+)?jar").containsMatchIn(body), body)
            assertTrue(Regex("name=\"?username\"?\\s+(Content-Length: \\d+\\s+)?sam").containsMatchIn(body), body)
            assertTrue(body.contains("filename=\"${file.name}\""))
            respond("File uploaded successfully", HttpStatusCode.OK)
        }
        assertEquals("File uploaded successfully", AmieBackend.upload(file, "jar", "sam"))

        backend { respond("No class with run(String) and close()", HttpStatusCode.BadRequest) }
        val refused = assertFailsWith<BackendException> { AmieBackend.upload(file, "jar", "sam") }
        assertEquals(400, refused.status)
        assertTrue(refused.message!!.contains("No class with run"), refused.message)
    }

    @Test
    fun `publishJar refuses a file that is not a jar before any request`() = runTest {
        backend { respond("", HttpStatusCode.OK) }
        val notJar = File.createTempFile("model", ".onnx").apply { deleteOnExit() }
        assertFailsWith<BackendException> { AmieBackend.publishJar(notJar, "sam") }
        assertTrue(requests.isEmpty())
    }

    @Test
    fun `publishJar uploads, refreshes the site's listing and says whether the site lists the jar`() = runTest {
        val jar = File.createTempFile("echo", ".jar").apply { writeBytes(byteArrayOf(1)); deleteOnExit() }
        backend { request ->
            when {
                request.url.toString() == "http://backend.test/upload" -> respond("File uploaded successfully", HttpStatusCode.OK)
                request.url.toString().startsWith("http://site.test/api/jars") -> respond("[]", HttpStatusCode.OK, json)
                request.url.toString().startsWith("http://site.test/api/repo-by-username") -> {
                    assertTrue(request.url.toString().contains("username=sam") && request.url.toString().contains("refresh=1"))
                    respond("""[{"name":"${jar.name}","path":"uploads/sam/jar/${jar.name}","size":1,"type":"file"}]""", HttpStatusCode.OK, json)
                }
                else -> respond("", HttpStatusCode.NotFound)
            }
        }
        val published = AmieBackend.publishJar(jar, "sam")
        assertEquals(listOf(jar.name), published.files)
        assertEquals(true, published.onWebsite)
        assertEquals("http://site.test/dashboard?username=sam", published.dashboardUrl)
    }

    @Test
    fun `websiteUrl hands the tokens to the site and opens the sso page, or the plain page when the site cannot`() = runTest {
        backend { request ->
            assertEquals("http://site.test/api/sso/handoff", request.url.toString())
            val body = String(request.body.toByteArray())
            assertTrue(body.contains("\"access_token\":\"access.token.x\""), body)
            respond("""{"code":"one-time-code","expires_in":120}""", HttpStatusCode.OK, json)
        }
        assertEquals("http://site.test/sso?code=one-time-code&next=%2Fdashboard%3Fusername%3Dsam", AmieBackend.websiteUrl("/dashboard?username=sam"))

        backend { respond("down", HttpStatusCode.InternalServerError) }
        assertEquals("http://site.test/dashboard?username=sam", AmieBackend.websiteUrl("/dashboard?username=sam"))

        backend { throw IOException("Connection refused") }
        assertEquals("http://site.test/profile", AmieBackend.websiteUrl("profile"))
    }

    @Test
    fun `saved states are addressed by a valid name only`() = runTest {
        backend { request ->
            when (request.method) {
                HttpMethod.Put -> { assertEquals("http://backend.test/state/labels", request.url.toString()); respond("", HttpStatusCode.NoContent) }
                HttpMethod.Get -> respond("""{"a":1}""", HttpStatusCode.OK, json)
                HttpMethod.Delete -> respond("", HttpStatusCode.NotFound)
                else -> respond("", HttpStatusCode.MethodNotAllowed)
            }
        }
        AmieBackend.saveState("labels", """{"a":1}""")
        assertEquals("""{"a":1}""", AmieBackend.loadState("labels"))
        assertFalse(AmieBackend.deleteState("labels"))
        val before = requests.size
        assertFailsWith<BackendException> { AmieBackend.saveState("../etc/passwd", "{}") }
        assertEquals(before, requests.size, "an invalid name never reaches the backend")
    }

    @Test
    fun `reportError goes to the user's log with a token and to the login-error route as a guest, and never throws`() = runTest {
        backend { request ->
            assertEquals("http://backend.test/errors", request.url.toString())
            assertEquals("Bearer access.token.x", request.headers[HttpHeaders.Authorization])
            respond("", HttpStatusCode.NoContent)
        }
        AmieBackend.reportError("Script", "boom")
        assertEquals(1, requests.size)

        backend { throw IOException("down") }
        AmieBackend.reportError("Script", "boom again")   // swallowed
    }
}
