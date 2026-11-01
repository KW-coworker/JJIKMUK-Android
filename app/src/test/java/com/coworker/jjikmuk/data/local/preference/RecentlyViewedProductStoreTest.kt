package com.coworker.jjikmuk.data.local.preference

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.feature.auth.domain.AuthLogin
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import com.coworker.jjikmuk.feature.auth.session.MemorySessionStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class RecentlyViewedProductStoreTest {
    @get:Rule val temporary = TemporaryFolder()
    private val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
    private suspend fun login(id: Long = 1) {
        val attempt = sessions.beginLogin()
        check(sessions.acceptLogin(attempt,
            AuthLogin("test-access", id, "$id@example.com", "unused", 900, true), 1000))
    }
    private fun product(barcode: String, name: String = "상품$barcode") = ProductDetail(
        barcode, name, "제조사", "https://example.com/$barcode.png", emptyList(), null, null, null,
        ProductNutrition(null, null, null, null, null, null, null), ProductMacroPercents(null, null, null),
        ProductSafetyStatus.Unknown, emptyList(), null, false,
    )
    private fun TestScope.storage() = PreferenceDataStoreFactory.create(scope = backgroundScope) {
        temporary.newFolder().resolve("viewed.preferences_pb")
    }

    @Test fun `empty history has no samples and repeat views move to front with fresh metadata`() = runTest {
        login()
        val store = RecentlyViewedProductStore(storage(), sessions)
        assertTrue(store.products.first().isEmpty())
        val owner = store.captureOwner()
        store.record(product("A"), owner)
        store.record(product("B"), owner)
        store.record(product("A", "갱신된 상품명"), owner)
        val items = store.products.first()
        assertEquals(listOf("A", "B"), items.map { it.barcode })
        assertEquals("갱신된 상품명", items.first().productName)
        assertEquals("제조사", items.first().brandName)
        assertEquals("https://example.com/A.png", items.first().imageUrl)
    }

    @Test fun `records persist across a new history reader and returning to the same account`() = runTest {
        login()
        val data = storage()
        val store = RecentlyViewedProductStore(data, sessions)
        store.record(product("A"), store.captureOwner())
        sessions.clear()
        assertTrue(store.products.first().isEmpty())
        login()
        assertEquals("A", RecentlyViewedProductStore(data, sessions).products.first().single().barcode)
    }

    @Test fun `account switch isolates history and ignores late results from old account`() = runTest {
        login(1)
        val store = RecentlyViewedProductStore(storage(), sessions)
        val oldOwner = store.captureOwner()
        store.record(product("A"), oldOwner)
        login(2)
        assertTrue(store.products.first().isEmpty())
        store.record(product("LATE"), oldOwner)
        store.record(product("B"), store.captureOwner())
        assertEquals(listOf("B"), store.products.first().map { it.barcode })
        login(1)
        assertEquals(listOf("A"), store.products.first().map { it.barcode })
    }

    @Test fun `same account relogin does not accept a previous session response`() = runTest {
        login()
        val store = RecentlyViewedProductStore(storage(), sessions)
        val previous = store.captureOwner()
        login()
        store.record(product("LATE"), previous)
        assertTrue(store.products.first().isEmpty())
    }

    @Test fun `only newest fifty are retained`() = runTest {
        login()
        val store = RecentlyViewedProductStore(storage(), sessions)
        val owner = store.captureOwner()
        repeat(55) { store.record(product(it.toString()), owner) }
        val items = store.products.first()
        assertEquals(50, items.size)
        assertEquals("54", items.first().barcode)
        assertEquals("5", items.last().barcode)
    }

    @Test fun `signed out and invalid products are not recorded`() = runTest {
        val store = RecentlyViewedProductStore(storage(), sessions)
        assertNull(store.captureOwner())
        store.record(product("A"), null)
        login()
        store.record(product(""), store.captureOwner())
        store.record(product("A", ""), store.captureOwner())
        assertTrue(store.products.first().isEmpty())
    }

    @Test fun `corrupted history recovers on next view`() = runTest {
        login()
        val data = storage()
        data.edit { it[stringPreferencesKey("account_1")] = "broken-json" }
        val store = RecentlyViewedProductStore(data, sessions)
        assertTrue(store.products.first().isEmpty())
        store.record(product("A"), store.captureOwner())
        assertEquals("A", store.products.first().single().barcode)
    }
}
