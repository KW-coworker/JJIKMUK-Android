package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.domain.repository.FamilyAccount
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import javax.inject.Inject

internal interface FamilyRemote {
    suspend fun me(account: FamilyAccount): AuthResult<AuthProfile>
    suspend fun list(account: FamilyAccount): AuthResult<List<FamilyProfile>>
    suspend fun create(account: FamilyAccount, input: FamilySubmission): AuthResult<FamilyProfile>
    suspend fun patch(account: FamilyAccount, id: Long, version: Long, input: FamilyPatch): AuthResult<FamilyProfile>
    suspend fun delete(account: FamilyAccount, id: Long, version: Long): AuthResult<Unit>
}
class HttpFamilyRemote @Inject constructor(private val families: AuthFamilies,
    private val profiles: AuthProfiles, private val sessions: AuthSessionManager) : FamilyRemote {
    override suspend fun me(account: FamilyAccount) = guarded(account) { profiles.load() }
    override suspend fun list(account: FamilyAccount) = guarded(account) { families.list(account.generation) }
    override suspend fun create(account: FamilyAccount, input: FamilySubmission) = guarded(account) { families.create(input, account.generation) }
    override suspend fun patch(account: FamilyAccount, id: Long, version: Long, input: FamilyPatch) = guarded(account) { families.patch(id, version, input, account.generation) }
    override suspend fun delete(account: FamilyAccount, id: Long, version: Long) = guarded(account) { families.delete(id, version, account.generation) }
    private suspend fun <T> guarded(account: FamilyAccount, block: suspend () -> AuthResult<T>): AuthResult<T> {
        fun current() = sessions.state.value.let { it.canEnterHome && it.credentials?.userId == account.userId && it.generation == account.generation }
        fun changed() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, code = "SESSION_CHANGED"))
        if (!current()) return changed()
        val result = block()
        return if (current()) result else changed()
    }
}
