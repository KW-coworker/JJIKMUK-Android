package com.coworker.jjikmuk.data.repository

import com.coworker.jjikmuk.data.remote.api.*
import com.coworker.jjikmuk.domain.model.LikedProduct
import com.coworker.jjikmuk.domain.repository.*
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Server state only: legacy local favorites never leak into a different account. */
@OptIn(ExperimentalCoroutinesApi::class)
class LikedProductRepositoryImpl internal constructor(private val remote: FavoritesRemote,
    private val sessions: AuthSessionManager) : LikedProductRepository {
    @Inject constructor(remote: HttpFavoritesRemote, sessions: AuthSessionManager) : this(remote as FavoritesRemote, sessions)
    private val mutableState = MutableStateFlow(LikedProductsState())
    private val mutex = Mutex()
    private val pending = mutableSetOf<Pair<FavoriteAccount, Long>>()

    override fun captureAccount(): FavoriteAccount? = sessions.state.value.let { state ->
        state.credentials?.takeIf { state.canEnterHome }?.let { FavoriteAccount(it.userId, state.generation) }
    }
    override fun observeState(): Flow<LikedProductsState> = sessions.state.map { captureAccount() }.distinctUntilChanged()
        .flatMapLatest { account ->
            if (account == null) flowOf(LikedProductsState()) else flow {
                emit(LikedProductsState(account, isLoading = true))
                refresh(account)
                emitAll(mutableState.filter { it.account == account })
            }
        }
    private fun current(account: FavoriteAccount?) = account != null && account == captureAccount()
    private fun snapshot(account: FavoriteAccount) = mutableState.value.takeIf { it.account == account } ?: LikedProductsState(account)

    override suspend fun refresh(account: FavoriteAccount?) = mutex.withLock {
        if (!current(account)) return@withLock
        account!!
        val before = snapshot(account)
        mutableState.value = before.copy(isLoading = true, message = null)
        val items = mutableListOf<LikedProduct>()
        var page = 0
        while (true) {
            val result = remote.list(account, page)
            if (!current(account)) return@withLock
            if (result is AuthResult.Failure) {
                mutableState.value = before.copy(isLoading = false, message = failureMessage(result.error))
                return@withLock
            }
            val body = (result as AuthResult.Success).value
            if (body.page != page || body.items == null || body.hasNext == null || (body.hasNext && body.items.isEmpty()) || page > 10000) {
                mutableState.value = before.copy(isLoading = false, message = "즐겨찾기 정보를 확인하지 못했어요. 다시 시도해 주세요.")
                return@withLock
            }
            items += body.items.mapNotNull { item ->
                val id = item?.productId?.takeIf { it > 0 } ?: return@mapNotNull null
                val product = item.product?.takeIf { it.productId == id }
                LikedProduct(product?.barcode ?: item.barcode.orEmpty(),
                    product?.productName?.takeIf(String::isNotBlank) ?: product?.cleanProductName ?: "현재 정보를 조회할 수 없는 상품",
                    product?.manufacturer.orEmpty(), product?.imageUrl, id, product != null)
            }
            if (!body.hasNext) break
            page++
        }
        val products = items.distinctBy { it.productId }
        val statuses = before.statuses.mapValues { false }.toMutableMap()
        products.forEach { statuses[it.productId] = true }
        mutableState.value = before.copy(products = products, statuses = statuses, isLoading = false, message = null)
    }

    override suspend fun refreshStatus(productId: Long, account: FavoriteAccount?) = mutex.withLock {
        if (!current(account) || productId <= 0) return@withLock
        account!!
        val result = remote.state(account, productId)
        if (!current(account)) return@withLock
        val before = snapshot(account)
        mutableState.value = when {
            result is AuthResult.Success && result.value.productId == productId && result.value.isFavorite != null ->
                before.copy(statuses = before.statuses + (productId to result.value.isFavorite), message = null)
            result is AuthResult.Failure -> before.copy(message = failureMessage(result.error))
            else -> before.copy(message = "즐겨찾기 상태를 확인하지 못했어요. 다시 시도해 주세요.")
        }
    }

    override suspend fun setFavorite(productId: Long, favorite: Boolean, account: FavoriteAccount?) {
        if (!current(account) || productId <= 0) return
        account!!
        val key = account to productId
        if (!synchronized(pending) { pending.add(key) }) return
        var operationMessage: String? = null
        try {
            mutex.withLock {
                if (!current(account)) return@withLock
                val before = snapshot(account)
                mutableState.value = before.copy(pendingIds = before.pendingIds + productId, message = null)
                val result = remote.set(account, productId, favorite)
                if (!current(account)) return@withLock
                val latest = snapshot(account)
                mutableState.value = when {
                    result is AuthResult.Success && result.value.productId == productId && result.value.isFavorite == favorite ->
                        latest.copy(statuses = latest.statuses + (productId to favorite),
                            products = if (favorite) latest.products else latest.products.filterNot { it.productId == productId },
                            pendingIds = latest.pendingIds - productId, message = null)
                    result is AuthResult.Failure -> latest.copy(pendingIds = latest.pendingIds - productId,
                        statuses = if (result.error.outcomeUnknown) latest.statuses - productId else latest.statuses,
                        message = failureMessage(result.error))
                    else -> latest.copy(pendingIds = latest.pendingIds - productId, statuses = latest.statuses - productId,
                        message = "즐겨찾기 저장 상태를 확인하지 못했어요. 다시 조회해 주세요.")
                }
            }
            operationMessage = snapshot(account).message
            if (current(account)) {
                refresh(account) // Read after mutation, never replay an uncertain PUT/DELETE.
                mutex.withLock {
                    if (current(account) && operationMessage != null && snapshot(account).statuses[productId] != favorite)
                        mutableState.value = snapshot(account).copy(message = operationMessage)
                }
            }
        } finally {
            synchronized(pending) { pending.remove(key) }
            mutableState.update { if (it.account == account) it.copy(pendingIds = it.pendingIds - productId) else it }
        }
    }

    private fun failureMessage(error: AuthFailure) = when (error.code) {
        "AUTHENTICATION_REQUIRED", "AUTH_TOKEN_INVALID", "AUTH_TOKEN_EXPIRED", "AUTH_REFRESHED_RETRY_REQUIRED" -> "로그인 상태를 확인한 뒤 다시 시도해 주세요."
        "PRODUCT_NOT_FOUND" -> "현재 정보를 조회할 수 없는 상품이에요."
        else -> "즐겨찾기를 서버에서 확인하지 못했어요. 눌러서 다시 시도해 주세요."
    }
}
