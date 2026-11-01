package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.AuthorizedOperations
import com.coworker.jjikmuk.feature.scanner.domain.*

fun interface ScannerOcrTransport {
    suspend fun recognize(photo: PreparedScannerPhoto, accessToken: String): OcrResult
}

class AuthenticatedScannerOcrService(
    private val transport: ScannerOcrTransport, private val operations: AuthorizedOperations,
) : ScannerOcrService {
    override val available = true
    override suspend fun recognize(photo: PreparedScannerPhoto): OcrResult =
        when (val result = operations.execute { token ->
            val ocr = transport.recognize(photo, token)
            if (ocr is OcrResult.Failure && ocr.httpStatus == 401)
                AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 401, ocr.code))
            else AuthResult.Success(ocr)
        }) {
            is AuthResult.Success -> result.value
            is AuthResult.Failure -> OcrResult.Failure(
                reason = when {
                    result.error.status == 401 || result.error.code in setOf("AUTHENTICATION_REQUIRED", "AUTH_TOKEN_INVALID",
                        "AUTH_TOKEN_EXPIRED", "REFRESH_TOKEN_INVALID", "REFRESH_TOKEN_EXPIRED", "REFRESH_OUTCOME_UNKNOWN", "SESSION_CHANGED") -> OcrFailure.Unauthorized
                    result.error.kind == AuthFailureKind.NotConfigured -> OcrFailure.NotConfigured
                    else -> OcrFailure.ServiceUnavailable
                }, httpStatus = result.error.status, code = result.error.code)
        }
}
