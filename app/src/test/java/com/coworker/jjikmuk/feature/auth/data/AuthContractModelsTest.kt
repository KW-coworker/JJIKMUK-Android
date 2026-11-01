package com.coworker.jjikmuk.feature.auth.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.google.gson.JsonParser
import com.google.gson.JsonObject
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import okio.Buffer

class AuthContractModelsTest {
    private fun model(json: String = loginJson): LoginResponse = authJson().fromJson(json, LoginResponse::class.java)
    private fun google(json: String) = AuthResponseMapper.google(JsonParser.parseString(json).asJsonObject)
    private fun invalid(action: () -> Unit) { assertThrows(Exception::class.java, action) }

    @Test fun `new login retains refresh expiry provider and password capability without printing secrets`() {
        val login = AuthResponseMapper.login(model())
        assertEquals("refresh-secret", login.refresh!!.token)
        assertEquals(AuthApiTime.parseMillis("2026-11-22T12:00:00Z"), login.refresh.expiresAtMillis)
        assertEquals(AuthProvider.LOCAL, login.authProvider)
        assertEquals(true, login.hasPassword)
        assertEquals(false, login.isNewUser)
        assertEquals(900, login.expiresInSeconds)
        assertEquals(AuthApiTime.parseMillis("2026-10-23T12:15:00Z"), login.expiresAtMillis)
        assertFalse(login.toString().contains("access-secret"))
        assertFalse(AuthResult.Success(login).toString().contains("refresh-secret"))
        assertFalse(login.refresh.toString().contains("refresh-secret"))
    }

    @Test fun `wholly legacy responses remain access only but partial new responses are rejected`() {
        val legacy = JsonParser.parseString(loginJson).asJsonObject
        listOf("refreshToken", "refreshExpiresAt", "authProvider", "hasPassword").forEach(legacy::remove)
        assertNull(AuthResponseMapper.login(model(legacy.toString())).refresh)
        invalid { AuthResponseMapper.login(model(legacy.toString()), requireRefresh = true) }
        legacy.addProperty("refreshToken", "refresh-secret")
        invalid { AuthResponseMapper.login(model(legacy.toString())) }
    }

    @Test fun `invalid expiry provider capability or missing session fields never become valid new login`() {
        for ((field, value) in listOf("expiresAt" to "2026-10-23T12:00:00", "refreshExpiresAt" to "bad",
            "authProvider" to "UNKNOWN", "token" to "")) {
            val json = JsonParser.parseString(loginJson).asJsonObject.apply { addProperty(field, value) }
            invalid { AuthResponseMapper.login(model(json.toString())) }
        }
        for (field in listOf("refreshToken", "refreshExpiresAt", "authProvider", "hasPassword", "isNewUser", "profileCompleted")) {
            val json = JsonParser.parseString(loginJson).asJsonObject.apply { remove(field) }
            invalid { AuthResponseMapper.login(model(json.toString())) }
        }
        invalid { AuthResponseMapper.login(model(loginJson.replace("\"LOCAL\"", "\"GOOGLE\""))) }
    }

    @Test fun `google 200 link-required is a proof not a service login`() {
        val result = google(linkJson)
        assertTrue(result is GoogleAuthResult.LinkRequired)
        val proof = result as GoogleAuthResult.LinkRequired
        assertEquals("link-secret", proof.linkToken)
        assertEquals(300, proof.expiresInSeconds)
        assertFalse(proof.toString().contains("link-secret"))
        invalid { AuthResponseMapper.login(model(linkJson)) }
    }

    @Test fun `google direct success and linked success use full session model`() {
        for (provider in listOf("GOOGLE", "LOCAL_AND_GOOGLE")) {
            val json = JsonParser.parseString(loginJson).asJsonObject.apply {
                addProperty("authProvider", provider)
                addProperty("hasPassword", provider != "GOOGLE")
                addProperty("profileCompleted", false)
            }
            val result = google(json.toString()) as GoogleAuthResult.SignedIn
            assertFalse(result.login.profileCompleted)
            assertNotNull(result.login.refresh)
        }
    }

    @Test fun `google mixed granted and proof responses or missing proof are rejected`() {
        for (field in listOf("linkToken", "expiresAt", "expiresInSeconds", "loginGranted")) {
            val json = JsonParser.parseString(linkJson).asJsonObject.apply { remove(field) }
            invalid { google(json.toString()) }
        }
        val mixed = JsonParser.parseString(linkJson).asJsonObject.apply { addProperty("token", "access-secret") }
        invalid { google(mixed.toString()) }
        invalid { google(linkJson.replace("false", "true")) }
    }

    @Test fun `offset timestamps denote same instant and fractional milliseconds are preserved`() {
        assertEquals(AuthApiTime.parseMillis("2026-10-23T12:00:00Z"),
            AuthApiTime.parseMillis("2026-10-23T21:00:00+09:00"))
        assertEquals(AuthApiTime.parseMillis("2026-10-23T12:00:00.123456789Z"),
            AuthApiTime.parseMillis("2026-10-23T08:00:00.123-04:00"))
        assertEquals(1L, AuthApiTime.parseMillis("1970-01-01T00:00:00.001Z"))
        for (bad in listOf("2026-10-23T21:00:00", "2026-02-30T12:00:00Z", "2026-10-23T25:00:00Z",
            "2026-10-23T12:00:00+19:00", "2026-10-23T12:00:00+09:60", "2026-10-23T12:00:00Zjunk")) {
            invalid { AuthApiTime.parseMillis(bad) }
        }
    }

    @Test fun `error fields are optional independent and never include server message`() {
        val error = AuthErrorMapper.decode(429,
            """{"code":"OTP_RATE_LIMITED","field":null,"retryAfterSeconds":42,"remainingAttempts":0,"message":"secret"}""",
            "45", 0)
        assertEquals(42L, error.retryAfterSeconds)
        assertEquals(45L, error.retryAfterHeaderSeconds)
        assertEquals(45L, error.retryDelaySeconds)
        assertEquals(0, error.remainingAttempts)
        assertFalse(error.toString().contains("secret"))
        for (body in listOf(null, "not-json", "[]", "null", "{}", """{"message":"legacy error"}""")) {
            val fallback = AuthErrorMapper.decode(503, body, null, 0)
            assertEquals(503, fallback.status)
            assertNull(fallback.code)
        }
    }

    @Test fun `malformed negative fractional and overflowing counters do not erase known error code`() {
        for (value in listOf("null", "-1", "1.5", "9223372036854775808", "\"5\"", "true", "[]", "{}")) {
            val error = AuthErrorMapper.decode(400,
                """{"code":"OTP_INVALID","remainingAttempts":$value,"retryAfterSeconds":$value}""", null, 0)
            assertEquals("OTP_INVALID", error.code)
            assertNull(error.remainingAttempts)
            assertNull(error.retryAfterSeconds)
        }
    }

    @Test fun `retry-after supports delta seconds and HTTP dates without guessing invalid values`() {
        val now = AuthApiTime.parseMillis("2026-10-23T12:00:00Z")
        assertEquals(42L, AuthApiTime.retryAfterSeconds("42", now))
        assertEquals(0L, AuthApiTime.retryAfterSeconds("0", now))
        assertEquals(42L, AuthApiTime.retryAfterSeconds("Fri, 23 Oct 2026 12:00:42 GMT", now))
        assertEquals(0L, AuthApiTime.retryAfterSeconds("Fri, 23 Oct 2026 11:00:00 GMT", now))
        for (invalid in listOf("-1", "1.5", "forever", "9223372036854775808")) {
            assertNull(AuthApiTime.retryAfterSeconds(invalid, now))
        }
    }

    @Test fun `PATCH converter sends explicit null but omits unchanged fields and completion flag`() {
        val retrofit = Retrofit.Builder().baseUrl("https://example.com/")
            .addConverterFactory(GsonConverterFactory.create(authJson())).build()
        val converter = retrofit.requestBodyConverter<ProfilePatch>(ProfilePatch::class.java, emptyArray(), emptyArray())
        val body = converter.convert(ProfilePatch(nickname = " Ａlice ", allergies = ProfileField.Clear,
            specialDiet = ProfileField.Value("lowSugar")))!!
        val buffer = Buffer(); body.writeTo(buffer)
        val json = JsonParser.parseString(buffer.readUtf8()).asJsonObject
        assertEquals("Alice", json["nickname"].asString)
        assertTrue(json["allergies"].isJsonNull)
        assertEquals("lowSugar", json["specialDiet"].asString)
        assertFalse(json.has("diseases")); assertFalse(json.has("dislikedIngredients"))
        assertFalse(json.has("profileCompleted"))
        assertEquals("{}", authJson().toJson(ProfilePatch()))
        invalid { ProfilePatch(nickname = "　 ") }
        val signup = authJson().toJson(SignupBody("e", "p", "n", "proof", null, null))
        assertFalse(JsonParser.parseString(signup).asJsonObject.has("allergies"))
    }

    @Test fun `profile fields decode id capability CSV and private photo path`() {
        val json = """{"id":17,"email":"a@example.com","nickname":"테스트","allergies":"egg,wheat",
            "specialDiet":"lowSugar","role":"USER","authProvider":"LOCAL","profileCompleted":true,
            "hasPassword":true,"photoUrl":"/api/users/me/photo"}"""
        val profile = AuthResponseMapper.profile(authJson().fromJson(json, ProfileBody::class.java))
        assertEquals(17L, profile.id); assertEquals("egg,wheat", profile.allergies)
        assertNull(profile.diseases); assertEquals("/api/users/me/photo", profile.photoUrl)
        invalid { AuthResponseMapper.profile(authJson().fromJson(json.replace("/api/users/me/photo", "https://other.example/photo"), ProfileBody::class.java)) }
    }

    @Test fun `normalization follows NFKC without touching password or changing nickname case`() {
        assertEquals("user@example.com", normalizeAuthEmail("　Ｕｓｅｒ＠Ｅｘａｍｐｌｅ．ｃｏｍ　"))
        assertEquals("Nick_가", normalizeAuthNickname(" Ｎick_가 "))
    }

    companion object {
        const val loginJson = """{"token":"access-secret","tokenType":"Bearer","userId":17,
            "email":"user@example.com","expiresAt":"2026-10-23T12:15:00Z","expiresInSeconds":900,
            "refreshToken":"refresh-secret","refreshExpiresAt":"2026-11-22T12:00:00Z",
            "isNewUser":false,"profileCompleted":true,"authProvider":"LOCAL","hasPassword":true}"""
        const val linkJson = """{"code":"GOOGLE_LINK_REQUIRED","linkToken":"link-secret",
            "expiresAt":"2026-10-23T12:05:00Z","expiresInSeconds":300,"loginGranted":false}"""
    }
}
