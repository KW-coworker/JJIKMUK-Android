package com.coworker.jjikmuk.feature.scanner.presentation

import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.product.presentation.ProductSearchUiState
import com.coworker.jjikmuk.feature.scanner.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal enum class ScannerPhase { Camera, Recognizing, Searching, Results, RecognitionFailure, OcrError }

internal data class ScannerAnalysisState(
    val phase: ScannerPhase = ScannerPhase.Camera,
    val search: ProductSearchUiState = ProductSearchUiState(),
    val failure: OcrResult.Failure? = null,
    val trace: OcrTrace? = null,
)

/** Main-thread owner. Only explicit actions start work; cancellation invalidates late responses. */
internal class ScannerAnalysisController(
    private val scope: CoroutineScope,
    private val service: ScannerOcrService,
    private val searchProducts: suspend (String) -> Result<List<ProductSearchResult>>,
) {
    private val mutableState = MutableStateFlow(ScannerAnalysisState())
    val state = mutableState.asStateFlow()
    private var generation = 0L
    private var job: Job? = null

    fun analyze(photo: PreparedScannerPhoto) {
        if (state.value.phase != ScannerPhase.Camera) return
        val request = ++generation
        mutableState.value = ScannerAnalysisState(phase = ScannerPhase.Recognizing)
        job = scope.launch {
            val result = try {
                if (service.available) service.recognize(photo)
                else OcrResult.Failure(OcrFailure.NotConfigured)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                OcrResult.Failure(OcrFailure.OutcomeUnknown)
            }
            ensureActive()
            if (request != generation) return@launch
            when (result) {
                is OcrResult.Extracted -> {
                    val keyword = result.extraction.searchKeyword.orEmpty()
                    if (!validKeyword(keyword)) {
                        mutableState.value = ScannerAnalysisState(ScannerPhase.OcrError,
                            failure = OcrResult.Failure(OcrFailure.InvalidResponse, trace = result.trace))
                    } else {
                        mutableState.value = ScannerAnalysisState(ScannerPhase.Searching,
                            search = ProductSearchUiState(query = keyword, submittedQuery = keyword,
                                isLoading = true, hasSearched = true), trace = result.trace)
                        performSearch(keyword, request)
                    }
                }
                is OcrResult.NoText -> mutableState.value = ScannerAnalysisState(
                    ScannerPhase.RecognitionFailure, trace = result.trace)
                is OcrResult.NoSearchKeyword -> mutableState.value = ScannerAnalysisState(
                    ScannerPhase.RecognitionFailure, trace = result.trace)
                is OcrResult.Failure -> mutableState.value = ScannerAnalysisState(
                    ScannerPhase.OcrError, failure = result, trace = result.trace)
            }
        }
    }

    fun editQuery(query: String) {
        mutableState.update { it.copy(search = it.search.copy(query = query)) }
    }

    fun manualSearch() {
        reset()
        mutableState.value = ScannerAnalysisState(phase = ScannerPhase.Results)
    }

    fun submitSearch(query: String) {
        if (state.value.phase != ScannerPhase.Results) return
        val keyword = query.trim()
        if (state.value.search.isLoading && state.value.search.submittedQuery == keyword) return
        val request = ++generation
        job?.cancel()
        mutableState.update { it.copy(search = ProductSearchUiState(
            query = keyword, submittedQuery = keyword, hasSearched = true,
            isLoading = validKeyword(keyword),
            errorMessage = if (validKeyword(keyword)) null else "검색어는 문자나 숫자 2개를 포함해 2~100자로 입력해 주세요.",
        )) }
        if (validKeyword(keyword)) job = scope.launch { performSearch(keyword, request) }
    }

    private suspend fun performSearch(keyword: String, request: Long) {
        val result = try {
            searchProducts(keyword)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            Result.failure(error)
        }
        // Guard even dependencies that return after the caller has been cancelled.
        kotlin.coroutines.coroutineContext.ensureActive()
        if (request != generation) return
        mutableState.update { it.copy(phase = ScannerPhase.Results, search = it.search.copy(
            products = result.getOrNull().orEmpty(), isLoading = false,
            errorMessage = if (result.isFailure) "상품 검색에 실패했어요. 검색 버튼을 눌러 다시 시도해 주세요." else null,
        )) }
    }

    fun reset() {
        generation++
        job?.cancel()
        job = null
        mutableState.value = ScannerAnalysisState()
    }

    private fun validKeyword(value: String) = value.length in 2..100 && value.count(Char::isLetterOrDigit) >= 2
}
