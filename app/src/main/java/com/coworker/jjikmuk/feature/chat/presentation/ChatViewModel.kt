package com.coworker.jjikmuk.feature.chat.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.ChatProductCandidate
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

@HiltViewModel
class ChatViewModel @Inject constructor(private val store: StoredChatStore,
    private val chatHistoryRepository: ChatHistoryRepository, private val targets: AnalysisTargetRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState = _uiState.asStateFlow()
    val targetState = targets.state
    fun refreshTargets() { viewModelScope.launch { targets.refresh(confirmChanges = true) } }
    fun syncTargets() { viewModelScope.launch {
        targets.refresh()
        // A newly created home-question room must not be reloaded while its first question is unsent.
        if (startKey?.first != null && roomId != null && unsent == null && pending == null && !_uiState.value.isLoading) reloadConversation()
    } }
    fun selectTarget(id: String, checked: Boolean) { val account = targets.captureAccount(); viewModelScope.launch { targets.setSelected(id, checked, account) } }
    private var owner: FamilyAccount? = null
    private var roomId: Long? = null
    private var clientRoomId = UUID.randomUUID().toString()
    private var roomTitle = "새 대화"
    private var startKey: Triple<String?, String, String?>? = null
    private var revision = 0L
    private var job: Job? = null
    private var titleJob: Job? = null
    private var typingJob: Job? = null
    private var typingMessageId: Long? = null
    private var typingText: String? = null
    private var candidateJob: Job? = null
    private var delayedCandidates: List<ChatProductCandidate> = emptyList()
    private var candidateSelection: AnalysisTargetSelection? = null
    private val turns = linkedMapOf<String, StoredChatTurn>()
    private var selectedProduct: ChatProductCandidate? = null
    private var candidateQuestion: String? = null
    private var selection: AnalysisTargetSelection? = null
    private data class Pending(val question: StoredChatQuestion, val selection: AnalysisTargetSelection?, val canResend: Boolean = true)
    private var pending: Pending? = null
    private var failed: Pair<String, ChatProductCandidate?>? = null
    private var unsent: Pair<String, ChatProductCandidate?>? = null

    init {
        viewModelScope.launch {
            targetState.collect { state ->
                if (owner != null && store.captureAccount() != owner) {
                    reset(); _uiState.value = ChatUiState(notice = "계정이 바뀌었어요. 현재 계정의 대화를 다시 열어 주세요.")
                } else if (state.account == owner && state.selection != null && selection != null && state.selection != selection) {
                    cancelCandidatePresentation()
                    selectedProduct = null; candidateQuestion = null
                    _uiState.update { it.copy(error = null, canRetry = false, productCandidates = emptyList(), suggestedQuestions = emptyList(), notice = "분석 대상이 바뀌었어요. 새 질문부터 변경한 조건을 적용해요.") }
                }
                if (state.account == owner && state.selection != null) selection = state.selection
            }
        }
    }
    private fun current(ticket: Long, account: FamilyAccount) = revision == ticket && store.captureAccount() == account
    private fun reset() {
        revision++; cancelCandidatePresentation(); typingJob?.cancel(); typingJob = null; typingMessageId = null; typingText = null
        job?.cancel(); titleJob?.cancel(); titleJob = null; job = null; owner = null; roomId = null; startKey = null
        clientRoomId = UUID.randomUUID().toString(); roomTitle = "새 대화"
        turns.clear(); pending = null; failed = null; unsent = null; selectedProduct = null; candidateQuestion = null; selection = null
        _uiState.value = ChatUiState()
    }
    fun start(conversationId: String?, initialMessage: String, initialBarcode: String? = null) {
        val key = Triple(conversationId, initialMessage, initialBarcode)
        if (startKey == key && owner == store.captureAccount()) return
        reset(); startKey = key; owner = store.captureAccount(); selection = targets.state.value.selection
        if (conversationId != null) {
            roomId = conversationId.toLongOrNull()?.takeIf { it > 0 }
            if (roomId == null) { _uiState.value = ChatUiState(notice = "이 기기의 이전 대화는 서버 대화가 아니어서 불러올 수 없어요."); return }
            reloadConversation()
        } else if (initialMessage.isNotBlank()) {
            selectedProduct = initialBarcode?.takeIf { it.isNotBlank() }?.let {
                ChatProductCandidate("선택한 상품", "", it, null, emptyList(), null)
            }
            sendMessage(initialMessage)
        }
    }
    fun sendMessage(message: String) {
        val text = message.trim()
        if (text.isBlank() || text.length > 4000 || _uiState.value.isLoading) return
        if (pending != null) { _uiState.update { it.copy(notice = "이전 질문의 처리 상태를 먼저 확인해 주세요.", canRetry = true) }; return }
        dispatch(text, selectedProduct)
    }
    fun dismissProductCandidates() { cancelCandidatePresentation(); _uiState.update { it.copy(productCandidates = emptyList()) }; candidateQuestion = null }
    fun selectProductCandidate(product: ChatProductCandidate) {
        if (product !in _uiState.value.productCandidates || _uiState.value.isLoading || pending != null) return
        selectedProduct = product
        dispatch(candidateQuestion ?: "이 상품 먹어도 돼?", product)
    }
    private fun dispatch(message: String, product: ChatProductCandidate?) {
        val retryError = _uiState.value.error?.takeIf { it.isRetrying }
        _uiState.update { it.copy(error = retryError, suggestedQuestions = emptyList()) }
        cancelCandidatePresentation(); candidateQuestion = null
        finishTyping()
        unsent = message to product
        render()
        val account = store.captureAccount() ?: run { showError(ChatErrorCodes.AUTHENTICATION_REQUIRED); return }
        owner = account; val ticket = revision
        _uiState.update { it.copy(isLoading = true, productCandidates = emptyList(), notice = null, canRetry = false) }
        job = viewModelScope.launch {
            try {
                var chosen = targets.awaitSelection(account)
                if (!current(ticket, account)) return@launch
                if (chosen == null) {
                    // Recover a previous failed GET once; never replay an AI POST or overwrite a selection conflict.
                    targets.refresh(account)
                    if (!current(ticket, account)) return@launch
                    chosen = targets.awaitSelection(account)
                }
                if (!current(ticket, account)) return@launch
                if (chosen == null) { showError(ChatErrorCodes.ANALYSIS_SELECTION_UNAVAILABLE); return@launch }
                selection = chosen
                if (roomId == null) {
                    roomTitle = createChatTitle(message)
                    when (val created = store.create(account, clientRoomId, roomTitle)) {
                        is AuthResult.Success -> {
                            roomId = created.value.id.toLong()
                            watchTitle(account, ticket, created.value.title)
                        }
                        is AuthResult.Failure -> { showFailure(created.error); return@launch }
                    }
                }
                if (!current(ticket, account)) return@launch
                // A background GET can temporarily mark the same selection unready. Wait for it before comparing.
                val confirmed = targets.awaitSelection(account)
                if (!current(ticket, account)) return@launch
                if (confirmed != chosen) { showError(ChatErrorCodes.ANALYSIS_SELECTION_CHANGED); return@launch }
                val request = StoredChatQuestion(roomId!!, UUID.randomUUID().toString(), message, product?.barcode)
                pending = Pending(request, chosen); unsent = null; failed = null
                turns[request.clientMessageId] = StoredChatTurn(0, request.clientMessageId, message, request.barcode, "PENDING")
                render()
                submit(account, ticket, pending!!)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (current(ticket, account)) notice("처리 결과를 확인하지 못했어요. 상태를 다시 확인해 주세요.") }
            finally { if (current(ticket, account)) _uiState.update { it.copy(isLoading = false, error = it.error?.copy(isRetrying = false)) } }
        }
    }
    private suspend fun submit(account: FamilyAccount, ticket: Long, value: Pending) {
        when (val result = store.send(account, value.question)) {
            is AuthResult.Success -> if (current(ticket, account)) accept(result.value, value)
            is AuthResult.Failure -> if (current(ticket, account)) {
                if (result.error.outcomeUnknown || result.error.code == "CHAT_REQUEST_IN_PROGRESS") {
                    resolve(account, ticket, value, resendAfterMissing = false, retrySeconds = result.error.retryDelaySeconds ?: 2)
                } else {
                    // Definite refusal; keep the failed question separate from successful AI replies.
                    pending = null; failed = value.question.message to selectedProduct
                    turns[value.question.clientMessageId] = StoredChatTurn(0, value.question.clientMessageId,
                        value.question.message, value.question.barcode, "FAILED", errorCode = result.error.code)
                    render(); showFailure(result.error)
                }
            }
        }
    }
    private fun accept(turn: StoredChatTurn, value: Pending?) {
        val freshAnswer = turn.status == "SUCCEEDED" && turns[turn.clientMessageId]?.status != "SUCCEEDED"
        turns[turn.clientMessageId] = turn
        if (turn.status == "PENDING") { render(); return }
        pending = null
        if (turn.status == "FAILED") {
            failed = turn.message to selectedProduct
            render(); showError(turn.errorCode, "다시 시도하면 새 질문으로 전송해요.".takeIf { chatErrorFor(turn.errorCode).retryable })
            return
        }
        failed = null
        val sameConditions = value?.selection?.let(targets::isCurrent) ?: false
        val candidates = turn.response?.productCandidates.orEmpty().takeIf { sameConditions }.orEmpty()
        cancelCandidatePresentation()
        candidateQuestion = turn.message.takeIf { candidates.isNotEmpty() }
        delayedCandidates = candidates; candidateSelection = value?.selection
        if (candidates.isNotEmpty()) selectedProduct = null
        render()
        if (freshAnswer) startTyping(turn.id * 2 + 1, turn.response!!.answer) else scheduleCandidatePresentation()
        // 후속 질문 칩은 방금 받은 답변에만 보여 준다(다시 연 대화·조건이 바뀐 답변·상품 후보가 온 답변에는 없음).
        val suggestions = turn.response?.recommendedQuestions.orEmpty()
            .takeIf { freshAnswer && sameConditions && candidates.isEmpty() }.orEmpty()
        _uiState.update { it.copy(error = null, productCandidates = emptyList(), canRetry = false, suggestedQuestions = suggestions,
            notice = if (sameConditions) null else "이 답변은 질문 당시의 조건으로 작성됐어요. 현재 조건은 새 질문에 적용돼요.") }
    }
    private suspend fun resolve(account: FamilyAccount, ticket: Long, value: Pending, resendAfterMissing: Boolean, retrySeconds: Long = 2) {
        val interval = retrySeconds.coerceIn(2, 10)
        repeat((120 / interval).toInt() + 1) { attempt ->
            if (!current(ticket, account)) return
            if (attempt > 0) delay(interval * 1000)
            val turnId = turns[value.question.clientMessageId]?.id?.takeIf { it > 0 }
            val lookup = if (turnId == null) store.turn(account, value.question.conversationId, value.question.clientMessageId)
                else store.turnById(account, value.question.conversationId, turnId, value.question.clientMessageId)
            when (val result = lookup) {
                is AuthResult.Success -> {
                    if (!current(ticket, account)) return
                    accept(result.value, value)
                    if (result.value.status != "PENDING") return
                }
                is AuthResult.Failure -> {
                    if (!current(ticket, account)) return
                    if (result.error.status == 404 && attempt < 2) return@repeat
                    if (result.error.status == 404 && resendAfterMissing && value.canResend && value.selection?.let(targets::isCurrent) == true) {
                        submit(account, ticket, value); return // Same UUID and body, only after explicit user retry.
                    }
                    showFailure(result.error, "질문의 저장·처리 상태를 확인하지 못했어요. 다시 시도하면 기존 질문의 상태부터 확인해요."); return
                }
            }
        }
        notice("아직 처리 결과를 확인하지 못했어요. 상태를 다시 확인해 주세요.")
    }
    fun retry() {
        if (_uiState.value.isLoading || _uiState.value.error?.retryable == false) return
        _uiState.update { it.copy(error = it.error?.copy(isRetrying = true), suggestedQuestions = emptyList()) }
        val value = pending
        if (value == null) {
            val draft = unsent ?: failed
            if (draft == null) { reloadConversation(); return }
            dispatch(draft.first, draft.second); return
        }
        val account = owner ?: return; val ticket = revision
        _uiState.update { it.copy(isLoading = true, notice = "기존 질문의 처리 상태를 확인하고 있어요.", canRetry = false) }
        job = viewModelScope.launch {
            try { resolve(account, ticket, value, resendAfterMissing = true) }
            finally { if (current(ticket, account)) _uiState.update { it.copy(isLoading = false, error = it.error?.copy(isRetrying = false)) } }
        }
    }
    fun reloadConversation() {
        if (_uiState.value.isLoading) return
        cancelCandidatePresentation()
        finishTyping()
        val id = roomId ?: return
        val account = owner ?: return; val ticket = revision
        _uiState.update { it.copy(isLoading = true, error = it.error?.takeIf { error -> error.isRetrying }, notice = null, canRetry = false,
            suggestedQuestions = emptyList()) }
        job = viewModelScope.launch {
            try {
                when (val room = store.room(account, id)) {
                    is AuthResult.Success -> if (current(ticket, account)) { roomTitle = room.value.title; watchTitle(account, ticket, room.value.title) } else return@launch
                    is AuthResult.Failure -> { if (current(ticket, account)) showFailure(room.error); return@launch }
                }
                val restored = linkedMapOf<String, StoredChatTurn>(); var after = 0L
                do {
                    val result = store.turns(account, id, after)
                    if (!current(ticket, account)) return@launch
                    if (result !is AuthResult.Success) { showFailure((result as AuthResult.Failure).error); return@launch }
                    result.value.items.forEach { restored[it.clientMessageId] = it }
                    after = result.value.nextAfterId
                } while (result.value.hasNext)
                _uiState.update { it.copy(error = null) }
                if (restored.isEmpty()) {
                    render()
                    _uiState.update { it.copy(
                        notice = if (turns.isNotEmpty()) "서버에서 대화 내용을 확인하지 못했어요. 기존 내용은 유지했으니 다시 확인해 주세요."
                            else if (unsent != null || pending != null) "대화방은 있지만 질문 저장을 확인하지 못했어요. 다시 확인·재시도를 눌러 주세요."
                            else "이 대화에는 아직 저장된 메시지가 없어요. 아래 입력창에서 질문해 주세요.",
                        canRetry = turns.isNotEmpty() || unsent != null || pending != null) }
                    return@launch
                }
                turns.clear(); turns.putAll(restored); render()
                val last = turns.values.lastOrNull()
                selectedProduct = last?.barcode?.let { ChatProductCandidate("선택한 상품", "", it, null, emptyList(), null) }
                if (last?.status == "FAILED") {
                    failed = last.message to selectedProduct
                    showError(last.errorCode, "다시 시도하면 새 질문으로 전송해요.".takeIf { chatErrorFor(last.errorCode).retryable })
                }
                last?.takeIf { it.status == "SUCCEEDED" }?.response?.productCandidates?.let { candidates ->
                    candidateQuestion = last.message
                    delayedCandidates = candidates; candidateSelection = targets.state.value.selection
                    scheduleCandidatePresentation()
                }
                turns.values.firstOrNull { it.status == "PENDING" }?.let { active ->
                    val value = Pending(StoredChatQuestion(id, active.clientMessageId, active.message, active.barcode), null, canResend = false)
                    pending = value; resolve(account, ticket, value, resendAfterMissing = false)
                }
            } finally { if (current(ticket, account)) _uiState.update { it.copy(isLoading = false, error = it.error?.copy(isRetrying = false)) } }
        }
    }
    private fun watchTitle(account: FamilyAccount, ticket: Long, serverTitle: String) {
        val id = roomId ?: return
        titleJob?.cancel()
        titleJob = viewModelScope.launch {
            chatHistoryRepository.observeTitle(id.toString(), serverTitle).collect { title ->
                if (current(ticket, account)) { roomTitle = title; _uiState.update { it.copy(title = title) } }
            }
        }
    }
    private fun cancelCandidatePresentation() {
        candidateJob?.cancel(); candidateJob = null; delayedCandidates = emptyList(); candidateSelection = null
    }
    private fun scheduleCandidatePresentation() {
        val candidates = delayedCandidates.takeIf { it.isNotEmpty() } ?: return
        val chosen = candidateSelection ?: return
        val account = owner ?: return
        val ticket = revision
        candidateJob?.cancel()
        candidateJob = viewModelScope.launch {
            delay(1_500)
            if (current(ticket, account) && targets.isCurrent(chosen) && typingMessageId == null) {
                _uiState.update { it.copy(productCandidates = candidates) }
            }
        }
    }
    fun finishTyping() {
        val wasTyping = typingMessageId != null
        typingJob?.cancel(); typingJob = null; typingMessageId = null; typingText = null
        render()
        if (wasTyping) scheduleCandidatePresentation()
    }
    private fun startTyping(messageId: Long, answer: String) {
        typingJob?.cancel(); typingJob = null; typingMessageId = null; typingText = null
        if (answer.isEmpty()) { scheduleCandidatePresentation(); return }
        val ticket = revision
        val account = owner
        typingMessageId = messageId; typingText = ""
        render()
        typingJob = viewModelScope.launch {
            for (end in typingFrameEnds(answer)) {
                delay(20)
                if (ticket != revision || account != owner || store.captureAccount() != account) return@launch
                typingText = answer.substring(0, end)
                render()
            }
            typingMessageId = null; typingText = null; typingJob = null
            render()
            scheduleCandidatePresentation()
        }
    }
    private fun render() {
        val messages = turns.values.flatMapIndexed { index, turn ->
            val base = if (turn.id > 0) turn.id * 2 else -2L * (index + 1)
            listOf(ChatMessageUiModel(base, turn.message, true)) + when (turn.status) {
                "SUCCEEDED" -> listOf(ChatMessageUiModel(base + 1, turn.response!!.answer, false,
                    displayedText = typingText.takeIf { typingMessageId == base + 1 }, isTyping = typingMessageId == base + 1))
                "FAILED" -> emptyList()
                else -> emptyList()
            }
        }
        val displayed = messages + unsent?.let { listOf(ChatMessageUiModel(Long.MIN_VALUE, it.first, true)) }.orEmpty()
        _uiState.update { it.copy(messages = displayed, title = roomTitle) }
    }
    private fun showFailure(failure: AuthFailure, description: String? = null) {
        val code = when {
            failure.status == 401 -> ChatErrorCodes.AUTHENTICATION_REQUIRED
            failure.kind == AuthFailureKind.Network -> ChatErrorCodes.NETWORK_UNAVAILABLE
            else -> failure.code
        }
        showError(code, description)
    }
    private fun showError(code: String?, description: String? = null) {
        val error = chatErrorFor(code).let { if (description == null) it else it.copy(description = description) }
        _uiState.update { it.copy(error = error, notice = null, canRetry = error.retryable, suggestedQuestions = emptyList()) }
    }
    private fun notice(message: String) { showError(ChatErrorCodes.CHAT_REQUEST_FAILED, message) }
}
fun createChatTitle(message: String): String = message.trim().replace(Regex("\\s+"), " ").let { if (it.isEmpty()) "새 대화" else it.take(100) }
