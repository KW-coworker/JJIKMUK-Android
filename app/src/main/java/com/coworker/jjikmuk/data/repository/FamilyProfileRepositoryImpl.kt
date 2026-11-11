package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.domain.FamilyProfile as ServerFamily
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Server source of truth. Ownerless Room data is never uploaded, displayed, or deleted implicitly. */
@OptIn(ExperimentalCoroutinesApi::class)
class FamilyProfileRepositoryImpl internal constructor(private val remote: FamilyRemote,
    private val sessions: AuthSessionManager) : FamilyProfileRepository {
    @Inject constructor(remote: HttpFamilyRemote, sessions: AuthSessionManager) : this(remote as FamilyRemote, sessions)
    private val mutableState = MutableStateFlow(FamilyProfilesState(loaded = false))
    private val mutex = Mutex()
    override fun captureAccount(): FamilyAccount? = sessions.state.value.let { state ->
        state.credentials?.takeIf { state.canEnterHome }?.let { FamilyAccount(it.userId, state.generation) }
    }
    override fun currentState(): FamilyProfilesState? = mutableState.value.takeIf { it.account == captureAccount() }
    private fun current(account: FamilyAccount?) = account != null && captureAccount() == account
    private fun snapshot(account: FamilyAccount) = mutableState.value.takeIf { it.account == account }
        ?: FamilyProfilesState(account, loaded = false)

    override fun observeState(): Flow<FamilyProfilesState> = sessions.state.map { captureAccount() }.distinctUntilChanged()
        .flatMapLatest { account ->
            if (account == null) flowOf(FamilyProfilesState(loaded = false)) else flow {
                emit(FamilyProfilesState(account, loaded = false, busy = true))
                refresh(account)
                emitAll(mutableState.filter { it.account == account }.combine(sessions.state) { data, session ->
                    val profile = session.profile?.takeIf { it.id == account.userId && session.generation == account.generation }
                    if (profile == null) data else data.copy(profiles = listOf(self(profile)) + data.profiles.filterNot { it.isMe })
                })
            }
        }
    override fun observeProfiles(): Flow<List<FamilyProfile>> = observeState().map { it.profiles }.distinctUntilChanged()
    override suspend fun ensureDefaultProfiles() { refresh() } // Reads GET me; never creates a fake self/family.

    override suspend fun refresh(account: FamilyAccount?) = mutex.withLock {
        if (!current(account)) return@withLock
        account!!
        val before = snapshot(account)
        mutableState.value = before.copy(busy = true, message = null)
        try {
            val me = remote.me(account)
            if (!current(account)) return@withLock
            val families = remote.list(account)
            if (!current(account)) return@withLock
            if (me is AuthResult.Success && me.value.id == account.userId && families is AuthResult.Success) {
                mutableState.value = FamilyProfilesState(account,
                    listOf(self(me.value)) + families.value.map(::family), loaded = true)
            } else {
                val error = (me as? AuthResult.Failure)?.error ?: (families as? AuthResult.Failure)?.error
                mutableState.value = before.copy(busy = false, needsReload = true,
                    message = error?.let(::message) ?: "가족 정보를 확인하지 못했어요. 다시 불러와 주세요.")
            }
        } finally {
            if (current(account)) mutableState.update { it.copy(busy = false) }
        }
    }

    override suspend fun addProfile(name: String, emoji: String, relation: String, account: FamilyAccount?): Boolean = mutate(account) { before ->
        val normalized = normalizeAuthNickname(name)
        if (!Regex("^[가-힣A-Za-z0-9_]{2,20}$").matches(normalized)) return@mutate rejected("이름은 한글·영문·숫자·밑줄로 2~20자 입력해 주세요.")
        if (before.profiles.count { !it.isMe } >= 20) return@mutate rejected("가족은 본인 제외 최대 20명까지 등록할 수 있어요.")
        when (val result = remote.create(account!!, FamilySubmission(normalized, relation = relation, emoji = emoji))) {
            is AuthResult.Success -> Changed(before.profiles + family(result.value))
            is AuthResult.Failure -> Failed(result.error)
        }
    }

    override suspend fun updateProfile(profileId: String, name: String, emoji: String, relation: String,
        version: Long?, account: FamilyAccount?): Boolean = mutate(account) { before ->
        val member = before.profiles.firstOrNull { it.id == profileId && !it.isMe } ?: return@mutate rejected("가족 목록을 다시 불러와 주세요.")
        val id = member.serverId ?: return@mutate rejected("가족 목록을 다시 불러와 주세요.")
        val normalized = normalizeAuthNickname(name)
        if (!Regex("^[가-힣A-Za-z0-9_]{2,20}$").matches(normalized)) return@mutate rejected("이름은 한글·영문·숫자·밑줄로 2~20자 입력해 주세요.")
        val patch = FamilyPatch(ProfilePatch(nickname = normalized.takeIf { it != member.name }),
            field(relation != member.relation, relation), field(emoji != member.emoji, emoji))
        patch(account!!, id, version ?: member.version ?: return@mutate rejected("가족 목록을 다시 불러와 주세요."), patch, before)
    }

    override suspend fun updateDietCondition(profileId: String, vegetarian: String, allergies: Set<String>, preferences: Set<String>,
        version: Long?, account: FamilyAccount?): Boolean = mutate(account) { before ->
        val member = before.profiles.firstOrNull { it.id == profileId && !it.isMe } ?: return@mutate rejected("본인 정보는 본인 식이조건 화면에서 수정해 주세요.")
        if (!member.conditionsSupported) return@mutate rejected("현재 앱에서 표시할 수 없는 조건이 있어요. 기존 정보를 유지하려면 앱을 업데이트해 주세요.")
        val encoded = FamilyConditions.encode(vegetarian, allergies, preferences) ?: return@mutate rejected("선택한 식이조건을 확인해 주세요.")
        val input = FamilyPatch(ProfilePatch(
            allergies = field(allergies != member.allergies, encoded.first),
            specialDiet = field(vegetarian != member.vegetarian || preferences != member.preferences, encoded.second)))
        patch(account!!, member.serverId ?: return@mutate rejected("가족 목록을 다시 불러와 주세요."),
            version ?: member.version ?: return@mutate rejected("가족 목록을 다시 불러와 주세요."), input, before)
    }

    override suspend fun deleteProfile(profileId: String, version: Long?, account: FamilyAccount?): Boolean = mutate(account) { before ->
        val member = before.profiles.firstOrNull { it.id == profileId && !it.isMe } ?: return@mutate rejected("가족 목록을 다시 불러와 주세요.")
        when (val result = remote.delete(account!!, member.serverId ?: return@mutate rejected("가족 목록을 다시 불러와 주세요."),
            version ?: member.version ?: return@mutate rejected("가족 목록을 다시 불러와 주세요."))) {
            is AuthResult.Success -> Changed(before.profiles.filterNot { it.id == profileId })
            is AuthResult.Failure -> Failed(result.error)
        }
    }

    private suspend fun patch(account: FamilyAccount, id: Long, version: Long, input: FamilyPatch, before: FamilyProfilesState): Change =
        when (val result = remote.patch(account, id, version, input)) {
            is AuthResult.Success -> Changed(before.profiles.map { if (it.serverId == id) family(result.value) else it })
            is AuthResult.Failure -> Failed(result.error)
        }
    private sealed interface Change
    private class Changed(val profiles: List<FamilyProfile>) : Change
    private class Failed(val error: AuthFailure) : Change
    private class Rejected(val text: String) : Change
    private fun rejected(text: String): Change = Rejected(text)
    private suspend fun mutate(account: FamilyAccount?, action: suspend (FamilyProfilesState) -> Change): Boolean {
        if (!current(account) || !mutex.tryLock()) return false
        account!!
        try {
            if (!current(account)) return false
            val before = snapshot(account)
            if (!before.loaded || before.needsReload) {
                mutableState.value = before.copy(message = "가족 목록을 다시 불러온 뒤 저장해 주세요.")
                return false
            }
            mutableState.value = before.copy(busy = true, message = null)
            val result = action(before)
            if (!current(account)) return false
            when (result) {
                is Changed -> mutableState.value = before.copy(profiles = result.profiles, busy = false, message = "가족 정보를 저장했어요.")
                is Rejected -> mutableState.value = before.copy(busy = false, message = result.text)
                is Failed -> mutableState.value = before.copy(busy = false,
                    needsReload = result.error.outcomeUnknown || result.error.status in setOf(404, 409) || result.error.code == "AUTH_REFRESHED_RETRY_REQUIRED",
                    message = message(result.error))
            }
            return result is Changed
        } catch (cancelled: CancellationException) {
            if (current(account)) mutableState.value = snapshot(account).copy(busy = false, needsReload = true,
                message = "저장 결과를 확인해야 해요. 목록을 다시 불러와 확인해 주세요.")
            throw cancelled
        } finally {
            if (current(account)) mutableState.update { it.copy(busy = false) }
            mutex.unlock()
        }
    }
    private fun field(changed: Boolean, value: String?): ProfileField = when {
        !changed -> ProfileField.Unchanged
        value.isNullOrBlank() -> ProfileField.Clear
        else -> ProfileField.Value(value)
    }
    private fun message(error: AuthFailure): String = when (error.code) {
        "FAMILY_LIMIT_EXCEEDED" -> "가족은 본인 제외 최대 20명까지 등록할 수 있어요."
        "PROFILE_VERSION_CONFLICT" -> "다른 기기에서 수정된 정보가 있어요. 목록을 다시 불러온 뒤 수정해 주세요."
        "FAMILY_NOT_FOUND" -> "삭제되었거나 조회할 수 없는 가족이에요. 목록을 다시 불러와 주세요."
        "AUTHENTICATION_REQUIRED", "AUTH_TOKEN_INVALID", "AUTH_TOKEN_EXPIRED" -> "로그인 상태를 확인해 주세요."
        else -> if (error.outcomeUnknown) "서버 응답을 확인하지 못했어요. 목록을 다시 불러와 저장 여부를 확인해 주세요."
            else "가족 정보를 저장하거나 불러오지 못했어요. 입력 내용과 연결 상태를 확인해 주세요."
    }
    private fun family(value: ServerFamily): FamilyProfile {
        val conditions = FamilyConditions.decode(value.allergies, value.specialDiet)
        return FamilyProfile(value.id.toString(), value.nickname, value.emoji ?: "🙂", value.relation ?: "가족", false,
            conditions.vegetarian, conditions.allergies, conditions.preferences, value.id, value.version,
            conditions.supported, value.diseases, value.dislikedIngredients)
    }
    private fun self(value: AuthProfile): FamilyProfile {
        val conditions = FamilyConditions.decode(value.allergies, value.specialDiet)
        return FamilyProfile("me", value.nickname, "🙂", "본인", true, conditions.vegetarian, conditions.allergies,
            conditions.preferences, conditionsSupported = conditions.supported, diseases = value.diseases, dislikedIngredients = value.dislikedIngredients)
    }
}
