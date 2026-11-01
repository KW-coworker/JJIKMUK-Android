package com.coworker.jjikmuk.data.local.preference

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

private val Context.viewedProductsDataStore by preferencesDataStore(name = "recently_viewed_products")

data class RecentlyViewedProduct(
    val barcode: String,
    val productName: String,
    val brandName: String,
    val imageUrl: String?,
)

/** Captured when the detail request starts; a late result cannot become another account's history. */
data class ProductViewOwner(val userId: Long, val generation: Long)

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class RecentlyViewedProductStore internal constructor(
    private val dataStore: DataStore<Preferences>,
    private val sessions: AuthSessionManager,
) {
    @Inject constructor(@ApplicationContext context: Context, sessions: AuthSessionManager) :
        this(context.viewedProductsDataStore, sessions)

    private val gson = Gson()

    val products: Flow<List<RecentlyViewedProduct>> = sessions.state
        .map { it.credentials?.userId?.takeIf { _ -> it.canEnterHome } }
        .distinctUntilChanged()
        .flatMapLatest { owner ->
            if (owner == null) flowOf(emptyList()) else dataStore.data
                .catch { error -> if (error is IOException) emit(emptyPreferences()) else throw error }
                .map { decode(it[key(owner)]) }
        }

    fun captureOwner(): ProductViewOwner? = sessions.state.value.let { state ->
        state.credentials?.takeIf { state.canEnterHome }?.let { ProductViewOwner(it.userId, state.generation) }
    }

    suspend fun record(product: ProductDetail, owner: ProductViewOwner?) {
        if (owner == null || product.barcode.isBlank() || product.productName.isBlank()) return
        dataStore.edit { preferences ->
            if (captureOwner() != owner) return@edit
            val viewed = RecentlyViewedProduct(product.barcode, product.productName, product.brandName, product.imageUrl)
            val history = listOf(viewed) + decode(preferences[key(owner.userId)]).filterNot { it.barcode == viewed.barcode }
            preferences[key(owner.userId)] = gson.toJson(history.take(MAX_PRODUCTS))
        }
    }

    private fun key(owner: Long) = stringPreferencesKey("account_$owner")

    private fun decode(raw: String?): List<RecentlyViewedProduct> = runCatching {
        if (raw.isNullOrBlank()) return@runCatching emptyList()
        // Gson can populate null into Kotlin fields; validate persisted entries before UI access.
        gson.fromJson(raw, Array<StoredProduct>::class.java).orEmpty().mapNotNull { value ->
            val barcode = value?.barcode?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            val name = value.productName?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            RecentlyViewedProduct(barcode, name, value.brandName.orEmpty(), value.imageUrl)
        }.distinctBy { it.barcode }.take(MAX_PRODUCTS)
    }.getOrDefault(emptyList())

    private data class StoredProduct(val barcode: String?, val productName: String?, val brandName: String?, val imageUrl: String?)
    companion object { const val MAX_PRODUCTS = 50 }
}
