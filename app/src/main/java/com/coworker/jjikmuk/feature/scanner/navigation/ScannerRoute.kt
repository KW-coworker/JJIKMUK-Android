package com.coworker.jjikmuk.feature.scanner.navigation

import android.net.Uri

internal object ScannerRoute {
    const val Main = "scanner/main"
    const val AnalysisReport = "scanner/analysis-report"
    const val ProductDetailBarcodeArg = "barcode"
    const val ProductDetail = "scanner/product/{$ProductDetailBarcodeArg}"

    /** Barcodes are passed through as opaque strings (leading zeros, non-digits kept). */
    fun productDetail(barcode: String): String = "scanner/product/${Uri.encode(barcode)}"
}
