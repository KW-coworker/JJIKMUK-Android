package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.ChatApi
import com.coworker.jjikmuk.data.remote.dto.*
import com.coworker.jjikmuk.di.NetworkModule
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.google.gson.JsonParser
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import mockwebserver3.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

@OptIn(ExperimentalCoroutinesApi::class)
class ChatSelectedTargetsTest {
    private val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
    private suspend fun login() {
        val attempt = sessions.beginLogin()
        check(sessions.acceptLogin(attempt, AuthLogin("account-token", 5, "5@example.com", "unused", 900, true), 1000))
    }
    private class Targets(private val sessions: AuthSessionManager) : AnalysisTargetRepository {
        override val state = MutableStateFlow(AnalysisTargetsState())
        fun choose(self: Boolean, ids: List<Long>) {
            val account = captureAccount()!!
            state.value = AnalysisTargetsState(account, selection = AnalysisTargetSelection(account, self, ids, "revision"), ready = true)
        }
        override fun captureAccount() = sessions.state.value.let { s -> s.credentials?.let { FamilyAccount(it.userId, s.generation) } }
        override suspend fun setSelected(id: String, checked: Boolean, account: FamilyAccount?) = false
        override suspend fun awaitSelection(account: FamilyAccount?) = state.value.selection?.takeIf { it.account == account }
        override fun isCurrent(selection: AnalysisTargetSelection) = captureAccount() == selection.account && state.value.selection == selection
    }
    private fun api(server: MockWebServer) = Retrofit.Builder().baseUrl(server.url("/"))
        .client(NetworkModule.provideOkHttpClient()).addConverterFactory(GsonConverterFactory.create()).build().create(ChatApi::class.java)
    private fun repo(api: ChatApi, targets: Targets) = ChatRepositoryImpl(api, AuthorizedOperations(sessions, TestAuthRepository()), targets)

    @Test fun `self and all selected family ids use query while body excludes private profile snapshots`() = runBlocking {
        login(); val targets = Targets(sessions).apply { choose(true, listOf(41,42)) }
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"answer":"분석 결과"}""").build())
            val result = repo(api(server), targets).sendMessage("먹어도 돼?", selection = targets.state.value.selection)
            assertEquals("분석 결과", result.answer)
            val request = server.takeRequest()
            assertEquals("Bearer account-token", request.headers["Authorization"])
            assertEquals("selected", request.url.queryParameter("analysisMode"))
            assertEquals("true", request.url.queryParameter("includeSelf"))
            assertEquals(listOf("41","42"), request.url.queryParameterValues("familyProfileIds"))
            val body = JsonParser.parseString(request.body!!.utf8()).asJsonObject
            assertEquals(setOf("message","chatHistory"), body.keySet())
            assertFalse(body.has("allergies")); assertFalse(body.has("profile")); assertFalse(body.has("profiles")); assertFalse(body.has("externalAiConsent"))
        }
    }
    @Test fun `family only does not implicitly include self`() = runBlocking {
        login(); val targets = Targets(sessions).apply { choose(false,listOf(41)) }
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"answer":"가족 분석"}""").build())
            repo(api(server),targets).sendMessage("질문")
            val request = server.takeRequest()
            assertEquals("selected",request.url.queryParameter("analysisMode"))
            assertEquals("false",request.url.queryParameter("includeSelf"))
        }
    }
    @Test fun `all off preserves bearer and explicitly requests none without selected-only parameters`() = runBlocking {
        login(); val targets = Targets(sessions).apply { choose(false,emptyList()) }
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"answer":"일반 조회"}""").build())
            repo(api(server),targets).sendMessage("질문")
            val request=server.takeRequest()
            assertEquals("none",request.url.queryParameter("analysisMode"))
            assertNull(request.url.queryParameter("includeSelf")); assertNull(request.url.queryParameter("familyProfileIds"))
            assertEquals("Bearer account-token",request.headers["Authorization"])
        }
    }
    @Test fun `self only does not send incompatible includeSelf query`() = runBlocking {
        login(); val targets = Targets(sessions).apply { choose(true,emptyList()) }
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().body("""{"answer":"본인 분석"}""").build())
            repo(api(server),targets).sendMessage("질문")
            val request=server.takeRequest()
            assertEquals("self",request.url.queryParameter("analysisMode")); assertNull(request.url.queryParameter("includeSelf"))
        }
    }
    @Test fun `unsupported deployment never falls back to a self-only retry`() = runBlocking {
        login(); val targets=Targets(sessions).apply { choose(false,listOf(41)) }
        MockWebServer().use { server ->
            server.start(); server.enqueue(MockResponse.Builder().code(400).body("""{"code":"CHAT_ANALYSIS_SELECTION_UNSUPPORTED"}""").build())
            val result=repo(api(server),targets).sendMessage("질문")
            assertEquals("CHAT_ANALYSIS_SELECTION_UNSUPPORTED",result.errorCode)
            assertEquals(1,server.requestCount)
        }
    }
    @Test fun `late answer from previous target is discarded`() = runTest {
        login(); val targets=Targets(sessions).apply { choose(true,emptyList()) }
        val gate=CompletableDeferred<Unit>()
        val fake=object:ChatApi {
            override suspend fun sendMessage(authorization:String,request:ChatApiRequest,analysisMode:String?,includeSelf:Boolean?,familyProfileIds:List<Long>?):ChatApiResponse {
                gate.await(); return ChatApiResponse(answer="이전 대상 답변")
            }
        }
        val pending=async { repo(fake,targets).sendMessage("질문") }; runCurrent()
        targets.choose(false,listOf(41)); gate.complete(Unit)
        assertEquals("ANALYSIS_SELECTION_CHANGED",pending.await().errorCode)
    }
}
