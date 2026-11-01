package com.coworker.jjikmuk.feature.auth

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.di.AuthModule
import com.coworker.jjikmuk.data.remote.BackendServer
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.Assume.assumeTrue
import org.junit.Test

/** Explicit opt-in, one read using the existing session. Never logs credentials or profile values. */
class StoredProfileDiagnosticTest {
    @Test fun inspectCurrentProfile() = runBlocking {
        val instrument = InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("inspectStoredProfile") == "true")
        fun report(value: String) { instrument.sendStatus(0, Bundle().apply { putString("profileDiagnostic", value) }) }
        val session = AuthModule.provideSessionStore(instrument.targetContext).read()
        if (session == null) { report("NO_STORED_SESSION"); return@runBlocking }
        report("storedSession=true accessExpired=${session.expiresAtMillis <= System.currentTimeMillis()}")
        val client = OkHttpClient.Builder().retryOnConnectionFailure(false).followRedirects(false).followSslRedirects(false).build()
        client.newCall(Request.Builder().url(BackendServer.BASE_URL + "api/users/me")
            .header("Authorization", "Bearer ${session.accessToken}").build()).execute().use { response ->
            report("HTTP=${response.code} date=${response.header("Date")}")
            val json = JsonParser.parseString(response.body.string()).asJsonObject
            fun shape(obj: com.google.gson.JsonObject): String = obj.entrySet().joinToString { (key, value) ->
                val type = when { value.isJsonNull -> "null"; value.isJsonObject -> "object"; value.isJsonArray -> "array"
                    value.asJsonPrimitive.isBoolean -> "boolean"; value.asJsonPrimitive.isNumber -> "number"; else -> "string" }
                "$key:$type"
            }
            report("root=" + shape(json))
            val data = json.getAsJsonObject("data")
            if (data != null) {
                report("data=" + shape(data))
                report("identityMatches=${data["id"]?.asLong == session.userId}; providerPresent=${data.has("authProvider")}; passwordCapabilityPresent=${data.has("hasPassword")}")
            }
            val code = json["code"]?.asString
            if (code != null && code.matches(Regex("[A-Z_]{1,80}"))) report("code=$code")
        }
        Unit
    }
}
