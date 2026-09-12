package com.coworker.jjikmuk.feature.product.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.data.local.preference.RecentSearchKeywordStore
import com.coworker.jjikmuk.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class ProductViewModel @Inject constructor(
    private val productRepository: ProductRepository,
    private val recentSearchKeywordStore: RecentSearchKeywordStore,
) : ViewModel() {

    private val _searchUiState = MutableStateFlow(ProductSearchUiState())
    val searchUiState: StateFlow<ProductSearchUiState> = _searchUiState.asStateFlow()

    private val _detailUiState = MutableStateFlow(ProductDetailUiState())
    val detailUiState: StateFlow<ProductDetailUiState> = _detailUiState.asStateFlow()

    private val _recentSearchKeywords = MutableStateFlow<List<String>>(emptyList())
    val recentSearchKeywords: StateFlow<List<String>> = _recentSearchKeywords.asStateFlow()

    init {
        viewModelScope.launch {
            recentSearchKeywordStore.keywords.collect { keywords ->
                _recentSearchKeywords.value = keywords
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

            productRepository.searchProducts(trimmedKeyword)
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

            productRepository.getProductDetail(productBarcode)
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

    private companion object {
        const val MIN_SEARCH_KEYWORD_LENGTH = 2
    }
}
