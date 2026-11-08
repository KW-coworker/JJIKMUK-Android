package com.coworker.jjikmuk.feature.scanner.domain

import com.coworker.jjikmuk.domain.model.ProductSearchResult

/** Opt-in ranked-list scan. Old OCR contract remains independent. */
fun interface ScannerListService {
    suspend fun scan(photo: PreparedScannerPhoto): ScanListResult
}

sealed interface ScanListResult {
    data class Products(val products: List<ProductSearchResult>, val trace: OcrTrace) : ScanListResult
    data class ManualSearchRequired(val trace: OcrTrace) : ScanListResult
    data class Failure(val failure: OcrResult.Failure) : ScanListResult
}
