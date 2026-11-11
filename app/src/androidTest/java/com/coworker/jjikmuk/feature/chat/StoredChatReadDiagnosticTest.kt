package com.coworker.jjikmuk.feature.chat

import android.os.Bundle
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.di.AuthModule
import com.coworker.jjikmuk.data.remote.BackendServer
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Explicit opt-in. Reads existing session/server records only; never prints tokens or message contents. */
class StoredChatReadDiagnosticTest {
    @Test fun readExistingChatMetadata() = runBlocking {
        assumeTrue(InstrumentationRegistry.getArguments().getString("readOnlyChatAudit") == "true")
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        fun report(text: String) { instrumentation.sendStatus(0, Bundle().apply { putString("chatAudit",text) }) }
        val session = AuthModule.provideSessionStore(instrumentation.targetContext).read()
        if (session == null) { report("NO_STORED_SESSION"); return@runBlocking }
        report("sessionExists=true expired=${session.expiresAtMillis <= System.currentTimeMillis()}")
        val client=OkHttpClient.Builder().retryOnConnectionFailure(false).callTimeout(15,TimeUnit.SECONDS).build()
        fun shape(value: JsonObject) = value.entrySet().joinToString { (key,v) ->
            key+":"+when { v.isJsonNull->"null";v.isJsonObject->"object";v.isJsonArray->"array";v.asJsonPrimitive.isBoolean->"boolean";v.asJsonPrimitive.isNumber->"number";else->"string" }
        }
        fun get(path: String): JsonObject? = client.newCall(Request.Builder().url(BackendServer.BASE_URL+path)
            .header("Authorization","Bearer ${session.accessToken}").build()).execute().use {
                report("path=$path http=${it.code}")
                val json=try { JsonParser.parseString(it.body.string()).asJsonObject } catch (_:Exception) { return@use null }
                if (!it.isSuccessful) { report("errorCode="+(json["code"]?.asString ?: "missing")); return@use null }
                report("root="+shape(json));json
            }
        get("api/users/me")?.getAsJsonObject("data")?.let {
            report("me="+shape(it))
            try {
                com.coworker.jjikmuk.feature.auth.data.AuthResponseMapper.profile(
                    com.coworker.jjikmuk.feature.auth.data.authJson().fromJson(it, com.coworker.jjikmuk.feature.auth.data.ProfileBody::class.java))
                report("profileMapper=OK")
            } catch (error: Exception) { report("profileMapper="+error.javaClass.simpleName) }
        }
        get("api/users/me/family-profiles")?.getAsJsonArray("data")?.let { data ->
            report("familyCount=${data.size()}");if (data.size()>0) report("family="+shape(data[0].asJsonObject))
        }
        get("api/users/me/analysis-selection")?.getAsJsonObject("data")?.let {
            report("selection="+shape(it));report("selectionMode="+it["mode"]+" configured="+it["configured"]+" version="+it["version"])
        }
        val actualRepo=com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository.create(BackendServer.BASE_URL.toHttpUrl())
        for ((name,result) in listOf("me" to actualRepo.me(session.accessToken), "families" to actualRepo.families(session.accessToken))) {
            report("productionDecode${name}="+when(result) {
                is com.coworker.jjikmuk.feature.auth.domain.AuthResult.Success -> "OK"
                is com.coworker.jjikmuk.feature.auth.domain.AuthResult.Failure -> "${result.error.kind}/${result.error.status}/${result.error.code}"
            })
        }
        val data=get("api/chat/conversations?page=0&size=20")?.getAsJsonObject("data")
        val rooms=data?.getAsJsonArray("items")
        report("roomCount=${rooms?.size() ?: -1}")
        rooms?.take(3)?.forEachIndexed { index,room ->
            val id=room.asJsonObject["id"].asLong
            get("api/chat/conversations/$id")?.getAsJsonObject("data")?.let { report("room$index="+shape(it))
                val time=it["updatedAt"]?.asString.orEmpty()
                report("roomTimeLocalParse="+runCatching { java.time.LocalDateTime.parse(time) }.isSuccess+" endsZ="+time.endsWith("Z")) }
            val turns=get("api/chat/conversations/$id/messages?afterId=0&limit=20")?.getAsJsonObject("data")
            if (turns!=null) {
                report("turnPage$index="+shape(turns))
                val items=turns.getAsJsonArray("items");report("turnCount$index=${items?.size() ?: -1}")
                items?.forEach { entry ->
                    val turn=entry.asJsonObject; report("turnShape="+shape(turn));report("turnStatus="+turn["status"]?.asString)
                    val response=turn["response"]?.takeIf { it.isJsonObject }?.asJsonObject
                    if (response!=null) {
                        report("responseShape="+shape(response))
                        response["product_candidates"]?.takeIf { it.isJsonArray }?.asJsonArray?.firstOrNull()?.takeIf { it.isJsonObject }?.let { report("candidateShape="+shape(it.asJsonObject)) }
                        report("answerSource="+response["answer_source"]?.asString)
                        val external=response["external_ai_processing"]?.takeIf { it.isJsonObject }?.asJsonObject
                        if (external!=null) report("externalAi allowed=${external["allowed_for_request"]} used=${external["used"]} status=${external["status"]}")
                    }
                }
            }
        }
        Unit
    }
}
