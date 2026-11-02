package com.coworker.jjikmuk.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.AuthLogin
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
    @get:Rule val temporary = TemporaryFolder()
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
    private fun TestScope.storage() = PreferenceDataStoreFactory.create(scope = backgroundScope) {
        temporary.newFolder().resolve("selection.preferences_pb")
    }
    private suspend fun ready(repo: AnalysisTargetRepository) = repo.state.first { it.ready }

    @Test fun `first use selects only self and maps to self query without selected-only arguments`() = runTest {
        login()
        val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope)
        val state = ready(repo)
        assertEquals(setOf("me"), state.selectedProfileIds)
        assertEquals("self", state.selection!!.mode)
        assertNull(state.selection!!.queryIncludeSelf); assertNull(state.selection!!.queryFamilyIds)
    }
    @Test fun `selected families are all included and self is independently controlled`() = runTest {
        login()
        val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope); ready(repo)
        assertTrue(repo.setSelected("me", false)); assertTrue(repo.setSelected("42", true)); assertTrue(repo.setSelected("41", true))
        val selected = repo.awaitSelection()!!
        assertEquals("selected", selected.mode); assertEquals(listOf(41L, 42L), selected.familyProfileIds)
        assertEquals(false, selected.queryIncludeSelf)
        assertTrue(repo.setSelected("me", true)); assertEquals(true, repo.awaitSelection()!!.queryIncludeSelf)
    }
    @Test fun `all off is explicit none and survives a new reader`() = runTest {
        login(); val data = storage()
        val repo = AnalysisTargetRepositoryImpl(data, sessions, families, backgroundScope); ready(repo)
        repo.setSelected("me", false)
        assertEquals("none", repo.awaitSelection()!!.mode)
        val restarted = AnalysisTargetRepositoryImpl(data, sessions, families, backgroundScope)
        val result = ready(restarted).selection!!
        assertEquals("none", result.mode); assertNull(result.queryIncludeSelf); assertNull(result.queryFamilyIds)
    }
    @Test fun `account selection is isolated and returning account restores its own IDs`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope); ready(repo)
        repo.setSelected("me", false); repo.setSelected("41", true)
        login(2); repo.state.first { it.ready && it.account?.userId == 2L }
        assertEquals(setOf("me"), repo.state.value.selectedProfileIds)
        login(1); repo.state.first { it.ready && it.account?.userId == 1L }
        assertEquals(setOf("41"), repo.state.value.selectedProfileIds)
    }
    @Test fun `deleting sole selected family persists none and never enables self`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope); ready(repo)
        repo.setSelected("me", false); repo.setSelected("41", true)
        families.values.value = families.values.value.copy(profiles = families.values.value.profiles.filterNot { it.serverId == 41L })
        repo.state.first { it.ready && it.selection?.mode == "none" }
        runCurrent()
        families.values.value = families.values.value.copy(profiles = families.values.value.profiles + member(41))
        runCurrent()
        assertEquals("none", repo.awaitSelection()!!.mode)
    }
    @Test fun `snapshot and old account toggle are rejected after account change`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope); ready(repo)
        val previous = repo.awaitSelection()!!
        login(2); repo.state.first { it.ready && it.account?.userId == 2L }
        assertFalse(repo.isCurrent(previous)); assertFalse(repo.setSelected("41", true, previous.account))
        assertNull(repo.awaitSelection(previous.account))
    }
    @Test fun `selected profile revision invalidates snapshot while unselected edits do not`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope); ready(repo)
        repo.setSelected("41", true)
        val snapshot = repo.awaitSelection()!!
        families.values.value = families.values.value.copy(profiles = families.values.value.profiles.map { if (it.serverId == 42L) it.copy(version = 1) else it })
        runCurrent(); assertTrue(repo.isCurrent(snapshot))
        families.values.value = families.values.value.copy(profiles = families.values.value.profiles.map { if (it.serverId == 41L) it.copy(version = 1, allergies = setOf("땅콩")) else it })
        repo.state.first { it.ready && it.selection != snapshot }
        assertFalse(repo.isCurrent(snapshot))
    }
    @Test fun `query waits for an immediately preceding toggle commit`() = runTest {
        login(); val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope); ready(repo)
        val toggle = async(start = CoroutineStart.UNDISPATCHED) { repo.setSelected("41", true) }
        val selected = repo.awaitSelection()!!
        assertTrue(toggle.await()); assertEquals(listOf(41L), selected.familyProfileIds)
    }
    @Test fun `storage failure never silently substitutes self or none`() = runTest {
        login()
        val broken = object : DataStore<Preferences> {
            override val data: Flow<Preferences> = flow { throw IOException("unavailable") }
            override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences = throw IOException()
        }
        val repo = AnalysisTargetRepositoryImpl(broken, sessions, families, backgroundScope)
        repo.state.first { it.message != null }
        assertFalse(repo.state.value.ready); assertNull(repo.awaitSelection())
    }
    @Test fun `failed server family read blocks dispatch until targets can be verified`() = runTest {
        login(); families.values.value = families.values.value.copy(loaded = false, needsReload = true, message = "서버 오류")
        val repo = AnalysisTargetRepositoryImpl(storage(), sessions, families, backgroundScope)
        repo.state.first { it.message != null }
        assertNull(repo.awaitSelection())
    }
}
