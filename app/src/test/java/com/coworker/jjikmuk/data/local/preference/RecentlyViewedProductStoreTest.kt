package com.coworker.jjikmuk.data.local.preference

import com.coworker.jjikmuk.data.remote.api.HistoryEntry
import com.coworker.jjikmuk.data.remote.dto.ProductDto
import com.coworker.jjikmuk.data.repository.HistoryRemote
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class RecentlyViewedProductStoreTest {
    private val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
    private suspend fun login(manager: AuthSessionManager = sessions, id: Long = 1) {
        val attempt = manager.beginLogin()
        check(manager.acceptLogin(attempt, AuthLogin("access-$id", id, "$id@example.com", "unused", 900, true), 1000))
    }
    private fun product(code: String) = ProductDetail(code, "상품$code", "제조사", null, emptyList(), null, null, null,
        ProductNutrition(null, null, null, null, null, null, null), ProductMacroPercents(null, null, null),
        ProductSafetyStatus.Unknown, emptyList(), null, false)
    private fun event(code: String, id: Long, type: String = "SCAN") = HistoryEntry(id, type,
        "2026-11-02T01:00:00", ProductDto(barcode = code, productName = "상품$code", manufacturer = "제조사"))
    private class Server : HistoryRemote {
        val histories = mutableMapOf<Long, List<HistoryEntry?>>()
        var writes = 0
        var failure = false
        var gate: CompletableDeferred<Unit>? = null
        var started: CompletableDeferred<Unit>? = null
        override suspend fun list(owner: ProductViewOwner): AuthResult<List<HistoryEntry?>> {
            val captured = histories[owner.userId].orEmpty()
            started?.complete(Unit)
            gate?.await()
            return if (failure) AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) else AuthResult.Success(captured)
        }
        override suspend fun record(owner: ProductViewOwner, barcode: String): AuthResult<Unit> {
            writes++
            if (failure) return AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
            histories[owner.userId] = listOf(HistoryEntry(100, "DETAIL_VIEW", "2026-11-03T00:00:00",
                ProductDto(barcode = barcode, productName = "상품$barcode"))) + histories[owner.userId].orEmpty()
            return AuthResult.Success(Unit)
        }
    }

    @Test fun `fresh device reads same account server history without local preferences`() = runTest {
        login()
        val server = Server().apply { histories[1] = listOf(event("A", 1)) }
        val first = RecentlyViewedProductStore(server, sessions)
        assertEquals("A", first.state.first { !it.isLoading }.products.single().barcode)
        val otherDevice = AuthSessionManager(MemorySessionStore()) { 1000L }
        login(otherDevice)
        assertEquals("A", RecentlyViewedProductStore(server, otherDevice).state.first { !it.isLoading }.products.single().barcode)
        server.histories[1] = listOf(event("B", 2), event("A", 1))
        assertEquals(listOf("B", "A"), first.state.first { !it.isLoading }.products.map { it.barcode })
    }

    @Test fun `latest ten distinct products exclude favorites missing products and invalid data`() = runTest {
        login()
        val server = Server().apply {
            histories[1] = (1L..15L).map { event(it.toString(), it) } + listOf(
                event("15", 16), event("favorite", 20, "FAVORITE"), HistoryEntry(30, "SCAN"), null,
                HistoryEntry(40, "SCAN", product = ProductDto(barcode = "", productName = "invalid")))
        }
        val items = RecentlyViewedProductStore(server, sessions).state.first { !it.isLoading }.products
        assertEquals((15 downTo 6).map(Int::toString), items.map { it.barcode })
    }

    @Test fun `normal detail refresh does not duplicate server automatic scan event`() = runTest {
        login()
        val server = Server().apply { histories[1] = listOf(event("A", 1)) }
        val store = RecentlyViewedProductStore(server, sessions)
        store.record(product("A"), store.captureOwner())
        assertEquals(0, server.writes)
        assertEquals("A", store.state.first { !it.isLoading }.products.single().barcode)
    }

    @Test fun `capacity selection records event and failed mutation is not retried`() = runTest {
        login()
        val server = Server()
        val store = RecentlyViewedProductStore(server, sessions)
        store.record(product("A"), store.captureOwner(), alreadyRecorded = false)
        assertEquals(1, server.writes)
        assertEquals("A", store.state.first { !it.isLoading }.products.single().barcode)
        server.failure = true
        store.record(product("B"), store.captureOwner(), alreadyRecorded = false)
        assertEquals(2, server.writes)
        assertNotNull(store.state.first { !it.isLoading }.failure)
    }

    @Test fun `account switch and logout do not expose previous account history`() = runTest {
        login()
        val server = Server().apply { histories[1] = listOf(event("A", 1)); histories[2] = listOf(event("B", 2)) }
        val store = RecentlyViewedProductStore(server, sessions)
        val oldOwner = store.captureOwner()
        assertEquals("A", store.state.first { !it.isLoading }.products.single().barcode)
        login(id = 2)
        store.record(product("OLD"), oldOwner, alreadyRecorded = false)
        assertEquals(0, server.writes)
        assertEquals("B", store.state.first { !it.isLoading }.products.single().barcode)
        sessions.clear()
        assertTrue(store.state.first().products.isEmpty())
        assertNull(store.captureOwner())
    }

    @Test fun `in flight old account read is discarded when account switches`() = runTest {
        login()
        val server = Server().apply { histories[1] = listOf(event("A", 1)); gate = CompletableDeferred(); started = CompletableDeferred() }
        val store = RecentlyViewedProductStore(server, sessions)
        val pending = launch { store.refresh() }
        server.started!!.await()
        login(id = 2)
        server.gate!!.complete(Unit)
        pending.join()
        server.gate = null
        assertTrue(store.state.first { !it.isLoading }.products.isEmpty())
    }

    @Test fun `network failure is distinguished from an empty successful history`() = runTest {
        login()
        val server = Server().apply { failure = true }
        val store = RecentlyViewedProductStore(server, sessions)
        assertNotNull(store.state.first { !it.isLoading }.failure)
        server.failure = false
        assertNull(store.state.first { !it.isLoading }.failure)
    }
}
