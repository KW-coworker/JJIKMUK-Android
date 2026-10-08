package com.coworker.jjikmuk.feature.scanner.presentation

import com.coworker.jjikmuk.feature.scanner.domain.*
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerAnalysisControllerTest {
    private val photo = PreparedScannerPhoto(File("test-photo"), 10, 10)

    @Test fun `no request on construction and duplicate clicks start just one call`() = runTest {
        var calls = 0
        val result = CompletableDeferred<OcrResult>()
        val controller = ScannerAnalysisController(this, service { calls++; result.await() })
        runCurrent()
        assertEquals(0, calls)
        controller.analyze(photo)
        controller.analyze(photo)
        runCurrent()
        assertEquals(1, calls)
        assertTrue(controller.state.value is ScannerAnalysisState.Running)
        result.complete(OcrResult.NoText(OcrTrace()))
        runCurrent()
        assertTrue(controller.state.value is ScannerAnalysisState.Finished)
    }

    @Test fun `late response after photo reset cannot overwrite next photo result`() = runTest {
        val old = CompletableDeferred<OcrResult>()
        var calls = 0
        val controller = ScannerAnalysisController(this, service {
            if (++calls == 1) withContext(NonCancellable) { old.await() }
            else OcrResult.NoText(OcrTrace("new"))
        })
        controller.analyze(photo)
        runCurrent()
        controller.reset()
        controller.analyze(photo.copy(file = File("next-photo")))
        runCurrent()
        val next = controller.state.value
        old.complete(OcrResult.Failure(OcrFailure.OutcomeUnknown))
        runCurrent()
        assertEquals(next, controller.state.value)
    }

    @Test fun `failure never retries until a new explicit action`() = runTest {
        var calls = 0
        val controller = ScannerAnalysisController(this, service { calls++; OcrResult.Failure(OcrFailure.OutcomeUnknown) })
        controller.analyze(photo)
        runCurrent()
        runCurrent()
        assertEquals(1, calls)
        controller.analyze(photo)
        runCurrent()
        assertEquals(2, calls)
    }

    @Test fun `disabled production service cannot start analysis`() = runTest {
        val controller = ScannerAnalysisController(this, UnavailableScannerOcrService())
        controller.analyze(photo)
        runCurrent()
        assertEquals(ScannerAnalysisState.Idle, controller.state.value)
        assertFalse(controller.available)
    }

    private fun service(block: suspend () -> OcrResult) = object : ScannerOcrService {
        override val available = true
        override suspend fun recognize(photo: PreparedScannerPhoto) = block()
    }
}
