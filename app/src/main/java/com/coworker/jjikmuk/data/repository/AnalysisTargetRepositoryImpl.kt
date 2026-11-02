package com.coworker.jjikmuk.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private val Context.analysisTargets by preferencesDataStore("analysis_targets")

/** Only IDs are persisted, per account. Health conditions are always resolved by the backend. */
@Singleton
@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisTargetRepositoryImpl internal constructor(private val storage: DataStore<Preferences>,
    private val sessions: AuthSessionManager, private val families: FamilyProfileRepository, scope: CoroutineScope) : AnalysisTargetRepository {
    @Inject constructor(@ApplicationContext context: Context, sessions: AuthSessionManager, families: FamilyProfileRepository) :
        this(context.analysisTargets, sessions, families, CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate))
    private val writes = Mutex()
    private val writeError = MutableStateFlow<String?>(null)
    private data class Saved(val ids: Set<String>? = null, val failed: Boolean = false)
    private data class Projection(val state: AnalysisTargetsState, val original: Set<String>? = null, val prune: Set<String>? = null)
    private fun key(id: Long) = stringSetPreferencesKey("account_$id")
    override fun captureAccount(): FamilyAccount? = sessions.state.value.let {
        it.credentials?.takeIf { _ -> it.canEnterHome }?.let { credentials -> FamilyAccount(credentials.userId, it.generation) }
    }
    override val state: StateFlow<AnalysisTargetsState> = sessions.state.map { captureAccount() }.distinctUntilChanged().onEach { writeError.value = null }
        .flatMapLatest { account ->
            if (account == null) flowOf(AnalysisTargetsState()) else {
                val saved = storage.data.map { Saved(it[key(account.userId)]) }
                    .catch { error -> if (error is IOException) emit(Saved(failed = true)) else throw error }
                families.observeState().combine(saved) { family, stored ->
                    if (captureAccount() != account || family.account != account) return@combine Projection(AnalysisTargetsState(account))
                    if (stored.failed) return@combine Projection(AnalysisTargetsState(account, family.profiles,
                        message = "대상 선택을 읽지 못했어요. 저장소 상태를 확인해 주세요."))
                    val ids = stored.ids ?: setOf("me") // First login: self only. An explicitly empty set stays none.
                    val valid = family.profiles.filter { it.isMe || (it.serverId ?: 0) > 0 }.map { it.id }.toSet()
                    val selected = ids.intersect(valid)
                    val active = family.profiles.filter { it.id in selected }
                    val self = active.any { it.isMe }
                    val familyIds = active.filterNot { it.isMe }.mapNotNull { it.serverId }.distinct().sorted()
                    val profile = sessions.state.value.profile.takeIf { self }
                    val revision = active.sortedBy { it.id }.map {
                        listOf(it.id, it.version, it.name, it.vegetarian, it.allergies.sorted(), it.preferences.sorted(), it.diseases, it.dislikedIngredients)
                    }.toString() + listOf(profile?.allergies, profile?.specialDiet, profile?.diseases, profile?.dislikedIngredients)
                    val digest = MessageDigest.getInstance("SHA-256").digest(revision.toByteArray()).joinToString("") { "%02x".format(it) }
                    val snapshot = if (family.loaded && !family.needsReload) AnalysisTargetSelection(account, self, familyIds, digest) else null
                    Projection(AnalysisTargetsState(account, family.profiles, selected, snapshot,
                        ready = snapshot != null && !family.busy, message = family.message.takeIf { family.needsReload || !family.loaded }),
                        stored.ids, selected.takeIf { family.loaded && !family.needsReload && !family.busy && stored.ids != null && stored.ids != selected })
                }.transform { projection ->
                    emit(projection.state)
                    projection.prune?.let { clean ->
                        try { storage.edit { preferences ->
                            if (captureAccount() == account && preferences[key(account.userId)] == projection.original)
                                preferences[key(account.userId)] = clean
                        } } catch (_: IOException) { writeError.value = "대상 선택을 정리하지 못했어요. 다시 선택해 주세요." }
                    }
                }
            }
        }.combine(writeError) { value, error -> if (error == null) value else value.copy(ready = false, message = error) }
        .stateIn(scope, SharingStarted.Eagerly, AnalysisTargetsState())

    override suspend fun setSelected(id: String, checked: Boolean, account: FamilyAccount?): Boolean = writes.withLock {
        val current = state.value
        if (account == null || captureAccount() != account || current.account != account ||
            (!current.ready && writeError.value == null) || current.profiles.none { it.id == id }) return@withLock false
        writeError.value = null
        try {
            var expected: Set<String>? = null
            storage.edit { preferences ->
                if (captureAccount() != account) return@edit
                val valid = state.value.profiles.map { it.id }.toSet()
                val previous = (preferences[key(account.userId)] ?: setOf("me")).intersect(valid)
                expected = if (checked) previous + id else previous - id
                preferences[key(account.userId)] = expected!!
            }
            val desired = expected ?: return@withLock false
            val observed = withTimeoutOrNull(5_000) { state.first { updated ->
                captureAccount() != account || (updated.account == account &&
                    updated.selectedProfileIds == desired.intersect(updated.profiles.map { it.id }.toSet()))
            } }
            if (captureAccount() != account) return@withLock false
            if (observed == null) writeError.value = "대상 선택 저장을 확인하지 못했어요. 다시 선택해 주세요."
            observed != null
        } catch (_: IOException) {
            writeError.value = "대상 선택을 저장하지 못했어요. 다시 선택해 주세요."
            false
        }
    }
    override suspend fun awaitSelection(account: FamilyAccount?): AnalysisTargetSelection? = writes.withLock {
        if (account == null || captureAccount() != account) return@withLock null
        val resolved = withTimeoutOrNull(35_000) { state.first {
            captureAccount() != account || (it.account == account && (it.ready || it.message != null))
        } } ?: return@withLock null
        resolved.selection?.takeIf { resolved.ready && it.account == account && captureAccount() == account }
    }
    override fun isCurrent(selection: AnalysisTargetSelection) = captureAccount() == selection.account && state.value.selection == selection
}
