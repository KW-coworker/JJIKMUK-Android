package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.di.NetworkModule
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.google.gson.JsonParser
import kotlinx.coroutines.runBlocking
import mockwebserver3.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class StoredChatHttpTest {
    private val sessions=AuthSessionManager(MemorySessionStore()) { 1000L }
    private suspend fun login() { val attempt=sessions.beginLogin(); check(sessions.acceptLogin(attempt,
        AuthLogin("account-token",1,"1@example.com","unused",900,true),1000)) }
    private fun store(server:MockWebServer)=HttpStoredChatStore(Retrofit.Builder().baseUrl(server.url("/"))
        .client(NetworkModule.provideOkHttpClient()).addConverterFactory(GsonConverterFactory.create()).build()
        .create(StoredChatApi::class.java),sessions,AuthenticatedRequests(sessions,TestAuthRepository()))
    private val account get()=FamilyAccount(1,sessions.state.value.generation)
    private val room="""{"id":12,"clientConversationId":"room-uuid","title":"대화","updatedAt":"2026-11-11T01:00:00"}"""
    @Test fun `stored question omits history profiles owner and all selection queries`()=runBlocking {
        login(); MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().addHeader("X-Jjikmuk-Conversation-Id","12")
                .addHeader("X-Jjikmuk-Chat-Turn-Id","34").addHeader("X-Jjikmuk-Chat-Replayed","true")
                .body("""{"answer":"답변"}""").build())
            val response=store(server).send(account,StoredChatQuestion(12,"message-uuid","질문","880")) as AuthResult.Success
            assertEquals(34L,response.value.id)
            val request=server.takeRequest(); assertEquals("/api/chat",request.url.encodedPath)
            assertEquals("Bearer account-token",request.headers["Authorization"]); assertNull(request.url.encodedQuery)
            val body=JsonParser.parseString(request.body!!.utf8()).asJsonObject
            assertEquals(12L,body["conversationId"].asLong); assertEquals("message-uuid",body["clientMessageId"].asString)
            assertTrue(body["externalAiConsent"].asBoolean)
            for (key in listOf("profile","profiles","chatHistory","chat_history","ownerId","userId","analysisMode")) assertFalse(body.has(key))
        }
    }
    @Test fun `room create and nested list use server ids and UTC chat dates`()=runBlocking {
        login(); MockWebServer().use { server ->
            server.start(); val store=store(server)
            server.enqueue(MockResponse.Builder().code(201).body("""{"data":$room}""").build())
            val result=store.create(account,"room-uuid","대화") as AuthResult.Success
            assertEquals("12",result.value.id)
            assertEquals(java.time.Instant.parse("2026-11-11T01:00:00Z").toEpochMilli(),result.value.updatedAt)
            val create=server.takeRequest(); assertEquals("/api/chat/conversations",create.url.encodedPath)
            assertEquals("room-uuid",JsonParser.parseString(create.body!!.utf8()).asJsonObject["clientConversationId"].asString)
            server.enqueue(MockResponse.Builder().body("""{"data":{"items":[$room],"page":0,"hasNext":false}}""").build())
            assertEquals("12",(store.rooms(account,0) as AuthResult.Success).value.items.single().id)
            assertEquals("0",server.takeRequest().url.queryParameter("page"))
        }
    }
    @Test fun `pending turn lookup and bodyless 204 deletion use account auth`()=runBlocking {
        login(); MockWebServer().use { server ->
            server.start(); val store=store(server)
            server.enqueue(MockResponse.Builder().body("""{"data":{"id":34,"clientMessageId":"uuid","message":"질문","status":"PENDING"}}""").build())
            assertEquals("PENDING",(store.turn(account,12,"uuid") as AuthResult.Success).value.status)
            assertEquals("/api/chat/conversations/12/messages/by-client-id/uuid",server.takeRequest().url.encodedPath)
            server.enqueue(MockResponse.Builder().code(204).build())
            assertTrue(store.delete(account,12) is AuthResult.Success)
            assertEquals("DELETE",server.takeRequest().method)
        }
    }
    @Test fun `paid post is not replayed on server failure`()=runBlocking {
        login(); MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().code(503).addHeader("Retry-After","0").body("{}").build())
            assertTrue(store(server).send(account,StoredChatQuestion(12,"uuid","질문")) is AuthResult.Failure)
            assertEquals(1,server.requestCount)
        }
    }
}
