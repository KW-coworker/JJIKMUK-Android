package com.coworker.jjikmuk.feature.scanner.domain

data class OcrTrace(val relayRequestId: String? = null, val ocrRequestId: String? = null)
data class OcrEvidence(val sourceId: String?, val text: String?)
data class OcrExtraction(
    val productName: String?,
    val brand: String?,
    val flavor: String?,
    val totalWeight: String?,
    val searchKeyword: String?,
    val warnings: List<String>,
    val evidence: Map<String, List<OcrEvidence>>,
)

sealed interface OcrResult {
    data class Extracted(val extraction: OcrExtraction, val trace: OcrTrace) : OcrResult
    data class NoText(val trace: OcrTrace) : OcrResult
    data class NoSearchKeyword(val extraction: OcrExtraction, val trace: OcrTrace) : OcrResult
    data class Failure(
        val reason: OcrFailure,
        val httpStatus: Int? = null,
        val code: String? = null,
        val trace: OcrTrace = OcrTrace(),
        val retryAfterSeconds: Int? = null,
    ) : OcrResult
}

enum class OcrFailure {
    NotConfigured, InvalidImage, Unauthorized, Busy, RateLimited, ServiceUnavailable,
    OutcomeUnknown, InvalidResponse, Rejected,
}

interface ScannerOcrService {
    val available: Boolean
    suspend fun recognize(photo: PreparedScannerPhoto): OcrResult
}

/** Explicitly disabled until team access/token injection and the product flow are agreed. */
class UnavailableScannerOcrService : ScannerOcrService {
    override val available = false
    override suspend fun recognize(photo: PreparedScannerPhoto) = OcrResult.Failure(OcrFailure.NotConfigured)
}
