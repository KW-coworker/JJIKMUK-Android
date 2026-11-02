package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.domain.FamilyProfile as ServerFamily
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FamilyProfileRepositoryTest {
    private val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
    private suspend fun login(manager: AuthSessionManager = sessions, id: Long = 1) {
        val attempt = manager.beginLogin()
        check(manager.acceptLogin(attempt, AuthLogin("access-$id", id, "$id@example.com", "unused", 900, true), 1000))
    }
    private fun family(id: Long = 41, allergies: String? = "milk", diet: String? = "lowSugar") =
        ServerFamily(id, "아빠", allergies, diet, "질환 기록", "기피 성분", 0, "아버지", "👨🏻")
    private class Server : FamilyRemote {
        val families = mutableMapOf<Long, MutableList<ServerFamily>>()
        var writes = 0
        var lostCreate = false
        var failRead = false
        var lastPatch: FamilyPatch? = null
        var createGate: CompletableDeferred<Unit>? = null
        var createStarted: CompletableDeferred<Unit>? = null
        var readGate: CompletableDeferred<Unit>? = null
        var readStarted: CompletableDeferred<Unit>? = null
        override suspend fun me(account: FamilyAccount) = AuthResult.Success(AuthProfile(account.userId,
            "${account.userId}@example.com", "본인${account.userId}", "egg", null, null, null, "USER", AuthProvider.LOCAL, true, true, null))
        override suspend fun list(account: FamilyAccount): AuthResult<List<ServerFamily>> {
            val snapshot = families[account.userId].orEmpty().toList()
            readStarted?.complete(Unit); readGate?.await()
            return if (failRead) AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) else AuthResult.Success(snapshot)
        }
        override suspend fun create(account: FamilyAccount, input: FamilySubmission): AuthResult<ServerFamily> {
            writes++; createStarted?.complete(Unit); createGate?.await()
            val value = ServerFamily(100L + writes, input.nickname, input.allergies, input.specialDiet, input.diseases,
                input.dislikedIngredients, 0, input.relation, input.emoji)
            families.getOrPut(account.userId) { mutableListOf() }.add(value)
            return if (lostCreate) AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) else AuthResult.Success(value)
        }
        override suspend fun patch(account: FamilyAccount, id: Long, version: Long, input: FamilyPatch): AuthResult<ServerFamily> {
            writes++; lastPatch = input
            val list = families[account.userId] ?: return missing()
            val previous = list.firstOrNull { it.id == id } ?: return missing()
            if (previous.version != version) return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "PROFILE_VERSION_CONFLICT"))
            fun field(value: ProfileField, old: String?) = when (value) {
                ProfileField.Unchanged -> old
                ProfileField.Clear -> null
                is ProfileField.Value -> value.value
            }
            val p = input.conditions
            val result = previous.copy(nickname = p.nickname ?: previous.nickname,
                allergies = field(p.allergies, previous.allergies), specialDiet = field(p.specialDiet, previous.specialDiet),
                relation = field(input.relation, previous.relation), emoji = field(input.emoji, previous.emoji), version = version + 1)
            list[list.indexOf(previous)] = result
            return AuthResult.Success(result)
        }
        override suspend fun delete(account: FamilyAccount, id: Long, version: Long): AuthResult<Unit> {
            writes++
            val list = families[account.userId] ?: return missing()
            val previous = list.firstOrNull { it.id == id } ?: return missing()
            if (previous.version != version) return AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 409, "PROFILE_VERSION_CONFLICT"))
            list.remove(previous); return AuthResult.Success(Unit)
        }
        private fun missing() = AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 404, "FAMILY_NOT_FOUND"))
    }
    private fun TestScope.watch(repo: FamilyProfileRepositoryImpl): StateFlow<FamilyProfilesState> {
        val state = repo.observeState().stateIn(backgroundScope, SharingStarted.Eagerly, FamilyProfilesState(loaded = false))
        runCurrent(); return state
    }

    @Test fun `same account on fresh device restores family metadata and conditions without local database`() = runTest {
        login()
        val server = Server().apply { families[1] = mutableListOf(family()) }
        val first = watch(FamilyProfileRepositoryImpl(server, sessions))
        assertEquals(1, first.value.profiles.count { it.isMe })
        val member = first.value.profiles.single { !it.isMe }
        assertEquals("41", member.id); assertEquals(41L, member.serverId)
        assertEquals("👨🏻", member.emoji); assertEquals("아버지", member.relation)
        assertEquals(setOf("우유"), member.allergies); assertEquals(setOf("저당"), member.preferences)
        val other = AuthSessionManager(MemorySessionStore()) { 1000L }; login(other)
        val second = watch(FamilyProfileRepositoryImpl(server, other))
        assertEquals(first.value.profiles, second.value.profiles)
        assertEquals(0, server.writes)
    }

    @Test fun `create edit conditions metadata and delete use server ids and versions`() = runTest {
        login()
        val server = Server(); val repo = FamilyProfileRepositoryImpl(server, sessions); val state = watch(repo)
        assertTrue(repo.addProfile("엄마", "👩🏻", "어머니")); runCurrent()
        var member = state.value.profiles.single { !it.isMe }
        assertTrue(repo.updateDietCondition(member.id, "비건", setOf("우유", "밀"), setOf("저당"), member.version)); runCurrent()
        member = state.value.profiles.single { !it.isMe }
        assertEquals("milk,wheat", server.families[1]!!.single().allergies)
        assertEquals("vegan,lowSugar", server.families[1]!!.single().specialDiet)
        assertEquals(1L, member.version)
        assertTrue(repo.updateProfile(member.id, "어머니", "🙂", "보호자", member.version)); runCurrent()
        member = state.value.profiles.single { !it.isMe }
        assertEquals(setOf("우유", "밀"), member.allergies)
        assertTrue(repo.deleteProfile(member.id, member.version)); runCurrent()
        assertEquals(1, state.value.profiles.size); assertTrue(state.value.profiles.single().isMe)
    }

    @Test fun `condition clear uses explicit null and preserves unrelated diseases and display metadata`() = runTest {
        login()
        val server = Server().apply { families[1] = mutableListOf(family()) }
        val repo = FamilyProfileRepositoryImpl(server, sessions); val state = watch(repo)
        assertTrue(repo.updateDietCondition("41", "해당 없음", emptySet(), emptySet(), 0)); runCurrent()
        val patch = server.lastPatch!!
        assertSame(ProfileField.Clear, patch.conditions.allergies)
        assertSame(ProfileField.Clear, patch.conditions.specialDiet)
        assertSame(ProfileField.Unchanged, patch.conditions.diseases)
        assertSame(ProfileField.Unchanged, patch.emoji)
        assertEquals("질환 기록", state.value.profiles.single { !it.isMe }.diseases)
    }

    @Test fun `stale version conflict is not retried and requires fresh read`() = runTest {
        login()
        val server = Server().apply { families[1] = mutableListOf(family()) }
        val repo = FamilyProfileRepositoryImpl(server, sessions); val state = watch(repo)
        server.families[1]!![0] = family().copy(version = 1, nickname = "다른기기")
        assertFalse(repo.updateProfile("41", "내수정", "🙂", "보호자", 0)); runCurrent()
        assertEquals(1, server.writes); assertTrue(state.value.needsReload)
        assertEquals("다른기기", server.families[1]!!.single().nickname)
        repo.refresh(); runCurrent()
        assertFalse(state.value.needsReload)
        assertEquals(1L, state.value.profiles.single { !it.isMe }.version)
    }

    @Test fun `lost create response never automatically creates duplicate family`() = runTest {
        login()
        val server = Server().apply { lostCreate = true }
        val repo = FamilyProfileRepositoryImpl(server, sessions); val state = watch(repo)
        assertFalse(repo.addProfile("엄마", "👩🏻", "어머니")); runCurrent()
        assertTrue(state.value.needsReload)
        assertFalse(repo.addProfile("엄마", "👩🏻", "어머니"))
        assertEquals(1, server.writes)
        repo.refresh(); runCurrent()
        assertEquals(1, state.value.profiles.count { !it.isMe })
    }

    @Test fun `unknown conditions remain visible and cannot be overwritten by the editor`() = runTest {
        login()
        val server = Server().apply { families[1] = mutableListOf(family(allergies = "future_allergen")) }
        val repo = FamilyProfileRepositoryImpl(server, sessions); val state = watch(repo)
        assertFalse(state.value.profiles.single { !it.isMe }.conditionsSupported)
        assertFalse(repo.updateDietCondition("41", "해당 없음", emptySet(), emptySet(), 0))
        assertEquals(0, server.writes)
        assertTrue(repo.updateProfile("41", "새이름", "🙂", "보호자", 0))
        assertEquals("future_allergen", server.families[1]!!.single().allergies)
    }

    @Test fun `name and twenty member limit are checked without global nickname lookup`() = runTest {
        login()
        val server = Server().apply { families[1] = (1L..20L).map { family(id = it) }.toMutableList() }
        val repo = FamilyProfileRepositoryImpl(server, sessions); watch(repo)
        assertFalse(repo.addProfile("가족추가", "🙂", "가족"))
        assertFalse(repo.updateProfile("1", "a", "🙂", "가족", 0))
        assertEquals(0, server.writes)
    }

    @Test fun `double create taps dispatch exactly one mutation`() = runTest {
        login()
        val server = Server().apply { createGate = CompletableDeferred(); createStarted = CompletableDeferred() }
        val repo = FamilyProfileRepositoryImpl(server, sessions); watch(repo)
        val first = launch { repo.addProfile("엄마", "👩🏻", "어머니") }
        server.createStarted!!.await()
        assertFalse(repo.addProfile("엄마", "👩🏻", "어머니"))
        server.createGate!!.complete(Unit); first.join()
        assertEquals(1, server.writes)
    }

    @Test fun `account switch logout and late old account selection are isolated`() = runTest {
        login()
        val server = Server().apply { families[1] = mutableListOf(family()) }
        val repo = FamilyProfileRepositoryImpl(server, sessions); val state = watch(repo)
        val old = repo.captureAccount()
        login(id = 2); runCurrent()
        assertEquals("본인2", state.value.profiles.single().name)
        assertFalse(repo.addProfile("엄마", "👩🏻", "어머니", old))
        assertEquals(0, server.writes)
        sessions.clear(); runCurrent()
        assertTrue(state.value.profiles.isEmpty())
    }

    @Test fun `old in flight family response cannot appear after switching accounts`() = runTest {
        login()
        val server = Server().apply { families[1] = mutableListOf(family()); readGate = CompletableDeferred(); readStarted = CompletableDeferred() }
        val repo = FamilyProfileRepositoryImpl(server, sessions)
        val old = launch { repo.refresh() }; server.readStarted!!.await()
        login(id = 2); server.readGate!!.complete(Unit); old.join(); server.readGate = null
        val state = watch(repo)
        assertEquals("본인2", state.value.profiles.single().name)
    }

    @Test fun `server read failure is not treated as empty successful family list`() = runTest {
        login()
        val server = Server().apply { failRead = true }
        val repo = FamilyProfileRepositoryImpl(server, sessions); val state = watch(repo)
        assertFalse(state.value.loaded); assertNotNull(state.value.message)
        assertFalse(repo.addProfile("엄마", "👩🏻", "어머니")); assertEquals(0, server.writes)
        server.failRead = false; repo.refresh(); runCurrent()
        assertTrue(state.value.loaded)
    }
}
