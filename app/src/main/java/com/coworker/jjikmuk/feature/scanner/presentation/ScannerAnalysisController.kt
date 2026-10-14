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
