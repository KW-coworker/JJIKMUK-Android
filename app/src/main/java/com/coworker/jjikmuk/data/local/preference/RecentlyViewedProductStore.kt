package com.coworker.jjikmuk.data.local.preference

import com.coworker.jjikmuk.data.remote.api.HistoryEntry
import com.coworker.jjikmuk.data.repository.HistoryRemote
import com.coworker.jjikmuk.data.repository.HttpHistoryRemote
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class RecentlyViewedProduct(val barcode: String, val productName: String, val brandName: String, val imageUrl: String?)
/** Captured at request start; late responses cannot write into another account/session. */
data class ProductViewOwner(val userId: Long, val generation: Long)
data class RecentProductsState(val owner: ProductViewOwner? = null,
    val products: List<RecentlyViewedProduct> = emptyList(), val isLoading: Boolean = false,
    val failure: AuthFailure? = null)

/** Server history is the source of truth. Old local preferences are neither uploaded nor displayed. */
@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RecentlyViewedProductStore internal constructor(
    private val remote: HistoryRemote,
    private val sessions: AuthSessionManager,
) {
    @Inject constructor(remote: HttpHistoryRemote, sessions: AuthSessionManager) : this(remote as HistoryRemote, sessions)
    private val mutableState = MutableStateFlow(RecentProductsState())
    private val mutex = Mutex()

    val state: Flow<RecentProductsState> = sessions.state.map { captureOwner() }.distinctUntilChanged()
        .flatMapLatest { owner ->
            if (owner == null) flowOf(RecentProductsState()) else flow {
                emit(RecentProductsState(owner, isLoading = true))
                refresh(owner)
                emitAll(mutableState.filter { it.owner == owner })
            }
        }
    val products: Flow<List<RecentlyViewedProduct>> = state.map { it.products }

    fun captureOwner(): ProductViewOwner? = sessions.state.value.let { state ->
        state.credentials?.takeIf { state.canEnterHome }?.let { ProductViewOwner(it.userId, state.generation) }
    }

    suspend fun refresh(owner: ProductViewOwner? = captureOwner()) = mutex.withLock {
        if (owner == null || captureOwner() != owner) return@withLock
        val previous = mutableState.value.takeIf { it.owner == owner } ?: RecentProductsState(owner)
        mutableState.value = previous.copy(isLoading = true, failure = null)
        val result = remote.list(owner)
        if (captureOwner() != owner) return@withLock
        mutableState.value = when (result) {
            is AuthResult.Success -> RecentProductsState(owner, mapHistory(result.value))
            is AuthResult.Failure -> previous.copy(isLoading = false, failure = result.error)
        }
    }

    /** Product detail already creates SCAN on the server; capacity choices need an explicit event. */
    suspend fun record(product: ProductDetail, owner: ProductViewOwner?, alreadyRecorded: Boolean = true) {
        if (owner == null || captureOwner() != owner || product.barcode.isBlank() || product.productName.isBlank()) return
        if (!alreadyRecorded) {
            val result = remote.record(owner, product.barcode)
            if (captureOwner() != owner) return
            if (result is AuthResult.Failure) {
                mutex.withLock {
                    if (captureOwner() == owner) {
                        val previous = mutableState.value.takeIf { it.owner == owner } ?: RecentProductsState(owner)
                        mutableState.value = previous.copy(isLoading = false, failure = result.error)
                    }
                }
                return // Never retry an ambiguous mutation automatically.
            }
        }
        refresh(owner)
    }

    internal fun mapHistory(entries: List<HistoryEntry?>): List<RecentlyViewedProduct> = entries.filterNotNull()
        .filter { it.actionType in setOf("SCAN", "DETAIL_VIEW", "SEARCH_CLICK") }
        .sortedWith(compareByDescending<HistoryEntry> { it.scannedAt.orEmpty() }.thenByDescending { it.id })
        .mapNotNull { entry ->
            val product = entry.product ?: return@mapNotNull null
            val barcode = product.barcode?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = product.productName?.takeIf(String::isNotBlank)
                ?: product.cleanProductName?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            RecentlyViewedProduct(barcode, name, product.manufacturer.orEmpty(), product.imageUrl)
        }.distinctBy { it.barcode }.take(MAX_PRODUCTS)

    companion object { const val MAX_PRODUCTS = 10 }
}
