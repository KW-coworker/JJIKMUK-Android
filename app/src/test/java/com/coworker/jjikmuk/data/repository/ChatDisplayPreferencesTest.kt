package com.coworker.jjikmuk.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.coworker.jjikmuk.data.local.preference.ChatDisplayPreferences
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ChatDisplayPreferencesTest {
    @get:Rule val temporary=TemporaryFolder()
    private fun TestScope.storage()=PreferenceDataStoreFactory.create(scope=backgroundScope) {
        temporary.newFolder().resolve("chat-display.preferences_pb")
    }
    @Test fun `title and pin survive a new reader and remain isolated by account and room`()=runTest {
        val data=storage(); val prefs=ChatDisplayPreferences(data)
        prefs.title(1,"12"," 내 대화 "); prefs.pin(1,"12",true)
        val reopened=ChatDisplayPreferences(data)
        assertEquals("내 대화",reopened.observe(1).first()["12"]!!.title)
        assertTrue(reopened.observe(1).first()["12"]!!.pinned)
        assertNull(reopened.observe(1).first()["13"])
        assertTrue(reopened.observe(2).first().isEmpty())
        prefs.title(2,"12","다른 계정")
        assertEquals("내 대화",reopened.observe(1).first()["12"]!!.title)
    }
    @Test fun `unpin and delete affect only the specified account room metadata`()=runTest {
        val prefs=ChatDisplayPreferences(storage())
        prefs.title(1,"12","첫 계정"); prefs.pin(1,"12",true); prefs.title(2,"12","둘째 계정")
        prefs.pin(1,"12",false)
        assertFalse(prefs.observe(1).first()["12"]!!.pinned)
        assertEquals("첫 계정",prefs.observe(1).first()["12"]!!.title)
        prefs.remove(1,"12")
        assertNull(prefs.observe(1).first()["12"])
        assertEquals("둘째 계정",prefs.observe(2).first()["12"]!!.title)
    }
    @Test fun `invalid titles are rejected and unicode titles are preserved`()=runTest {
        val prefs=ChatDisplayPreferences(storage())
        for (title in listOf(" ","제목\n변경","a".repeat(101))) {
            try { prefs.title(1,"12",title); fail("Invalid title must not be saved") } catch (_:IllegalArgumentException) {}
        }
        val title="🙂".repeat(100)
        prefs.title(1,"12",title)
        assertEquals(title,prefs.observe(1).first()["12"]!!.title)
    }
    private class Store(private val sessions:AuthSessionManager):StoredChatStore {
        val rooms=listOf(ChatConversation("11","새 대화","",false,20),ChatConversation("12","서버 제목","",false,10))
        var pageItems = rooms
        override fun captureAccount()=sessions.state.value.let { s -> s.credentials?.let { FamilyAccount(it.userId,s.generation) } }
        override suspend fun rooms(account:FamilyAccount,page:Int)=AuthResult.Success(StoredChatRooms(pageItems,page,false))
        override suspend fun room(account:FamilyAccount,id:Long)=AuthResult.Success(rooms.first { it.id==id.toString() })
        override suspend fun create(account:FamilyAccount,clientId:String,title:String)=error("No server title mutation allowed")
        override suspend fun turns(account:FamilyAccount,id:Long,after:Long)=error("unused")
        override suspend fun turn(account:FamilyAccount,id:Long,clientId:String)=error("unused")
        override suspend fun send(account:FamilyAccount,question:StoredChatQuestion)=error("unused")
        override suspend fun delete(account:FamilyAccount,id:Long)=AuthResult.Success(Unit)
    }
    @Test fun `local display overrides survive server refresh and do not follow an account switch`()=runTest {
        val sessions=AuthSessionManager(MemorySessionStore()) { 1000L }
        suspend fun login(id:Long) { val attempt=sessions.beginLogin(); check(sessions.acceptLogin(attempt,
            AuthLogin("token",id,"$id@example.com","unused",900,true),1000)) }
        login(1)
        val prefs=ChatDisplayPreferences(storage());val remote=Store(sessions)
        val repo=ChatHistoryRepositoryImpl(remote,sessions,prefs)
        val values=repo.observeConversations().stateIn(backgroundScope,SharingStarted.Eagerly,emptyList())
        values.first { it.size==2 }
        repo.updatePinned("12",true)
        values.first { it.firstOrNull()?.id=="12" && it.first().isPinned }
        repo.updateTitle("12","기기 제목")
        values.first { it.firstOrNull()?.title=="기기 제목" }
        assertEquals("기기 제목",repo.observeTitle("12","서버 제목").first())
        repo.refresh()
        assertEquals("기기 제목",values.first { it.size==2 }.first().title)
        login(2)
        values.first { it.size==2 && it.none { row -> row.isPinned } }
        assertEquals("서버 제목",values.value.first { it.id=="12" }.title)
        assertEquals("서버 제목",remote.rooms.first { it.id=="12" }.title)
    }
    @Test fun `pinned old room outside the first server page remains visible`()=runTest {
        val sessions=AuthSessionManager(MemorySessionStore()) { 1000L }
        val attempt=sessions.beginLogin(); check(sessions.acceptLogin(attempt,
            AuthLogin("token",1,"1@example.com","unused",900,true),1000))
        val prefs=ChatDisplayPreferences(storage());prefs.pin(1,"12",true)
        val remote=Store(sessions).apply { pageItems=rooms.take(1) }
        val repo=ChatHistoryRepositoryImpl(remote,sessions,prefs)
        val values=repo.observeConversations().first { it.size==2 }
        assertEquals("12",values.first().id); assertTrue(values.first().isPinned)
    }

}
