package com.coworker.jjikmuk.feature.scanner.presentation

import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.scanner.domain.*
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerAnalysisControllerTest {
    private val photo = PreparedScannerPhoto(File("test-photo"), 10, 10)
    private fun extracted(keyword: String = "포키") = OcrResult.Extracted(
        OcrExtraction(keyword, null, null, null, keyword, emptyList(), emptyMap()), OcrTrace("relay", "ocr"))
    private fun service(block: suspend () -> OcrResult) = object : ScannerOcrService {
        override val available = true
        override suspend fun recognize(photo: PreparedScannerPhoto) = block()
    }

    @Test fun `explicit capture runs once and stages wait for actual OCR and search`() = runTest {
        var calls = 0
        val ocr = CompletableDeferred<OcrResult>()
        val products = CompletableDeferred<Result<List<ProductSearchResult>>>()
        val controller = ScannerAnalysisController(this, service { calls++; ocr.await() }) { keyword ->
            assertEquals("포키", keyword)
            products.await()
        }
        runCurrent()
        assertEquals(0, calls)
        controller.analyze(photo)
        controller.analyze(photo)
        runCurrent()
        advanceTimeBy(10_000)
        assertEquals(ScannerPhase.Recognizing, controller.state.value.phase)
        assertEquals(1, calls)
        ocr.complete(extracted())
        runCurrent()
        assertEquals(ScannerPhase.Searching, controller.state.value.phase)
        products.complete(Result.success(emptyList()))
        runCurrent()
        assertEquals(ScannerPhase.Results, controller.state.value.phase)
        assertEquals("포키", controller.state.value.search.query)
        assertTrue(controller.state.value.search.products.isEmpty())
        assertNull(controller.state.value.search.errorMessage)
    }

    @Test fun `recognition failures never search while transport errors remain distinct`() = runTest {
        for (result in listOf(OcrResult.NoText(OcrTrace()),
            OcrResult.NoSearchKeyword(extracted().extraction, OcrTrace()),
            OcrResult.Failure(OcrFailure.OutcomeUnknown))) {
            var calls = 0
            val controller = ScannerAnalysisController(this, service { calls++; result }) { error("Must not search") }
            controller.analyze(photo)
            runCurrent()
            advanceTimeBy(200_000)
            assertEquals(1, calls)
            assertEquals(if (result is OcrResult.Failure) ScannerPhase.OcrError else ScannerPhase.RecognitionFailure,
                controller.state.value.phase)
        }
    }

    @Test fun `search retry keeps OCR keyword and never repeats OCR`() = runTest {
        var ocrCalls = 0
        var searches = 0
        val controller = ScannerAnalysisController(this, service { ocrCalls++; extracted() }) {
            if (++searches == 1) Result.failure(Exception("offline")) else Result.success(emptyList())
        }
        controller.analyze(photo)
        runCurrent()
        assertNotNull(controller.state.value.search.errorMessage)
        assertEquals("포키", controller.state.value.search.query)
        controller.submitSearch(controller.state.value.search.query)
        runCurrent()
        assertEquals(1, ocrCalls)
        assertEquals(2, searches)
        assertNull(controller.state.value.search.errorMessage)
    }

    @Test fun `late OCR after reset cannot replace a newer result`() = runTest {
        val old = CompletableDeferred<OcrResult>()
        var calls = 0
        val controller = ScannerAnalysisController(this, service {
            if (++calls == 1) withContext(NonCancellable) { old.await() } else extracted("새상품")
        }) { Result.success(emptyList()) }
        controller.analyze(photo)
        runCurrent()
        controller.reset()
        controller.analyze(photo.copy(file = File("next")))
        runCurrent()
        old.complete(extracted("이전상품"))
        runCurrent()
        assertEquals("새상품", controller.state.value.search.submittedQuery)
    }

    @Test fun `late search after new query or reset cannot restore stale results`() = runTest {
        val old = CompletableDeferred<Result<List<ProductSearchResult>>>()
        val controller = ScannerAnalysisController(this, service { error("Manual search must not OCR") }) { keyword ->
            if (keyword == "이전상품") withContext(NonCancellable) { old.await() } else Result.success(emptyList())
        }
        controller.manualSearch()
        controller.submitSearch("이전상품")
        runCurrent()
        controller.submitSearch("새상품")
        runCurrent()
        old.complete(Result.failure(Exception("late")))
        runCurrent()
        assertEquals("새상품", controller.state.value.search.submittedQuery)
        assertNull(controller.state.value.search.errorMessage)
        controller.reset()
        runCurrent()
        assertEquals(ScannerPhase.Camera, controller.state.value.phase)
    }

    @Test fun `repeated search taps send once and editing does not change submitted keyword`() = runTest {
        var calls = 0
        val pending = CompletableDeferred<Result<List<ProductSearchResult>>>()
        val controller = ScannerAnalysisController(this, service { extracted() }) { calls++; pending.await() }
        controller.manualSearch()
        controller.submitSearch("포키")
        controller.submitSearch("포키")
        runCurrent()
        controller.editQuery("다른상품")
        assertEquals(1, calls)
        pending.complete(Result.success(emptyList()))
        runCurrent()
        assertEquals("다른상품", controller.state.value.search.query)
        assertEquals("포키", controller.state.value.search.submittedQuery)
    }

    @Test fun `missing configuration and invalid OCR keywords fail without a search`() = runTest {
        for (service in listOf(UnavailableScannerOcrService(), service { extracted("!") })) {
            val controller = ScannerAnalysisController(this, service) { error("Must not search") }
            controller.analyze(photo)
            runCurrent()
            assertEquals(ScannerPhase.OcrError, controller.state.value.phase)
        }
    }
}
