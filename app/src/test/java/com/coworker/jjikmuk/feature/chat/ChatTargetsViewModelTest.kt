package com.coworker.jjikmuk.feature.chat

import com.coworker.jjikmuk.feature.auth.domain.*
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
        var refreshCount = 0
        var refreshAction: (() -> Unit)? = null
        override suspend fun refresh(account: FamilyAccount?, confirmChanges: Boolean) { refreshCount++; refreshAction?.invoke() }
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
    private class Store(private val targets: Targets) : StoredChatStore {
        val calls = mutableListOf<StoredChatQuestion>()
        val creates = mutableListOf<String>()
        val answers = ArrayDeque<ChatResponse>()
        var gate: CompletableDeferred<Unit>? = null
        var ignoreCancellation = false
        var sendFailure: AuthFailure? = null
        var recovered: StoredChatTurn? = null
        var roomFailure = false
        var createGate: CompletableDeferred<Unit>? = null
        var oldTurns = emptyList<StoredChatTurn>()
        override fun captureAccount() = targets.captureAccount()
        override suspend fun create(account: FamilyAccount, clientId: String, title: String): AuthResult<ChatConversation> {
            creates += clientId
            createGate?.await()
            return if (roomFailure) AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
            else AuthResult.Success(ChatConversation("12",title,"",false,0))
        }
        override suspend fun rooms(account: FamilyAccount, page: Int) = AuthResult.Success(StoredChatRooms(emptyList(),page,false))
        override suspend fun room(account: FamilyAccount, id: Long) = AuthResult.Success(ChatConversation(id.toString(),"서버 대화","",false,0))
        override suspend fun turns(account: FamilyAccount, id: Long, after: Long) = AuthResult.Success(StoredChatTurns(oldTurns, oldTurns.lastOrNull()?.id ?: after,false))
        override suspend fun turn(account: FamilyAccount, id: Long, clientId: String) = AuthResult.Success(
            recovered?.copy(clientMessageId = clientId) ?: StoredChatTurn(9,clientId,"질문",null,"SUCCEEDED",ChatResponse("복구한 답변")))
        override suspend fun send(account: FamilyAccount, question: StoredChatQuestion): AuthResult<StoredChatTurn> {
            calls += question
            if (ignoreCancellation) withContext(NonCancellable) { gate?.await() } else gate?.await()
            sendFailure?.let { return AuthResult.Failure(it) }
            return AuthResult.Success(StoredChatTurn(calls.size.toLong(),question.clientMessageId,question.message,question.barcode,
                "SUCCEEDED",if (answers.isEmpty()) ChatResponse("서버 답변") else answers.removeFirst()))
        }
        override suspend fun delete(account: FamilyAccount, id: Long) = AuthResult.Success(Unit)
    }
    @Test fun `candidate confirmation keeps original question barcode and server room`() = runTest {
        val targets=Targets(); val history=History(); val store=Store(targets)
        val candidate=ChatProductCandidate("포키","해태","880",null,emptyList(),null)
        store.answers.add(ChatResponse("선택해 주세요",listOf(candidate))); store.answers.add(ChatResponse("분석 결과"))
        val vm=ChatViewModel(store,history,targets)
        vm.start(null,"포키 먹어도 돼?"); runCurrent()
        advanceTimeBy(6000); runCurrent()
        vm.selectProductCandidate(candidate); runCurrent()
        vm.sendMessage("성분도 알려줘"); runCurrent()
        assertEquals("포키 먹어도 돼?",store.calls[1].message)
        assertEquals("880",store.calls[1].barcode); assertEquals("880",store.calls[2].barcode)
        assertEquals(1,store.creates.size); assertTrue(store.calls.all { it.conversationId==12L })
        assertEquals(3,store.calls.map { it.clientMessageId }.distinct().size)
        assertTrue(history.saved.isEmpty())
    }
    @Test fun `initial product detail barcode reaches the first stored question`() = runTest {
        val targets=Targets(); val store=Store(targets); val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"포키 먹어도 돼?", "880"); runCurrent()
        assertEquals("880",store.calls.single().barcode)
        assertTrue(store.calls.single().externalAiConsent)
    }
    @Test fun `changing family keeps stored history but clears product context`() = runTest {
        val targets=Targets(); val store=Store(targets); val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"첫 질문", "880"); runCurrent()
        targets.choose(false,listOf(41)); runCurrent()
        vm.sendMessage("다음 질문"); runCurrent()
        assertEquals(1,store.creates.size); assertNull(store.calls.last().barcode)
        assertEquals(4,vm.uiState.value.messages.size)
    }
    @Test fun `lost send response resolves the same uuid without another paid call or duplicate bubbles`() = runTest {
        val targets=Targets(); val store=Store(targets).apply { sendFailure=AuthFailure(AuthFailureKind.Network) }
        val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"질문"); runCurrent()
        assertEquals(1,store.calls.size); assertEquals(2,vm.uiState.value.messages.size)
        assertEquals("복구한 답변",vm.uiState.value.messages.last().text)
        assertFalse(vm.uiState.value.canRetry)
    }
    @Test fun `restored pending turn is polled without resending the question`() = runTest {
        val targets=Targets(); val store=Store(targets)
        store.oldTurns=listOf(StoredChatTurn(9,"original-uuid","질문",null,"PENDING"))
        val vm=ChatViewModel(store,History(),targets)
        vm.start("12",""); runCurrent()
        assertTrue(store.calls.isEmpty()); assertEquals(2,vm.uiState.value.messages.size)
    }
    @Test fun `failed room creation retry retains the client room uuid`() = runTest {
        val targets=Targets(); val store=Store(targets).apply { roomFailure=true }; val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"질문"); runCurrent()
        assertTrue(vm.uiState.value.canRetry); store.roomFailure=false
        vm.retry(); runCurrent()
        assertEquals(2,store.creates.size); assertEquals(1,store.creates.distinct().size)
        assertEquals(1,store.calls.size)
    }
    @Test fun `double sends do not dispatch another paid request`() = runTest {
        val targets=Targets(); val store=Store(targets).apply { gate=CompletableDeferred() }; val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"질문"); vm.sendMessage("연타"); runCurrent()
        assertEquals(1,store.calls.size); store.gate!!.complete(Unit); runCurrent()
    }
    @Test fun `all off retains external AI processing while using the server selection`() = runTest {
        val targets=Targets().apply { choose(false,emptyList()) }; val store=Store(targets); val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"일반 질문"); runCurrent()
        assertEquals(1,store.calls.size); assertTrue(store.calls.single().externalAiConsent)
    }
    @Test fun `late old-account response cannot enter the new account conversation`() = runTest {
        val targets=Targets(); val store=Store(targets).apply { gate=CompletableDeferred(); ignoreCancellation=true }
        val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"이전 계정 질문"); runCurrent()
        val next=FamilyAccount(2,2)
        targets.state.value=AnalysisTargetsState(next,selection=AnalysisTargetSelection(next,true,emptyList(),"new"),ready=true)
        runCurrent(); store.gate!!.complete(Unit); runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun `restored failed turn retry uses a new client message uuid`() = runTest {
        val targets=Targets(); val store=Store(targets)
        store.oldTurns=listOf(StoredChatTurn(9,"failed-uuid","질문",null,"FAILED",errorCode="CHAT_REQUEST_INTERRUPTED"))
        val vm=ChatViewModel(store,History(),targets)
        vm.start("12",""); runCurrent()
        assertTrue(vm.uiState.value.canRetry)
        vm.retry(); runCurrent()
        assertEquals(1,store.calls.size); assertNotEquals("failed-uuid",store.calls.single().clientMessageId)
    }

    @Test fun `new answer reveals progressively while preserving the complete server text`() = runTest {
        val targets=Targets(); val store=Store(targets)
        store.answers.add(ChatResponse("안녕하세요. 우유 성분을 확인했어요."))
        val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"질문"); runCurrent()
        val answer=vm.uiState.value.messages.last()
        assertTrue(answer.isTyping); assertEquals("",answer.displayedText)
        assertEquals("안녕하세요. 우유 성분을 확인했어요.",answer.text)
        advanceTimeBy(60); runCurrent()
        val partial=vm.uiState.value.messages.last()
        assertTrue(partial.isTyping); assertTrue(partial.displayedText!!.isNotEmpty())
        assertTrue(partial.displayedText!!.length < partial.text.length)
        vm.finishTyping(); runCurrent()
        assertFalse(vm.uiState.value.messages.last().isTyping)
        assertNull(vm.uiState.value.messages.last().displayedText)
        assertEquals(1,store.calls.size)
    }
    @Test fun `restored successful history is displayed immediately without replaying the typing effect`() = runTest {
        val targets=Targets(); val store=Store(targets)
        store.oldTurns=listOf(StoredChatTurn(9,"done-uuid","질문",null,"SUCCEEDED",ChatResponse("저장된 답변")))
        val vm=ChatViewModel(store,History(),targets)
        vm.start("12",""); runCurrent()
        val answer=vm.uiState.value.messages.last()
        assertEquals("저장된 답변",answer.text); assertFalse(answer.isTyping); assertNull(answer.displayedText)
        assertTrue(store.calls.isEmpty())
    }
    @Test fun `typing ends naturally and the next question completes the previous effect`() = runTest {
        val targets=Targets(); val store=Store(targets); val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"질문"); runCurrent()
        advanceTimeBy(5000); runCurrent()
        assertTrue(vm.uiState.value.messages.none { it.isTyping })
        vm.sendMessage("두번째"); runCurrent()
        assertEquals(1,vm.uiState.value.messages.count { it.isTyping })
        vm.sendMessage("세번째"); runCurrent()
        assertEquals(1,vm.uiState.value.messages.count { it.isTyping })
        assertEquals(3,store.calls.size)
    }

    @Test fun `ambiguous product picker waits until typing finishes plus 1500 milliseconds`() = runTest {
        val targets=Targets(); val store=Store(targets)
        val candidate=ChatProductCandidate("포키","해태","880",null,emptyList(),null)
        store.answers.add(ChatResponse("선택",listOf(candidate)))
        val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"포키 먹어도 돼?"); runCurrent()
        assertTrue(vm.uiState.value.messages.last().isTyping); assertTrue(vm.uiState.value.productCandidates.isEmpty())
        advanceTimeBy(40); runCurrent()
        assertFalse(vm.uiState.value.messages.last().isTyping)
        advanceTimeBy(1499); runCurrent(); assertTrue(vm.uiState.value.productCandidates.isEmpty())
        advanceTimeBy(1); runCurrent(); assertEquals(listOf(candidate),vm.uiState.value.productCandidates)
    }
    @Test fun `target change cancels a delayed picker and full text skips begin the same delay`() = runTest {
        val targets=Targets(); val store=Store(targets)
        val candidate=ChatProductCandidate("포키","해태","880",null,emptyList(),null)
        store.answers.add(ChatResponse("검색된 상품 중에서 선택해 주세요.",listOf(candidate)))
        val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"질문"); runCurrent()
        vm.finishTyping(); advanceTimeBy(1000); runCurrent()
        assertTrue(vm.uiState.value.productCandidates.isEmpty())
        targets.choose(false,listOf(41)); runCurrent()
        advanceTimeBy(1000); runCurrent()
        assertTrue(vm.uiState.value.productCandidates.isEmpty()); assertEquals(1,store.calls.size)
    }

    @Test fun `empty server room is explained rather than leaving only the target label`() = runTest {
        val targets=Targets(); val store=Store(targets); val vm=ChatViewModel(store,History(),targets)
        vm.start("12",""); runCurrent()
        assertTrue(vm.uiState.value.messages.isEmpty())
        assertTrue(vm.uiState.value.notice!!.contains("저장된 메시지가 없어요"))
        assertFalse(vm.uiState.value.isLoading)
    }
    @Test fun `an empty later read cannot silently erase already visible successful turns`() = runTest {
        val targets=Targets(); val store=Store(targets); val vm=ChatViewModel(store,History(),targets)
        store.oldTurns=listOf(StoredChatTurn(9,"done-uuid","질문",null,"SUCCEEDED",ChatResponse("기존 답변")))
        vm.start("12",""); runCurrent()
        store.oldTurns=emptyList(); vm.reloadConversation(); runCurrent()
        assertEquals("기존 답변",vm.uiState.value.messages.last().text)
        assertTrue(vm.uiState.value.canRetry)
        assertTrue(vm.uiState.value.notice!!.contains("기존 내용은 유지"))
    }

    @Test fun `home question bubble is visible immediately while target loading is pending`() = runTest {
        val targets=Targets().apply { awaitGate=CompletableDeferred() }
        val store=Store(targets); val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"새우깡 먹어도 돼?"); runCurrent()
        assertEquals("새우깡 먹어도 돼?",vm.uiState.value.messages.single().text)
        assertTrue(vm.uiState.value.messages.single().isMine); assertTrue(vm.uiState.value.isLoading)
        targets.awaitGate!!.complete(Unit); runCurrent()
    }
    @Test fun `background target refresh during room creation does not drop the home question`() = runTest {
        val targets=Targets(); val store=Store(targets).apply { createGate=CompletableDeferred() }
        val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"새우깡 먹어도 돼?"); runCurrent()
        val saved=targets.state.value
        targets.awaitGate=CompletableDeferred()
        targets.state.value=saved.copy(ready=false,selection=null)
        store.createGate!!.complete(Unit); runCurrent()
        assertTrue(store.calls.isEmpty()); assertTrue(vm.uiState.value.messages.any { it.isMine })
        targets.state.value=saved; targets.awaitGate!!.complete(Unit); runCurrent()
        assertEquals(1,store.calls.size)
        assertEquals("새우깡 먹어도 돼?",store.calls.single().message)
    }

    @Test fun `failed target read is refreshed once before retrying the unsent home question`() = runTest {
        val targets=Targets(); val good=targets.state.value
        targets.state.value=good.copy(ready=false,selection=null,message="조회 실패")
        targets.refreshAction={ targets.state.value=good }
        val store=Store(targets);val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"새우깡 먹어도 돼?");runCurrent()
        assertEquals(1,targets.refreshCount);assertEquals(1,store.calls.size)
        assertEquals("새우깡 먹어도 돼?",store.calls.single().message)
    }
    @Test fun `persistent target failure does not loop or create empty server rooms`() = runTest {
        val targets=Targets();targets.state.value=targets.state.value.copy(ready=false,selection=null,message="조회 실패")
        val store=Store(targets);val vm=ChatViewModel(store,History(),targets)
        vm.start(null,"질문");runCurrent()
        assertEquals(1,targets.refreshCount);assertTrue(store.calls.isEmpty());assertTrue(store.creates.isEmpty())
        assertTrue(vm.uiState.value.canRetry);assertTrue(vm.uiState.value.messages.single().isMine)
        vm.retry();runCurrent();assertEquals(2,targets.refreshCount);assertTrue(store.calls.isEmpty())
    }

}
