package com.coworker.jjikmuk.feature.scanner.data

import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.AuthorizedOperations
import com.coworker.jjikmuk.feature.scanner.domain.*

fun interface ScannerListTransport {
    suspend fun scan(photo: PreparedScannerPhoto, accessToken: String): ScanListResult
}

class AuthenticatedScannerListService(private val transport: ScannerListTransport,
                                     private val operations: AuthorizedOperations) : ScannerListService {
    override suspend fun scan(photo: PreparedScannerPhoto): ScanListResult = when (val result = operations.execute { token ->
        val value = transport.scan(photo, token)
        if (value is ScanListResult.Failure && (value.failure.httpStatus == 401 ||
                    (value.failure.httpStatus == 403 && value.failure.code == "PROFILE_INCOMPLETE")))
            AuthResult.Failure(AuthFailure(AuthFailureKind.Server, value.failure.httpStatus, value.failure.code))
        else AuthResult.Success(value)
    }) {
        is AuthResult.Success -> result.value
        is AuthResult.Failure -> ScanListResult.Failure(OcrResult.Failure(
            if (result.error.status == 401 || result.error.code == "SESSION_CHANGED") OcrFailure.Unauthorized
            else OcrFailure.ServiceUnavailable, result.error.status, result.error.code))
    }
}
