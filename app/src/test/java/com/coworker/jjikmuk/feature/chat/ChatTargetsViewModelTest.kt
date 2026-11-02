package com.coworker.jjikmuk.feature.chat

import com.coworker.jjikmuk.MainDispatcherRule
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.chat.presentation.ChatViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatTargetsViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private class Targets : AnalysisTargetRepository {
        private val account=FamilyAccount(1,1)
        override val state=MutableStateFlow(AnalysisTargetsState(account,
            selection=AnalysisTargetSelection(account,true,emptyList(),"self"),ready=true))
        fun choose(self:Boolean,ids:List<Long>) { state.value=state.value.copy(selection=AnalysisTargetSelection(account,self,ids,"changed")) }
        var awaitGate: CompletableDeferred<Unit>? = null
        override fun captureAccount()=state.value.account
        override suspend fun awaitSelection(account:FamilyAccount?):AnalysisTargetSelection? {
            awaitGate?.await()
            return state.value.selection?.takeIf { it.account==account }
        }
        override fun isCurrent(selection:AnalysisTargetSelection)=state.value.selection==selection
        override suspend fun setSelected(id:String,checked:Boolean,account:FamilyAccount?)=false
    }
    private class History : ChatHistoryRepository {
        val saved=mutableListOf<Pair<String,Boolean>>()
        val created=mutableListOf<String>()
        override fun observeConversations():Flow<List<ChatConversation>> = flowOf(emptyList())
        override fun observeConversations(query:String):Flow<List<ChatConversation>> = flowOf(emptyList())
        override fun observeMessages(conversationId:String):Flow<List<ChatSavedMessage>> = flowOf(emptyList())
        override suspend fun createConversation(conversationId:String,title:String,preview:String) { created+=conversationId }
        override suspend fun addMessage(conversationId:String,text:String,isMine:Boolean):Long { saved+=text to isMine; return saved.size.toLong() }
        override suspend fun updateTitle(conversationId:String,title:String)=Unit
        override suspend fun updatePinned(conversationId:String,isPinned:Boolean)=Unit
        override suspend fun deleteConversation(conversationId:String)=Unit
        override suspend fun deleteConversations(conversationIds:List<String>)=Unit
    }
    private class Chat : ChatRepository {
        val calls=mutableListOf<Triple<String,ChatProductCandidate?,AnalysisTargetSelection?>>()
        val responses=ArrayDeque<ChatResponse>()
        var gate:CompletableDeferred<Unit>?=null
        override suspend fun sendMessage(message:String,selectedProduct:ChatProductCandidate?,selection:AnalysisTargetSelection?):ChatResponse {
            calls+=Triple(message,selectedProduct,selection)
            withContext(NonCancellable) { gate?.await() }
            return if (responses.isEmpty()) ChatResponse("서버 답변") else responses.removeFirst()
        }
    }
    @Test fun `candidate confirmation retains original question and selected profiles`() = runTest {
        val targets=Targets(); val history=History(); val chat=Chat()
        val candidate=ChatProductCandidate("포키","해태","880",null,emptyList(),null)
        chat.responses.add(ChatResponse("선택해 주세요",listOf(candidate))); chat.responses.add(ChatResponse("분석 결과"))
        val vm=ChatViewModel(chat,history,targets)
        vm.start(null,"포키 먹어도 돼?"); runCurrent()
        vm.selectProductCandidate(candidate); runCurrent()
        assertEquals(2,chat.calls.size)
        assertEquals("포키 먹어도 돼?",chat.calls[1].first)
        assertEquals("880",chat.calls[1].second!!.barcode)
        assertEquals(targets.state.value.selection,chat.calls[1].third)
    }
    @Test fun `changing target cancels pending context and never displays or saves its late answer`() = runTest {
        val targets=Targets(); val history=History(); val chat=Chat().apply { gate=CompletableDeferred() }
        val vm=ChatViewModel(chat,history,targets)
        vm.start(null,"질문"); runCurrent()
        targets.choose(false,listOf(41)); runCurrent()
        chat.gate!!.complete(Unit); runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.messages.any { it.text=="서버 답변" })
        assertFalse(history.saved.any { it.first=="서버 답변" })
        assertTrue(vm.uiState.value.productCandidates.isEmpty())
    }
    @Test fun `changed target starts new conversation rather than reusing old context`() = runTest {
        val targets=Targets(); val history=History(); val chat=Chat(); val vm=ChatViewModel(chat,history,targets)
        vm.start(null,"첫 질문"); runCurrent()
        targets.choose(false,listOf(41)); runCurrent()
        vm.sendMessage("다음 질문"); runCurrent()
        assertEquals(2,history.created.distinct().size)
        assertEquals(listOf(41L),chat.calls.last().third!!.familyProfileIds)
        assertFalse(chat.calls.last().third!!.includeSelf)
    }
    @Test fun `double sends do not dispatch another paid request`() = runTest {
        val targets=Targets(); val history=History(); val chat=Chat().apply { gate=CompletableDeferred() }
        val vm=ChatViewModel(chat,history,targets)
        vm.start(null,"질문"); vm.sendMessage("연타"); runCurrent()
        assertEquals(1,chat.calls.size)
        chat.gate!!.complete(Unit); runCurrent()
    }
    @Test fun `all off selection is passed unchanged instead of substituting self`() = runTest {
        val targets=Targets().apply { choose(false,emptyList()) }
        val chat=Chat();val vm=ChatViewModel(chat,History(),targets)
        vm.start(null,"일반 질문"); runCurrent()
        assertEquals("none",chat.calls.single().third!!.mode)
    }
    @Test fun `question immediately after toggle uses committed target without losing question`() = runTest {
        val targets=Targets().apply { awaitGate=CompletableDeferred() }
        val chat=Chat(); val vm=ChatViewModel(chat,History(),targets)
        vm.start(null,"포키 먹어도 돼?"); runCurrent()
        targets.choose(false,listOf(41)); runCurrent()
        targets.awaitGate!!.complete(Unit); runCurrent()
        assertEquals(1,chat.calls.size)
        assertEquals("포키 먹어도 돼?",chat.calls.single().first)
        assertEquals(listOf(41L),chat.calls.single().third!!.familyProfileIds)
    }

}
