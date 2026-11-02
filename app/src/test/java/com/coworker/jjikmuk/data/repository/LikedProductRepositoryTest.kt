package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.*
import com.coworker.jjikmuk.data.remote.dto.ProductDto
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LikedProductRepositoryTest {
    private val sessions = AuthSessionManager(MemorySessionStore()) { 1000L }
    private suspend fun login(manager: AuthSessionManager = sessions, id: Long = 1) {
        val attempt = manager.beginLogin()
        check(manager.acceptLogin(attempt, AuthLogin("access-$id", id, "$id@example.com", "unused", 900, true), 1000))
    }
    private fun item(id: Long, code: String = "same-barcode") = FavoriteItem(id, code,
        ProductDto(productId = id, barcode = code, productName = "상품$id", manufacturer = "제조사"))
    private class Server : FavoritesRemote {
        val items = mutableMapOf<Long, MutableList<FavoriteItem>>()
        var writes = 0
        var listCalls = 0
        var failList = false
        var uncertainMutation = false
        var applyBeforeFailure = false
        var readGate: CompletableDeferred<Unit>? = null
        var readStarted: CompletableDeferred<Unit>? = null
        var writeGate: CompletableDeferred<Unit>? = null
        var writeStarted: CompletableDeferred<Unit>? = null
        override suspend fun list(account: FavoriteAccount, page: Int): AuthResult<FavoritePage> {
            listCalls++
            val snapshot = items[account.userId].orEmpty().toList()
            readStarted?.complete(Unit); readGate?.await()
            if (failList) return AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
            return AuthResult.Success(FavoritePage(snapshot.drop(page * 2).take(2), page, 2, snapshot.size > (page + 1) * 2))
        }
        override suspend fun state(account: FavoriteAccount, id: Long) = AuthResult.Success(
            FavoriteStatus(id, items[account.userId].orEmpty().any { it.productId == id }))
        override suspend fun set(account: FavoriteAccount, id: Long, favorite: Boolean): AuthResult<FavoriteStatus> {
            writes++
            writeStarted?.complete(Unit); writeGate?.await()
            if (!uncertainMutation || applyBeforeFailure) {
                val list = items.getOrPut(account.userId) { mutableListOf() }
                list.removeAll { it.productId == id }
                if (favorite) list.add(0, FavoriteItem(id, "code$id", ProductDto(productId = id, barcode = "code$id", productName = "상품$id")))
            }
            return if (uncertainMutation) AuthResult.Failure(AuthFailure(AuthFailureKind.Network)) else AuthResult.Success(FavoriteStatus(id, favorite))
        }
    }

    @Test fun `same account on a fresh device reads server favorites and remove propagates`() = runTest {
        login()
        val server = Server()
        val first = LikedProductRepositoryImpl(server, sessions)
        first.setFavorite(7, true, first.captureAccount())
        val otherSession = AuthSessionManager(MemorySessionStore()) { 1000L }
        login(otherSession)
        val second = LikedProductRepositoryImpl(server, otherSession)
        assertEquals(7L, second.observeState().first { !it.isLoading }.products.single().productId)
        second.setFavorite(7, false, second.captureAccount())
        assertTrue(first.observeState().first { !it.isLoading }.products.isEmpty())
    }

    @Test fun `all pages are loaded and same barcode retains separate product ids`() = runTest {
        login()
        val server = Server().apply { items[1] = mutableListOf(item(3), item(2), item(1)) }
        val state = LikedProductRepositoryImpl(server, sessions).observeState().first { !it.isLoading }
        assertEquals(listOf(3L, 2L, 1L), state.products.map { it.productId })
        assertEquals(2, server.listCalls)
        assertTrue(state.statuses.values.all { it })
    }

    @Test fun `missing catalog product retains server id and can be removed`() = runTest {
        login()
        val server = Server().apply { items[1] = mutableListOf(FavoriteItem(8, "old", null)) }
        val repo = LikedProductRepositoryImpl(server, sessions)
        val product = repo.observeState().first { !it.isLoading }.products.single()
        assertFalse(product.isAvailable)
        repo.setFavorite(product.productId, false, repo.captureAccount())
        assertTrue(repo.observeState().first { !it.isLoading }.products.isEmpty())
    }

    @Test fun `account switch logout and stale mutation cannot expose or modify another account`() = runTest {
        login()
        val server = Server().apply { items[1] = mutableListOf(item(1)); items[2] = mutableListOf(item(2)) }
        val repo = LikedProductRepositoryImpl(server, sessions)
        val old = repo.captureAccount()
        assertEquals(1L, repo.observeState().first { !it.isLoading }.products.single().productId)
        login(id = 2)
        repo.setFavorite(1, false, old)
        assertEquals(0, server.writes)
        assertEquals(2L, repo.observeState().first { !it.isLoading }.products.single().productId)
        sessions.clear()
        assertTrue(repo.observeState().first().products.isEmpty())
        assertNull(repo.captureAccount())
    }

    @Test fun `old in flight response is discarded after account changes`() = runTest {
        login()
        val server = Server().apply { items[1] = mutableListOf(item(1)); readGate = CompletableDeferred(); readStarted = CompletableDeferred() }
        val repo = LikedProductRepositoryImpl(server, sessions)
        val pending = launch { repo.refresh() }
        server.readStarted!!.await()
        login(id = 2)
        server.readGate!!.complete(Unit)
        pending.join(); server.readGate = null
        assertTrue(repo.observeState().first { !it.isLoading }.products.isEmpty())
    }

    @Test fun `concurrent heart taps send only one target state mutation`() = runTest {
        login()
        val server = Server().apply { writeGate = CompletableDeferred(); writeStarted = CompletableDeferred() }
        val repo = LikedProductRepositoryImpl(server, sessions)
        val account = repo.captureAccount()
        val first = launch { repo.setFavorite(9, true, account) }
        server.writeStarted!!.await()
        repo.setFavorite(9, true, account)
        assertEquals(1, server.writes)
        server.writeGate!!.complete(Unit); first.join()
        assertEquals(9L, repo.observeState().first { !it.isLoading }.products.single().productId)
    }

    @Test fun `lost mutation response is reconciled with read without repeating write`() = runTest {
        login()
        val server = Server().apply { uncertainMutation = true; applyBeforeFailure = true }
        val repo = LikedProductRepositoryImpl(server, sessions)
        repo.setFavorite(7, true, repo.captureAccount())
        val state = repo.observeState().first { !it.isLoading }
        assertEquals(1, server.writes)
        assertEquals(true, state.statuses[7])
        assertTrue(state.pendingIds.isEmpty())
    }

    @Test fun `failed read is shown as failure instead of a successful empty list`() = runTest {
        login()
        val server = Server().apply { failList = true }
        val repo = LikedProductRepositoryImpl(server, sessions)
        assertNotNull(repo.observeState().first { !it.isLoading }.message)
        server.failList = false
        assertNull(repo.observeState().first { !it.isLoading }.message)
    }

    @Test fun `same account relogin rejects previous session selection`() = runTest {
        login()
        val server = Server()
        val repo = LikedProductRepositoryImpl(server, sessions)
        val old = repo.captureAccount()
        login()
        repo.setFavorite(1, true, old)
        assertEquals(0, server.writes)
    }
}
