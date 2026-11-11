package com.coworker.jjikmuk.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class AnalysisTargetRepositoryTest {
    private val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
    private class Families : FamilyProfileRepository {
        val values = MutableStateFlow(FamilyProfilesState(loaded = false))
        override fun observeState() = values
        override fun observeProfiles() = values.map { it.profiles }
        override suspend fun ensureDefaultProfiles() = Unit
        override suspend fun addProfile(name: String, emoji: String, relation: String, account: FamilyAccount?) = error("unused")
        override suspend fun updateProfile(profileId: String, name: String, emoji: String, relation: String, version: Long?, account: FamilyAccount?) = error("unused")
        override suspend fun updateDietCondition(profileId: String, vegetarian: String, allergies: Set<String>, preferences: Set<String>, version: Long?, account: FamilyAccount?) = error("unused")
        override suspend fun deleteProfile(profileId: String, version: Long?, account: FamilyAccount?) = error("unused")
    }
    private val families = Families()
    private fun member(id: Long) = FamilyProfile(id.toString(), "가족$id", "🙂", "가족", false,
        "해당 없음", setOf("우유"), emptySet(), id, 0)
    private suspend fun login(id: Long = 1) {
        val attempt = sessions.beginLogin()
        check(sessions.acceptLogin(attempt, AuthLogin("access-$id", id, "$id@example.com", "unused", 900, true), 1000))
        val me = FamilyProfile("me", "나$id", "🙂", "본인", true, "해당 없음", emptySet(), emptySet())
        families.values.value = FamilyProfilesState(FamilyAccount(id, sessions.state.value.generation), listOf(me, member(41), member(42)))
    }
    private class Remote : AnalysisSelectionRemote {
        val values = mutableMapOf<Long, SavedAnalysisSelection>()
        var failGet = false
        var conflict = false
        var putCount = 0
        val versions = mutableListOf<Long>()
        override suspend fun get(account: FamilyAccount): AuthResult<SavedAnalysisSelection> =
            if (failGet) AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
            else AuthResult.Success(values.getOrPut(account.userId) { SavedAnalysisSelection(true, emptyList(), 0, false, "self") })
        override suspend fun put(account: FamilyAccount, version: Long, includeSelf: Boolean, ids: List<Long>): AuthResult<SavedAnalysisSelection> {
            putCount++; versions += version
            if (conflict) return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "ANALYSIS_SELECTION_VERSION_CONFLICT"))
            val value = SavedAnalysisSelection(includeSelf, ids, version + 1, true,
                if (ids.isNotEmpty()) "selected" else if (includeSelf) "self" else "none")
            values[account.userId] = value
            return AuthResult.Success(value)
        }
    }
    private val remote = Remote()
    private suspend fun ready(repo: AnalysisTargetRepository) = repo.state.first { it.ready }

    @Test fun `successful initial server read supplies the default not the device`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope)
        assertEquals(setOf("me"), ready(repo).selectedProfileIds)
        assertEquals(0L, repo.state.value.serverVersion)
        assertEquals(0, remote.putCount)
    }
    @Test fun `sequential writes use accepted server versions and all off survives a new reader`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope); ready(repo)
        assertTrue(repo.setSelected("me", false)); assertTrue(repo.setSelected("41", true)); assertTrue(repo.setSelected("41", false))
        assertEquals(listOf(0L, 1L, 2L), remote.versions)
        val restarted = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope)
        assertEquals("none", ready(restarted).selection!!.mode)
    }
    @Test fun `a new device restores family ids from the server`() = runTest {
        login(); remote.values[1] = SavedAnalysisSelection(false, listOf(42), 7, true, "selected")
        val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope)
        assertEquals(setOf("42"), ready(repo).selectedProfileIds)
        assertEquals(0, remote.putCount)
    }
    @Test fun `failed read does not invent self or all off`() = runTest {
        login(); remote.failGet = true
        val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope)
        runCurrent()
        assertFalse(repo.state.value.ready); assertNull(repo.awaitSelection())
        assertEquals(0, remote.putCount)
    }
    @Test fun `conflict reads latest state without replay and requires explicit confirmation`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope); ready(repo)
        remote.values[1] = SavedAnalysisSelection(false, listOf(42), 1, true, "selected"); remote.conflict = true
        assertFalse(repo.setSelected("41", true)); runCurrent()
        assertTrue(repo.state.value.needsConfirmation); assertFalse(repo.state.value.ready)
        assertEquals(setOf("42"), repo.state.value.selectedProfileIds)
        assertEquals(1, remote.putCount)
        assertFalse(repo.setSelected("41", true)); assertEquals(1, remote.putCount)
        repo.refresh(confirmChanges = true); ready(repo)
        assertEquals(1, remote.putCount)
    }
    @Test fun `account and generation isolate late snapshots`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope); ready(repo)
        repo.setSelected("me", false); repo.setSelected("41", true)
        val first = repo.awaitSelection()!!
        login(2); repo.state.first { it.ready && it.account?.userId == 2L }
        assertEquals(setOf("me"), repo.state.value.selectedProfileIds)
        assertFalse(repo.isCurrent(first)); assertFalse(repo.setSelected("41", true, first.account))
        login(1); repo.state.first { it.ready && it.account?.userId == 1L }
        assertFalse(repo.isCurrent(first)); assertEquals(setOf("41"), repo.state.value.selectedProfileIds)
    }
    @Test fun `server deletion selection is reloaded after family changes`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope); ready(repo)
        repo.setSelected("me", false); repo.setSelected("41", true)
        remote.values[1] = SavedAnalysisSelection(false, emptyList(), 3, true, "none")
        families.values.value = families.values.value.copy(profiles = families.values.value.profiles.filterNot { it.serverId == 41L })
        repo.state.first { it.ready && it.serverVersion == 3L }
        assertEquals("none", repo.state.value.selection!!.mode)
    }
    @Test fun `analysis waits for a preceding server toggle write`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(remote, sessions, families, backgroundScope); ready(repo)
        val write = async(start = CoroutineStart.UNDISPATCHED) { repo.setSelected("41", true) }
        val result = repo.awaitSelection()
        assertTrue(write.await()); assertEquals(listOf(41L), result!!.familyProfileIds)
    }

}
