package com.coworker.jjikmuk.feature.scanner.presentation

import com.coworker.jjikmuk.feature.scanner.domain.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ScannerAnalysisState {
    data object Idle : ScannerAnalysisState
    data class Running(val photoId: String) : ScannerAnalysisState
    data class Finished(val photoId: String, val result: OcrResult) : ScannerAnalysisState
}

/** Main-thread state owner. Starts only on explicit actions, never on photo selection/recollection. */
internal class ScannerAnalysisController(
    private val scope: CoroutineScope,
    private val service: ScannerOcrService,
) {
    private val mutableState = MutableStateFlow<ScannerAnalysisState>(ScannerAnalysisState.Idle)
    val state = mutableState.asStateFlow()
    val available get() = service.available
    private var generation = 0L
    private var job: Job? = null

    fun analyze(photo: PreparedScannerPhoto) {
        if (!available || state.value is ScannerAnalysisState.Running) return
        val request = ++generation
        mutableState.value = ScannerAnalysisState.Running(photo.id)
        job = scope.launch {
            val result = try {
                service.recognize(photo)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                OcrResult.Failure(OcrFailure.OutcomeUnknown)
            }
            if (request == generation) mutableState.value = ScannerAnalysisState.Finished(photo.id, result)
        }
    }

    fun reset() {
        generation++
        job?.cancel()
        job = null
        mutableState.value = ScannerAnalysisState.Idle
    }
}

internal fun scannerAnalysisMessage(state: ScannerAnalysisState, available: Boolean): String = when (state) {
    ScannerAnalysisState.Idle -> if (available) "상품 전면이 잘 보이는 사진인지 확인해 주세요." else "사진 분석 기능은 준비 중이에요."
    is ScannerAnalysisState.Running -> "사진의 글자를 읽고 있어요. 잠시 기다려 주세요."
    is ScannerAnalysisState.Finished -> when (val result = state.result) {
        is OcrResult.Extracted -> "읽은 검색어: ${result.extraction.searchKeyword}\n상품 확인이 필요해요."
        is OcrResult.NoText -> "글자를 찾지 못했어요. 상품 전면을 다시 촬영해 주세요."
        is OcrResult.NoSearchKeyword -> "상품명을 찾지 못했어요. 다른 사진을 선택해 주세요."
        is OcrResult.Failure -> when (result.reason) {
            OcrFailure.NotConfigured -> "사진 분석 기능은 준비 중이에요."
            OcrFailure.Busy, OcrFailure.RateLimited -> "현재 분석 요청이 많아요. 잠시 후 다시 시도해 주세요."
            OcrFailure.OutcomeUnknown -> "분석 결과를 확인하지 못했어요. 다시 실행하면 같은 사진이 중복 처리될 수 있어요."
            OcrFailure.InvalidImage -> "사진을 처리할 수 없어요. 다른 사진을 선택해 주세요."
            else -> "사진 분석을 완료하지 못했어요. 잠시 후 다시 시도해 주세요."
        }
    }
}
