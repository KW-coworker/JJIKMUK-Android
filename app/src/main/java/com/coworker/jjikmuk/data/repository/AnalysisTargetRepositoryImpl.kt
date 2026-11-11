package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Server selection only. Legacy device selections are never uploaded or used as a fallback. */
@Singleton
class AnalysisTargetRepositoryImpl internal constructor(private val remote: AnalysisSelectionRemote,
    private val sessions: AuthSessionManager, private val families: FamilyProfileRepository, scope: CoroutineScope) : AnalysisTargetRepository {
    @Inject constructor(remote: HttpAnalysisSelectionRemote, sessions: AuthSessionManager, families: FamilyProfileRepository) :
        this(remote as AnalysisSelectionRemote, sessions, families, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    private val writes = Mutex()
    private data class Saved(val account: FamilyAccount? = null, val data: SavedAnalysisSelection? = null,
        val busy: Boolean = false, val failed: Boolean = false, val confirm: Boolean = false, val message: String? = null)
    private val saved = MutableStateFlow(Saved())
    private val familyState = MutableStateFlow(FamilyProfilesState(loaded = false))
    override fun captureAccount(): FamilyAccount? = sessions.state.value.let {
        it.credentials?.takeIf { _ -> it.canEnterHome }?.let { c -> FamilyAccount(c.userId, it.generation) }
    }
    override val state: StateFlow<AnalysisTargetsState> = combine(familyState, saved, sessions.state) { family, stored, _ ->
        val account = captureAccount() ?: return@combine AnalysisTargetsState()
        if (family.account != account) return@combine AnalysisTargetsState(account, message = "가족과 분석 대상을 불러오고 있어요.")
        val value = stored.takeIf { it.account == account }
        val data = value?.data
        val selected = family.profiles.filter { p -> if (p.isMe) data?.includeSelf == true else p.serverId in data?.familyProfileIds.orEmpty() }
        val validIds = family.profiles.mapNotNull { it.serverId }.toSet()
        val consistent = data != null && data.familyProfileIds.all { it in validIds }
        val ready = family.loaded && !family.needsReload && !family.busy &&
            value != null && !value.busy && !value.failed && !value.confirm && consistent
        val revision = selected.sortedBy { it.id }.map {
            listOf(it.id, it.version, it.name, it.vegetarian, it.allergies.sorted(), it.preferences.sorted(), it.diseases, it.dislikedIngredients)
        }.toString() + if (data?.includeSelf == true) sessions.state.value.profile?.let {
            listOf(it.version, it.allergies, it.specialDiet, it.diseases, it.dislikedIngredients).toString()
        }.orEmpty() else ""
        val digest = MessageDigest.getInstance("SHA-256").digest(revision.toByteArray()).joinToString("") { "%02x".format(it) }
        val selection = if (ready) AnalysisTargetSelection(account, data!!.includeSelf, data.familyProfileIds, digest) else null
        val message = when {
            family.needsReload -> family.message ?: "가족 정보를 다시 불러와 주세요."
            value?.message != null -> value.message
            data != null && !consistent -> "가족과 분석 대상이 달라요. 최신 정보를 다시 확인해 주세요."
            !ready -> "분석 대상을 불러오고 있어요."
            else -> null
        }
        AnalysisTargetsState(account, family.profiles, selected.map { it.id }.toSet(), selection, ready,
            message, data?.version, value?.confirm == true)
    }.stateIn(scope, SharingStarted.Eagerly, AnalysisTargetsState())

    init {
        scope.launch {
            var previous: String? = null
            families.observeState().collect { family ->
                familyState.value = family
                val account = captureAccount()
                if (account == null) { saved.value = Saved(); previous = null }
                else if (family.account == account && family.loaded && !family.busy && !family.needsReload) {
                    val signature = account.toString() + family.profiles.toString()
                    if (signature != previous) {
                        previous = signature
                        writes.withLock { load(account, confirm = false) }
                    }
                }
            }
        }
    }

    private fun current(account: FamilyAccount?) = account != null && captureAccount() == account
    private suspend fun load(account: FamilyAccount, confirm: Boolean) {
        if (!current(account)) return
        val before = saved.value.takeIf { it.account == account } ?: Saved(account)
        saved.value = before.copy(busy = true, message = "분석 대상을 불러오고 있어요.")
        when (val result = remote.get(account)) {
            is AuthResult.Success -> if (current(account)) saved.value = Saved(account, result.value,
                confirm = !confirm && before.confirm,
                message = if (!confirm && before.confirm) "다른 기기에서 선택이 변경됐어요. 최신 선택을 확인해 주세요." else null)
            is AuthResult.Failure -> if (current(account)) saved.value = before.copy(busy = false, failed = true,
                message = "분석 대상을 불러오지 못했어요. 다시 확인해 주세요.")
        }
    }

    override suspend fun refresh(account: FamilyAccount?, confirmChanges: Boolean) {
        if (!current(account)) return
        families.refresh(account)
        // Publish the completed read before awaiting the asynchronously collected combined state.
        families.currentState()?.takeIf { it.account == account }?.let { familyState.value = it }
        writes.withLock { if (current(account)) load(account!!, confirm = confirmChanges) }
    }

    override suspend fun setSelected(id: String, checked: Boolean, account: FamilyAccount?): Boolean = writes.withLock {
        if (!current(account)) return@withLock false
        val before = saved.value.takeIf { it.account == account } ?: return@withLock false
        val data = before.data ?: return@withLock false
        val family = familyState.value
        if (before.failed || before.confirm || family.account != account || !family.loaded || family.needsReload || family.busy)
            return@withLock false
        val member = family.profiles.firstOrNull { it.id == id } ?: return@withLock false
        val self = if (member.isMe) checked else data.includeSelf
        val ids = if (member.isMe) data.familyProfileIds else {
            val serverId = member.serverId ?: return@withLock false
            (if (checked) data.familyProfileIds + serverId else data.familyProfileIds - serverId).distinct().sorted()
        }
        if (self == data.includeSelf && ids == data.familyProfileIds) return@withLock true
        saved.value = before.copy(busy = true, message = "분석 대상을 저장하고 있어요.")
        try {
            when (val result = remote.put(account!!, data.version, self, ids)) {
                is AuthResult.Success -> {
                    if (!current(account)) return@withLock false
                    if (result.value.version <= data.version || result.value.includeSelf != self || result.value.familyProfileIds != ids) {
                        saved.value = before.copy(failed = true, busy = false, message = "저장 결과를 확인하지 못했어요. 다시 확인해 주세요.")
                        return@withLock false
                    }
                    saved.value = Saved(account, result.value)
                    withTimeoutOrNull(5_000) { state.first { it.account != account || (it.serverVersion == result.value.version && it.ready) } }
                    current(account)
                }
                is AuthResult.Failure -> {
                    if (!current(account)) return@withLock false
                    val conflict = result.error.code == "ANALYSIS_SELECTION_VERSION_CONFLICT"
                    saved.value = before.copy(busy = false, failed = true, confirm = conflict,
                        message = if (conflict) "다른 기기에서 선택이 변경됐어요. 최신 선택을 확인해 주세요."
                            else "저장 결과를 확인하지 못했어요. 다시 확인해 주세요.")
                    if (conflict) load(account, confirm = false) // Read only; never retry the PUT with a new version.
                    false
                }
            }
        } catch (cancelled: CancellationException) {
            if (current(account)) saved.value = before.copy(busy = false, failed = true,
                message = "저장 결과를 확인해야 해요. 다시 확인해 주세요.")
            throw cancelled
        }
    }
    override suspend fun awaitSelection(account: FamilyAccount?): AnalysisTargetSelection? {
        if (!current(account)) return null
        // Wait for an already-dispatched write, without holding the mutex while awaiting initial reads.
        writes.withLock { if (!current(account)) return null }
        val resolved = withTimeoutOrNull(35_000) { state.first {
            !current(account) || (it.account == account && (it.ready || saved.value.failed || saved.value.confirm || familyState.value.needsReload))
        } } ?: return null
        return resolved.selection?.takeIf { resolved.ready && current(account) }
    }
    override fun isCurrent(selection: AnalysisTargetSelection) = current(selection.account) &&
        !saved.value.busy && !saved.value.failed && !saved.value.confirm && state.value.ready && state.value.selection == selection
}
