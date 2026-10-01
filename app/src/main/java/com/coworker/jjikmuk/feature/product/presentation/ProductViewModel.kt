package com.coworker.jjikmuk.feature.product.presentation

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
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val familyProfileRepository: FamilyProfileRepository,
    private val likedProductRepository: LikedProductRepository,
    private val recentSearchKeywordStore: RecentSearchKeywordStore,
) : ViewModel() {

    private val selectedProfileIds = MutableStateFlow<Set<String>?>(null)

    val scanTargetMembers: StateFlow<List<ScanTargetMemberUiModel>> =
        familyProfileRepository.observeProfiles()
            .combine(selectedProfileIds) { profiles, selectedIds ->
                val nextSelectedIds = selectedIds.syncWithProfiles(profiles)
                if (nextSelectedIds != selectedIds) {
                    selectedProfileIds.value = nextSelectedIds
                }
                profiles.map { profile ->
                    profile.toScanTargetMemberUiModel(
                        isSelected = profile.id in nextSelectedIds,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    private val _searchUiState = MutableStateFlow(ProductSearchUiState())
    val searchUiState: StateFlow<ProductSearchUiState> = _searchUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(ProductDetailUiState())
    val detailUiState: StateFlow<ProductDetailUiState> = _detailUiState.asStateFlow()

    private val _recommendationUiState = MutableStateFlow(ProductRecommendationUiState(isLoading = true))
    val recommendationUiState: StateFlow<ProductRecommendationUiState> = _recommendationUiState.asStateFlow()
    private var currentRecommendationConditionKey: RecommendationConditionKey? = null

    val likedProductBarcodes: StateFlow<Set<String>> =
        likedProductRepository.observeLikedProductBarcodes()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptySet(),
            )

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
            familyProfileRepository.observeProfiles()
                .map { profiles ->
                    profiles.firstOrNull { profile -> profile.isMe }.toRecommendationConditionKey()
                }
                .distinctUntilChanged()
                .collectLatest { conditionKey ->
                    currentRecommendationConditionKey = conditionKey
                    loadSafeRecommendations(conditionKey)
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

        viewModelScope.launch {
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
                allergies = selectedAllergies(),
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
        val productBarcode = barcode?.takeIf(String::isNotBlank) ?: return

        viewModelScope.launch {
            _detailUiState.update { state ->
                state.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            productRepository.getProductDetail(
                barcode = productBarcode,
                allergies = selectedAllergies(),
            )
                .onSuccess { product ->
                    _detailUiState.update {
                        ProductDetailUiState(
                            isLoading = false,
                            product = product,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _detailUiState.update {
                        ProductDetailUiState(
                            isLoading = false,
                            product = null,
                            errorMessage = throwable.message ?: "상품 상세 정보를 불러오지 못했어요.",
                        )
                    }
                }
        }
    }

    fun updateScanTargetSelection(
        memberId: String,
        checked: Boolean,
    ) {
        selectedProfileIds.update { current ->
            val currentSelectedIds = current.orEmpty()
            if (checked) currentSelectedIds + memberId else currentSelectedIds - memberId
        }
    }

    fun toggleLikedProduct(product: ProductDetail) {
        viewModelScope.launch {
            likedProductRepository.toggleLikedProduct(product)
        }
    }

    fun loadMoreSafeRecommendations() {
        val conditionKey = currentRecommendationConditionKey ?: return
        val currentState = _recommendationUiState.value
        if (currentState.isLoading || currentState.isLoadingMore || !currentState.canLoadMore) return

        viewModelScope.launch {
            _recommendationUiState.update { state ->
                state.copy(isLoadingMore = true, errorMessage = null)
            }

            productRepository.getSafeRecommendations(
                filters = conditionKey.toProductFilters(),
                allergies = conditionKey.toAllergyKeys(),
                limit = SAFE_RECOMMENDATION_LIMIT,
            )
                .onSuccess { products ->
                    val existingBarcodes = _recommendationUiState.value.products
                        .mapNotNull(ProductSearchResult::barcode)
                        .toSet()
                    val nextProducts = products.filterNot { product ->
                        product.barcode != null && product.barcode in existingBarcodes
                    }
                    _recommendationUiState.update { state ->
                        state.copy(
                            products = state.products + nextProducts,
                            isLoadingMore = false,
                            canLoadMore = nextProducts.size >= SAFE_RECOMMENDATION_LIMIT,
                            errorMessage = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _recommendationUiState.update { state ->
                        state.copy(
                            isLoadingMore = false,
                            canLoadMore = false,
                            errorMessage = throwable.message ?: "맞춤 안심 상품을 더 불러오지 못했어요.",
                        )
                    }
                }
        }
    }

    private suspend fun loadSafeRecommendations(conditionKey: RecommendationConditionKey) {
        _recommendationUiState.update { state ->
            state.copy(isLoading = true, errorMessage = null)
        }

        productRepository.getSafeRecommendations(
            filters = conditionKey.toProductFilters(),
            allergies = conditionKey.toAllergyKeys(),
            limit = SAFE_RECOMMENDATION_LIMIT,
        )
            .onSuccess { products ->
                _recommendationUiState.value = ProductRecommendationUiState(
                    products = products,
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = products.size >= SAFE_RECOMMENDATION_LIMIT,
                    errorMessage = null,
                )
            }
            .onFailure { throwable ->
                _recommendationUiState.value = ProductRecommendationUiState(
                    products = emptyList(),
                    isLoading = false,
                    isLoadingMore = false,
                    canLoadMore = false,
                    errorMessage = throwable.message ?: "맞춤 안심 상품을 불러오지 못했어요.",
                )
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

    private fun Set<String>?.syncWithProfiles(
        profiles: List<FamilyProfile>,
    ): Set<String> {
        val profileIds = profiles.map { profile -> profile.id }.toSet()
        if (this == null) return profileIds

        return intersect(profileIds)
    }

    private fun FamilyProfile.toScanTargetMemberUiModel(
        isSelected: Boolean,
    ): ScanTargetMemberUiModel {
        return ScanTargetMemberUiModel(
            id = id,
            name = name,
            relation = if (isMe) "나" else relation,
            emoji = emoji,
            isSelected = isSelected,
            vegetarian = vegetarian,
            allergies = allergies,
            preferences = preferences,
        )
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
        const val SAFE_RECOMMENDATION_LIMIT = 6
    }
}

private data class RecommendationConditionKey(
    val vegetarian: String,
    val allergies: List<String>,
    val preferences: List<String>,
) {
    fun toProductFilters(): List<String> {
        return listOfNotNull(vegetarian)
            .plus(preferences)
            .mapNotNull(String::toProductFilterKey)
            .distinct()
    }

    fun toAllergyKeys(): List<String> {
        return allergies
            .mapNotNull(String::toAllergyKey)
            .distinct()
    }
}

private fun String.toProductFilterKey(): String? {
    return when (trim()) {
        "비건", "vegan" -> "vegan"
        "락토", "lacto", "lactoVegetarian" -> "lactoVegetarian"
        "오보", "ovo", "ovoVegetarian" -> "ovoVegetarian"
        "락토오보", "락토 오보", "lacto_ovo", "lactoOvo", "lactoOvoVegetarian" -> "lactoOvoVegetarian"
        "페스코", "pesco", "pescatarian" -> "pescatarian"
        "폴로", "pollo", "pollotarian" -> "pollotarian"
        "저당", "low_sugar", "lowSugar" -> "lowSugar"
        "저염", "low_sodium", "lowSodium" -> "lowSodium"
        "글루텐프리", "gluten_free", "glutenFree" -> "glutenFree"
        "저칼로리", "low_calorie", "lowCalorie" -> "lowCalorie"
        "저지방", "low_fat", "lowFat" -> "lowFat"
        "고단백", "high_protein", "highProtein" -> "highProtein"
        else -> null
    }
}

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
