package com.coworker.jjikmuk.feature.scanner.presentation

import com.coworker.jjikmuk.feature.scanner.domain.OcrFailure
import com.coworker.jjikmuk.feature.scanner.domain.OcrResult

internal fun OcrResult.Failure.userMessage(): String = when (reason) {
    OcrFailure.NotConfigured -> "스캔 서비스 연결이 아직 준비되지 않았어요. 제품명을 직접 검색해 주세요."
    OcrFailure.InvalidImage -> "사진을 전송할 수 없어요. 다른 사진을 선택하거나 다시 촬영해 주세요."
    OcrFailure.Unauthorized -> "스캔 서비스에 접속할 수 없어요. 제품명을 직접 검색해 주세요."
    OcrFailure.Busy, OcrFailure.RateLimited -> "스캔 요청이 많아요. 잠시 후 다시 촬영하거나 제품명을 직접 검색해 주세요."
    OcrFailure.ServiceUnavailable -> "현재 스캔 서비스를 사용할 수 없어요. 제품명을 직접 검색해 주세요."
    OcrFailure.OutcomeUnknown -> "스캔 결과를 확인하지 못했어요. 요청이 이미 처리됐을 수 있어 자동으로 다시 보내지 않아요. 제품명을 직접 검색할 수 있어요."
    OcrFailure.InvalidResponse, OcrFailure.Rejected -> "스캔 결과를 불러오지 못했어요. 잠시 후 다시 촬영하거나 제품명을 직접 검색해 주세요."
}
