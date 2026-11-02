package com.coworker.jjikmuk.feature.product.presentation

import com.coworker.jjikmuk.data.remote.auth.AccessTokenStore
import com.coworker.jjikmuk.data.repository.SafeRecommendationRepository
import com.coworker.jjikmuk.data.repository.recommendationMessage
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.data.local.preference.RecentSearchKeywordStore
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.domain.repository.FamilyProfileRepository
import com.coworker.jjikmuk.domain.repository.LikedProductRepository
import com.coworker.jjikmuk.domain.repository.ProductRepository
import com.coworker.jjikmuk.ui.component.ScanTargetMemberUiModel
import com.coworker.jjikmuk.ui.component.toScanTargetMembers
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val targets: com.coworker.jjikmuk.domain.repository.AnalysisTargetRepository,
    private val productRepository: ProductRepository,
    private val familyProfileRepository: FamilyProfileRepository,
    private val likedProductRepository: LikedProductRepository,
    private val recentSearchKeywordStore: RecentSearchKeywordStore,
    private val safeRecommendations: SafeRecommendationRepository,
    private val accessTokens: AccessTokenStore,
    private val recentlyViewedProducts: com.coworker.jjikmuk.data.local.preference.RecentlyViewedProductStore,
    private val productOptions: com.coworker.jjikmuk.data.repository.ProductOptionsRepository,
) : ViewModel() {

    private var detailOwner: com.coworker.jjikmuk.data.local.preference.ProductViewOwner? = null
    private var detailJob: Job? = null
    private var searchJob: Job? = null
    private var lastDetailRequest: Pair<String?, Long?>? = null
    private var alternativeJob: Job? = null
    val scanTargetMembers = targets.state.map { it.toScanTargetMembers() }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), emptyList())

    private val _searchUiState = MutableStateFlow(ProductSearchUiState())
    val searchUiState: StateFlow<ProductSearchUiState> = _searchUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(ProductDetailUiState())
    val detailUiState: StateFlow<ProductDetailUiState> = _detailUiState.asStateFlow()

    private val _recommendationUiState = MutableStateFlow(ProductRecommendationUiState(isLoading = true))
    val recommendationUiState: StateFlow<ProductRecommendationUiState> = _recommendationUiState.asStateFlow()
    private var nextRecommendationCursor: String? = null
    private var recommendationGeneration = 0
    private var recommendationPageJob: Job? = null

    val favoriteState = likedProductRepository.observeState().stateIn(
        viewModelScope, SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0),
        com.coworker.jjikmuk.domain.repository.LikedProductsState())
    private var detailFavoriteAccount: com.coworker.jjikmuk.domain.repository.FavoriteAccount? = null

    private val _recentSearchKeywords = MutableStateFlow<List<String>>(emptyList())
    val recentSearchKeywords: StateFlow<List<String>> = _recentSearchKeywords.asStateFlow()

    init {
        viewModelScope.launch {
            familyProfileRepository.ensureDefaultProfiles()
        }

        viewModelScope.launch {
            recentSearchKeywordStore.keywords.collect { keywords ->
                _recentSearchKeywords.value = keywords
            }
        }

        viewModelScope.launch {
            targets.state.map { it.selection.takeIf { _ -> it.ready } }.distinctUntilChanged().collectLatest { selection ->
                detailJob?.cancel(); alternativeJob?.cancel()
                recommendationGeneration++; recommendationPageJob?.cancel(); nextRecommendationCursor = null
                val detailRequest = lastDetailRequest
                if (detailRequest != null) _detailUiState.value = ProductDetailUiState(isLoading = selection != null,
                    errorMessage = if (selection == null) "분석 대상을 확인하고 있어요." else null)
                if (selection == null) {
                    _recommendationUiState.value = ProductRecommendationUiState(notice = "분석 대상을 확인하고 있어요.")
                } else {
                    detailRequest?.let { loadDetail(it.first, it.second) }
                    if (selection.mode == "none") _recommendationUiState.value = ProductRecommendationUiState(
                        notice = "선택한 대상 없음 · 개인 식이조건 미적용")
                    else loadSafeRecommendations()
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _searchUiState.update { state ->
            state.copy(query = query)
        }
    }

    fun clearSearchQuery() {
        _searchUiState.update { state ->
            state.copy(query = "")
        }
    }

    fun resetSearchState() {
        _searchUiState.value = ProductSearchUiState()
    }

    fun searchProducts(keyword: String = _searchUiState.value.query) {
        val trimmedKeyword = keyword.trim()
        if (trimmedKeyword.length < MIN_SEARCH_KEYWORD_LENGTH) {
            _searchUiState.update { state ->
                state.copy(
                    query = trimmedKeyword,
                    submittedQuery = trimmedKeyword,
                    products = emptyList(),
                    isLoading = false,
                    errorMessage = "검색어는 2글자 이상 입력해주세요.",
                    hasSearched = true,
                )
            }
            return
        }

        viewModelScope.launch {
            recentSearchKeywordStore.addKeyword(trimmedKeyword)
        }

        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _searchUiState.update { state ->
                state.copy(
                    query = trimmedKeyword,
                    submittedQuery = trimmedKeyword,
                    isLoading = true,
                    errorMessage = null,
                    hasSearched = true,
                )
            }

            productRepository.searchProducts(
                keyword = trimmedKeyword,
                allergies = emptyList(),
            )
                .onSuccess { products ->
                    _searchUiState.update { state ->
                        state.copy(
                            products = products,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _searchUiState.update { state ->
                        state.copy(
                            products = emptyList(),
                            isLoading = false,
                            errorMessage = throwable.message ?: "상품 검색에 실패했어요.",
                        )
                    }
                }
        }
    }

    fun deleteRecentSearchKeyword(keyword: String) {
        viewModelScope.launch {
            recentSearchKeywordStore.deleteKeyword(keyword)
        }
    }

    fun clearRecentSearchKeywords() {
        viewModelScope.launch {
            recentSearchKeywordStore.clearKeywords()
        }
    }

    fun loadProductDetail(barcode: String?) {
        val code = barcode?.takeIf(String::isNotBlank) ?: return
        loadDetail(code, null)
    }

    fun loadProductDetailById(productId: Long) {
        if (productId > 0) loadDetail(null, productId)
    }

    private fun loadDetail(code: String?, productId: Long?) {
        lastDetailRequest = code to productId
        val viewOwner = recentlyViewedProducts.captureOwner()
        detailOwner = viewOwner
        detailFavoriteAccount = likedProductRepository.captureAccount()
        detailJob?.cancel()
        alternativeJob?.cancel()
        _detailUiState.value = ProductDetailUiState(isLoading = true, referenceBarcode = code)
        detailJob = viewModelScope.launch {
            try {
                // Read server-owned conditions via the authenticated product API; no implicit profile writes.
                val product = (if (productId != null) productRepository.getProductDetailById(productId)
                    else productRepository.getProductDetail(requireNotNull(code))).getOrThrow()
                if (viewOwner != recentlyViewedProducts.captureOwner()) return@launch
                _detailUiState.value = ProductDetailUiState(product = product, referenceBarcode = code, capacitiesLoading = true)
                recordViewedProduct(product, viewOwner)
                refreshFavoriteStatus(product)
                try {
                    val capacities = productOptions.capacities(product.barcode)
                    _detailUiState.update { it.copy(capacities = capacities.options,
                        selectedCapacityKey = capacities.selectedKey, capacitiesLoading = false,
                        product = capacities.options.firstOrNull { option -> option.key == capacities.selectedKey }?.product ?: product) }
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { _detailUiState.update { it.copy(capacitiesLoading = false, capacityError = "중량 옵션을 불러오지 못했어요.") } }
                _detailUiState.value.product?.takeIf { it.barcode != product.barcode }?.let {
                    recordViewedProduct(it, viewOwner, alreadyRecorded = false)
                }
                _detailUiState.value.product?.let(::refreshFavoriteStatus)
                refreshAlternatives()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { _detailUiState.value = ProductDetailUiState(referenceBarcode = code,
                errorMessage = "상품 정보를 불러오지 못했어요. 서버 연결을 확인해 주세요.") }
        }
    }

    fun selectProductCapacity(key: String) {
        if (detailOwner != recentlyViewedProducts.captureOwner() || key == _detailUiState.value.selectedCapacityKey) return
        val option = _detailUiState.value.capacities.firstOrNull { it.key == key } ?: return
        _detailUiState.update { it.copy(product = option.product, selectedCapacityKey = key,
            alternatives = emptyList(), alternativesError = null) }
        recordViewedProduct(option.product, detailOwner, alreadyRecorded = false)
        refreshFavoriteStatus(option.product)
        refreshAlternatives()
    }

    private fun recordViewedProduct(
        product: ProductDetail,
        owner: com.coworker.jjikmuk.data.local.preference.ProductViewOwner?,
        alreadyRecorded: Boolean = true,
    ) {
        viewModelScope.launch {
            try { recentlyViewedProducts.record(product, owner, alreadyRecorded) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: java.io.IOException) { /* History storage failure must not hide a valid product detail. */ }
        }
    }

    fun refreshAlternatives() {
        alternativeJob?.cancel()
        val product = _detailUiState.value.product ?: return
        if (!product.personalConditionsApplied || product.safetyStatus != com.coworker.jjikmuk.domain.model.ProductSafetyStatus.Danger) {
            _detailUiState.update { it.copy(alternativesLoading = false, alternatives = emptyList()) }
            return
        }
        _detailUiState.update { it.copy(alternativesLoading = true, alternativesError = null) }
        alternativeJob = viewModelScope.launch {
            try {
                val data = productOptions.alternatives(product.barcode)
                if (_detailUiState.value.product?.barcode == product.barcode) _detailUiState.update {
                    it.copy(alternatives = data.products, alternativesNotice = data.notice, alternativesLoading = false)
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (_detailUiState.value.product?.barcode == product.barcode) _detailUiState.update {
                it.copy(alternativesLoading = false, alternativesError = "대체 상품을 불러오지 못했어요.") }
            }
        }
    }

    fun updateScanTargetSelection(memberId: String, checked: Boolean) {
        val account = targets.captureAccount()
        viewModelScope.launch { targets.setSelected(memberId, checked, account) }
    }

    private fun refreshFavoriteStatus(product: ProductDetail) {
        val id = product.productId?.takeIf { it > 0 } ?: return
        val account = detailFavoriteAccount ?: return
        viewModelScope.launch { likedProductRepository.refreshStatus(id, account) }
    }

    fun retryFavorites() {
        val product = _detailUiState.value.product ?: return
        refreshFavoriteStatus(product)
    }

    fun toggleLikedProduct(product: ProductDetail) {
        val id = product.productId?.takeIf { it > 0 } ?: return
        val account = detailFavoriteAccount ?: return
        if (account != likedProductRepository.captureAccount()) return
        val state = favoriteState.value
        if (state.account != account || id in state.pendingIds) return
        val selected = state.statuses[id] ?: return
        viewModelScope.launch { likedProductRepository.setFavorite(id, !selected, account) }
    }

    fun loadMoreSafeRecommendations() {
        val cursor = nextRecommendationCursor ?: return
        val state = _recommendationUiState.value
        if (state.isLoading || state.isLoadingMore || !state.canLoadMore) return
        val generation = recommendationGeneration
        _recommendationUiState.update { it.copy(isLoadingMore = true, errorMessage = null) }
        recommendationPageJob = viewModelScope.launch {
            try {
                val page = safeRecommendations.page(cursor, state.products.mapNotNull { it.barcode })
                if (generation != recommendationGeneration) return@launch
                check(page.userId == state.userId) { "추천 사용자가 변경되었어요." }
                check(page.nextCursor == null || page.nextCursor != cursor) { "추천 페이지가 반복되었어요." }
                nextRecommendationCursor = page.nextCursor
                _recommendationUiState.update { current -> current.copy(
                    products = (current.products + page.products).distinctBy { it.barcode },
                    isLoadingMore = false, canLoadMore = page.nextCursor != null, notice = page.notice,
                ) }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                if (generation == recommendationGeneration) _recommendationUiState.update {
                    it.copy(isLoadingMore = false, canLoadMore = false, errorMessage = error.recommendationMessage())
                }
            }
        }
    }

    fun retrySafeRecommendations() {
        if (_recommendationUiState.value.isLoading) return
        recommendationGeneration++
        recommendationPageJob?.cancel()
        nextRecommendationCursor = null
        _recommendationUiState.value = ProductRecommendationUiState(isLoading = true)
        recommendationPageJob = viewModelScope.launch {
            val me = familyProfileRepository.observeProfiles().first().firstOrNull { it.isMe }
            if (me == null) {
                _recommendationUiState.value = ProductRecommendationUiState(
                    errorMessage = "마이페이지에서 나의 식이 조건을 설정해 주세요.")
            } else loadSafeRecommendations()
        }
    }

    private suspend fun loadSafeRecommendations() {
        val selection = targets.awaitSelection()
        if (selection == null || selection.mode == "none") {
            _recommendationUiState.value = ProductRecommendationUiState(notice = if (selection == null) "분석 대상을 확인할 수 없어요." else "선택한 대상 없음 · 개인 식이조건 미적용")
            return
        }
        _recommendationUiState.value = ProductRecommendationUiState(isLoading = true)
        try {
            val owner = safeRecommendations.owner()
            val page = safeRecommendations.page()
            check(page.userId == owner.userId) { "추천 사용자가 변경되었어요." }
            nextRecommendationCursor = page.nextCursor
            _recommendationUiState.value = ProductRecommendationUiState(
                products = page.products, userName = owner.userName, userId = page.userId,
                canLoadMore = page.nextCursor != null, notice = page.notice,
            )
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            _recommendationUiState.value = ProductRecommendationUiState(errorMessage = error.recommendationMessage())
        }
    }

    private fun selectedAllergies(): List<String> {
        return scanTargetMembers.value
            .filter { member -> member.isSelected }
            .flatMap { member -> member.allergies }
            .map(String::trim)
            .filter(String::isNotBlank)
            .distinct()
    }

    private fun FamilyProfile?.toRecommendationConditionKey(): RecommendationConditionKey {
        return RecommendationConditionKey(
            vegetarian = this?.vegetarian.orEmpty(),
            allergies = this?.allergies.orEmpty().sorted(),
            preferences = this?.preferences.orEmpty().sorted(),
        )
    }

    private companion object {
        const val MIN_SEARCH_KEYWORD_LENGTH = 2
    }
}

private data class RecommendationConditionKey(
    val vegetarian: String,
    val allergies: List<String>,
    val preferences: List<String>,
)

private fun String.toAllergyKey(): String? {
    return when (trim()) {
        "계란", "egg" -> "egg"
        "우유", "milk" -> "milk"
        "대두", "soy" -> "soy"
        "밀", "밀가루", "wheat" -> "wheat"
        "돼지고기", "pork" -> "pork"
        "닭고기", "chicken" -> "chicken"
        "새우", "shrimp" -> "shrimp"
        "게", "crab" -> "crab"
        "오징어", "squid" -> "squid"
        "고등어", "mackerel" -> "mackerel"
        "조개류", "shellfish" -> "shellfish"
        "굴", "oyster" -> "oyster"
        "홍합", "mussel" -> "mussel"
        "전복", "abalone" -> "abalone"
        "복숭아", "peach" -> "peach"
        "토마토", "tomato" -> "tomato"
        "땅콩", "peanut" -> "peanut"
        "호두", "walnut" -> "walnut"
        "메밀", "buckwheat" -> "buckwheat"
        "잣", "pine_nut", "pineNut" -> "pine_nut"
        "아황산류", "sulfites" -> "sulfites"
        "참깨", "sesame" -> "sesame"
        "아몬드", "almond" -> "almond"
        "머스타드", "mustard" -> "mustard"
        "셀러리", "celery" -> "celery"
        "소고기", "beef" -> "beef"
        else -> null
    }
}
