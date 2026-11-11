package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Account-scoped server history; old ownerless Room rows are neither uploaded nor read. */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatHistoryRepositoryImpl @Inject constructor(private val remote: StoredChatStore,
    private val sessions: AuthSessionManager,
    private val display: com.coworker.jjikmuk.data.local.preference.ChatDisplayPreferences) : ChatHistoryRepository {
    private data class Snapshot(val account: FamilyAccount? = null, val items: List<ChatConversation> = emptyList(),
        val page: Int = 0, val hasNext: Boolean = false, val loading: Boolean = false, val message: String? = null)
    private val snapshot = MutableStateFlow(Snapshot())
    private val mutableState = MutableStateFlow(ChatHistoryState())
    override val state: StateFlow<ChatHistoryState> = mutableState.asStateFlow()
    private val mutex = Mutex()
    override fun observeConversations(): Flow<List<ChatConversation>> = sessions.state.map { remote.captureAccount() }.distinctUntilChanged()
        .flatMapLatest { account -> flow {
            mutableState.value = ChatHistoryState(account, loading = account != null)
            emit(emptyList())
            if (account != null) {
                refresh()
                emitAll(snapshot.filter { it.account == account }.combine(display.observe(account.userId)) { data, overrides ->
                    if (remote.captureAccount() != account) emptyList() else data.items.map { room ->
                        val local = overrides[room.id]
                        room.copy(title = local?.title ?: room.title, isPinned = local?.pinned ?: false)
                    }.sortedWith(compareByDescending<ChatConversation> { it.isPinned }.thenByDescending { it.updatedAt })
                }.catch { error ->
                    if (error is kotlinx.coroutines.CancellationException) throw error
                    if (remote.captureAccount() == account) mutableState.value = ChatHistoryState(account,
                        message = "이 기기의 대화 표시 설정을 읽지 못했어요. 다시 확인해 주세요.")
                    emit(emptyList())
                })
            }
        } }
    override fun observeConversations(query: String) = observeConversations().map { values ->
        values.filter { it.title.contains(query, ignoreCase = true) || it.preview.contains(query, ignoreCase = true) }
    }
    override suspend fun refresh() = fetch(false)
    override suspend fun loadMore() = fetch(true)
    private suspend fun fetch(more: Boolean) = mutex.withLock {
        val account = remote.captureAccount() ?: return@withLock
        val before = snapshot.value.takeIf { it.account == account } ?: Snapshot(account)
        if (more && !before.hasNext) return@withLock
        val page = if (more) before.page + 1 else 0
        mutableState.value = ChatHistoryState(account, loading = true, hasNext = before.hasNext)
        when (val result = remote.rooms(account, page)) {
            is AuthResult.Success -> if (remote.captureAccount() == account) {
                val items = ((if (more) before.items else emptyList()) + result.value.items).distinctBy { it.id }.toMutableList()
                val overrides = try { display.observe(account.userId).first() } catch (_: java.io.IOException) { emptyMap() }
                var pinReadFailed = false
                // A pinned old room may no longer be in the server's first activity page.
                for ((id, local) in overrides) {
                    if (!local.pinned || items.any { it.id == id }) continue
                    when (val pinned = remote.room(account, id.toLong())) {
                        is AuthResult.Success -> items.add(pinned.value)
                        is AuthResult.Failure -> {
                            if (pinned.error.status == 404) {
                                try { display.remove(account.userId, id) } catch (_: java.io.IOException) { pinReadFailed = true }
                            } else pinReadFailed = true
                        }
                    }
                    if (remote.captureAccount() != account) return@withLock
                }
                snapshot.value = Snapshot(account, items.distinctBy { it.id }, page, result.value.hasNext)
                mutableState.value = ChatHistoryState(account, hasNext = result.value.hasNext,
                    message = if (pinReadFailed) "일부 고정 대화를 확인하지 못했어요. 다시 확인해 주세요." else null)
            }
            is AuthResult.Failure -> if (remote.captureAccount() == account) {
                snapshot.value = before
                mutableState.value = ChatHistoryState(account, message = result.error.userMessage(), hasNext = before.hasNext)
            }
        }
    }
    override fun observeMessages(conversationId: String): Flow<List<ChatSavedMessage>> = flow {
        val account = remote.captureAccount() ?: return@flow
        val id = conversationId.toLongOrNull() ?: return@flow
        val values = mutableListOf<ChatSavedMessage>(); var after = 0L
        do {
            val result = remote.turns(account, id, after)
            if (result !is AuthResult.Success || remote.captureAccount() != account) return@flow
            result.value.items.forEach { turn ->
                values += ChatSavedMessage(turn.id * 2, turn.message, true)
                turn.response?.let { values += ChatSavedMessage(turn.id * 2 + 1, it.answer, false) }
            }
            after = result.value.nextAfterId
        } while (result.value.hasNext)
        emit(values)
    }
    override fun observeTitle(conversationId: String, serverTitle: String): Flow<String> = sessions.state
        .map { remote.captureAccount() }.distinctUntilChanged().flatMapLatest { account ->
            if (account == null) flowOf(serverTitle) else display.observe(account.userId).map {
                if (remote.captureAccount() == account) it[conversationId]?.title ?: serverTitle else serverTitle
            }.catch { error ->
                if (error is kotlinx.coroutines.CancellationException) throw error
                emit(serverTitle)
            }
        }
    override suspend fun deleteConversation(conversationId: String) {
        val account = remote.captureAccount() ?: return
        val id = conversationId.toLongOrNull() ?: return
        when (val result = remote.delete(account, id)) {
            is AuthResult.Success -> if (remote.captureAccount() == account) {
                try { display.remove(account.userId, conversationId) } catch (_: java.io.IOException) { /* Stale display overrides cannot recreate a deleted server room. */ }
                refresh()
            }
            is AuthResult.Failure -> if (remote.captureAccount() == account)
                mutableState.value = ChatHistoryState(account, message = result.error.userMessage())
        }
    }
    override suspend fun deleteConversations(conversationIds: List<String>) {
        val owner = remote.captureAccount() ?: return
        for (id in conversationIds.distinct()) {
            if (remote.captureAccount() != owner) return
            deleteConversation(id)
            if (state.value.message != null) return
        }
    }
    override suspend fun updatePinned(conversationId: String, isPinned: Boolean) = updateDisplay(conversationId) { account ->
        display.pin(account.userId, conversationId, isPinned)
    }
    override suspend fun updateTitle(conversationId: String, title: String) = updateDisplay(conversationId) { account ->
        display.title(account.userId, conversationId, title)
    }
    private suspend fun updateDisplay(id: String, action: suspend (FamilyAccount) -> Unit) {
        val account = remote.captureAccount() ?: return
        if (snapshot.value.account != account || snapshot.value.items.none { it.id == id }) return
        try {
            action(account)
            if (remote.captureAccount() == account) mutableState.value = state.value.copy(message = null)
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (_: Exception) {
            if (remote.captureAccount() == account) mutableState.value = state.value.copy(message = "이 기기의 대화 표시 설정을 저장하지 못했어요.")
        }
    }
    override suspend fun createConversation(conversationId: String, title: String, preview: String) = error("Use server conversation creation")
    override suspend fun addMessage(conversationId: String, text: String, isMine: Boolean): Long = error("Turns are stored by POST /api/chat")
}
