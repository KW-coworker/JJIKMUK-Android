package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import org.junit.*
import org.junit.Assert.*

class AccountResourcesHttpTest {
    private val server = MockWebServer()
    @Before fun start() { server.start() }
    @After fun stop() { server.close() }
    private val repository get() = HttpAuthRepository.create(server.url("/"))
    private fun enqueue(body: String, status: Int = 200) = server.enqueue(MockResponse.Builder().code(status).body(body).build())
    private val family = """{"id":41,"nickname":"가족","allergies":"milk","specialDiet":"lowSugar","version":2}"""
    private val login = """{"token":"access","tokenType":"Bearer","userId":1,"email":"u@example.com","expiresAt":"2030-01-01T00:15:00Z","expiresInSeconds":900,"profileCompleted":true,"refreshToken":"refresh","refreshExpiresAt":"2030-01-31T00:00:00Z","authProvider":"LOCAL_AND_GOOGLE","hasPassword":true,"isNewUser":false}"""

    @Test fun `photo streams binary without using a public URL and upload uses file multipart`() = runBlocking {
        val bytes = byteArrayOf(1, 2, 3, 4)
        server.enqueue(MockResponse.Builder().addHeader("Content-Type", "image/png").addHeader("Cache-Control", "no-store").body(Buffer().write(bytes)).build())
        val photo = (repository.photo("access") as AuthResult.Success).value
        assertArrayEquals(bytes, photo.bytes)
        val get = server.takeRequest()
        assertEquals("/api/users/me/photo", get.url.encodedPath); assertEquals("Bearer access", get.headers["Authorization"])
        enqueue("""{"data":{"photoUrl":"/api/users/me/photo","authenticationRequired":true}}""")
        assertTrue(repository.uploadPhoto("access", photo) is AuthResult.Success)
        val upload = server.takeRequest()
        assertEquals("PUT", upload.method)
        assertTrue(upload.headers["Content-Type"]!!.startsWith("multipart/form-data; boundary="))
        val body = upload.body!!.utf8()
        assertTrue(body.contains("name=\"file\"")); assertTrue(body.contains("Content-Type: image/png"))
        assertFalse(body.contains("photoUrl"))
    }

    @Test fun `photo errors oversized binary and foreign receipt never become success`() = runBlocking {
        enqueue("""{"code":"PHOTO_NOT_FOUND"}""", 404)
        assertEquals("PHOTO_NOT_FOUND", (repository.photo("access") as AuthResult.Failure).error.code)
        server.enqueue(MockResponse.Builder().addHeader("Content-Type", "image/png").body("x".repeat(ProfilePhoto.MAX_BYTES + 1)).build())
        assertEquals(AuthFailureKind.InvalidResponse, (repository.photo("access") as AuthResult.Failure).error.kind)
        enqueue("""{"data":{"photoUrl":"https://other.test/photo","authenticationRequired":true}}""")
        assertTrue(repository.uploadPhoto("access", ProfilePhoto(byteArrayOf(1), "image/png")) is AuthResult.Failure)
        val before = server.requestCount
        assertTrue(repository.uploadPhoto("access", ProfilePhoto(ByteArray(0), "image/png")) is AuthResult.Failure)
        assertTrue(repository.uploadPhoto("access", ProfilePhoto(byteArrayOf(1), "image/gif")) is AuthResult.Failure)
        assertEquals(before, server.requestCount)
    }

    @Test fun `photo non-image response is rejected`() = runBlocking {
        enqueue("{}")
        assertTrue(repository.photo("access") is AuthResult.Failure)
    }

    @Test fun `all DELETE requests remain one-shot even on retry-after zero`() = runBlocking {
        repeat(3) { server.enqueue(MockResponse.Builder().code(503).addHeader("Retry-After", "0").body("{}").build()) }
        assertTrue(repository.deletePhoto("access") is AuthResult.Failure)
        assertTrue(repository.deleteFamily("access", 41, 2) is AuthResult.Failure)
        assertTrue(repository.deleteAccount("access", 1) is AuthResult.Failure)
        assertEquals(3, server.requestCount)
        repeat(3) { assertEquals("DELETE", server.takeRequest().method) }
    }

    @Test fun `family CRUD preserves IDs version and explicit null without adding owner`() = runBlocking {
        enqueue("""{"data":[$family]}""")
        assertEquals(41L, (repository.families("access") as AuthResult.Success).value.single().id)
        server.takeRequest()
        enqueue("""{"data":$family}""")
        assertTrue(repository.family("access", 41) is AuthResult.Success); server.takeRequest()
        enqueue("""{"data":$family}""", 201)
        assertTrue(repository.createFamily("access", ProfileSubmission("가족", "milk", "lowSugar")) is AuthResult.Success)
        val create = JsonParser.parseString(server.takeRequest().body!!.utf8()).asJsonObject
        assertFalse(create.has("ownerId")); assertFalse(create.has("version")); assertFalse(create.has("email"))
        enqueue("""{"data":$family}""")
        assertTrue(repository.patchFamily("access", 41, 2, ProfilePatch(allergies = ProfileField.Clear)) is AuthResult.Success)
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        val payload = JsonParser.parseString(patch.body!!.utf8()).asJsonObject
        assertEquals(2, payload["version"].asInt); assertTrue(payload["allergies"].isJsonNull)
        assertFalse(payload.has("specialDiet")); assertFalse(payload.has("nickname"))
        enqueue("""{"message":"deleted"}""")
        assertTrue(repository.deleteFamily("access", 41, 2) is AuthResult.Success)
        val delete = server.takeRequest()
        assertEquals("2", delete.url.queryParameter("version")); assertEquals(0L, delete.bodySize)
    }

    @Test fun `family conflict and mismatched IDs retain error instead of overwriting`() = runBlocking {
        enqueue("""{"code":"PROFILE_VERSION_CONFLICT"}""", 409)
        assertEquals("PROFILE_VERSION_CONFLICT", (repository.patchFamily("access", 41, 0, ProfilePatch(nickname = "가족")) as AuthResult.Failure).error.code)
        enqueue("""{"data":$family}""")
        assertTrue(repository.family("access", 42) is AuthResult.Failure)
        enqueue("""{"data":[$family,$family]}""")
        assertTrue(repository.families("access") is AuthResult.Failure)
    }

    @Test fun `Google link required is not a login and all proof endpoints omit bearer`() = runBlocking {
        enqueue("""{"code":"GOOGLE_LINK_REQUIRED","loginGranted":false,"linkToken":"proof","expiresAt":"2030-01-01T00:05:00Z","expiresInSeconds":300}""")
        val result = (repository.google("google-id") as AuthResult.Success).value
        assertTrue(result is GoogleAuthResult.LinkRequired)
        val google = server.takeRequest()
        assertNull(google.headers["Authorization"])
        assertEquals("google-id", JsonParser.parseString(google.body!!.utf8()).asJsonObject["idToken"].asString)
        enqueue(login)
        assertTrue(repository.linkGoogle("proof", "Password123") is AuthResult.Success)
        val link = server.takeRequest()
        assertNull(link.headers["Authorization"])
        assertEquals("/api/auth/google/link", link.url.encodedPath)
        enqueue("""{"message":"cancelled"}""")
        assertTrue(repository.cancelGoogleLink("proof") is AuthResult.Success)
        assertNull(server.takeRequest().headers["Authorization"])
    }

    @Test fun `Google malformed success and link proof mixed with access are rejected`() = runBlocking {
        enqueue("{}")
        assertTrue(repository.google("id") is AuthResult.Failure)
        enqueue("""{"code":"GOOGLE_LINK_REQUIRED","loginGranted":false,"token":"access","linkToken":"proof","expiresAt":"2030-01-01T00:05:00Z","expiresInSeconds":300}""")
        assertTrue(repository.google("id") is AuthResult.Failure)
    }

    @Test fun `profile completion requires completed server profile and no fabricated local flag`() = runBlocking {
        val profile = """{"data":{"id":1,"email":"u@example.com","nickname":"이름","role":"USER","authProvider":"GOOGLE","hasPassword":false,"profileCompleted":false}}"""
        enqueue(profile)
        assertTrue(repository.completeProfile("access", ProfileSubmission("이름")) is AuthResult.Failure)
        enqueue(profile.replace("\"profileCompleted\":false", "\"profileCompleted\":true"))
        assertTrue(repository.completeProfile("access", ProfileSubmission("이름")) is AuthResult.Success)
        val req = server.takeRequest()
        assertEquals("/api/users/me/profile/complete", req.url.encodedPath)
        assertFalse(JsonParser.parseString(req.body!!.utf8()).asJsonObject.has("profileCompleted"))
    }

    @Test fun `password change sends current and new values and keeps exact field errors`() = runBlocking {
        enqueue("""{"code":"PASSWORD_INVALID","field":"password"}""", 400)
        val result = repository.changePassword("access", 1, "old", "new") as AuthResult.Failure
        assertEquals("password", result.error.field)
        val req = server.takeRequest()
        assertEquals("PUT", req.method); assertEquals("/api/users/1/password", req.url.encodedPath)
        assertEquals("Bearer access", req.headers["Authorization"])
        val body = JsonParser.parseString(req.body!!.utf8()).asJsonObject
        assertEquals("old", body["currentPassword"].asString); assertEquals("new", body["newPassword"].asString)
        enqueue("""{"message":"deleted","data":null}""")
        assertTrue(repository.deleteAccount("access", 1) is AuthResult.Success)
        assertEquals("/api/users/1", server.takeRequest().url.encodedPath)
    }
}
