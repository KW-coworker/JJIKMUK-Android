package com.coworker.jjikmuk.feature.scanner.presentation

import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.feature.scanner.domain.*
import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerListControllerTest {
    private val photo = PreparedScannerPhoto(File("not-uploaded"), 10, 10)
    private val products = listOf("후보B", "후보A").mapIndexed { i, name ->
        ProductSearchResult("NO_BARCODE_ROW_$i", name, "제조사", null, emptyList())
    }
    private fun result() = ScanListResult.Products(products, OcrTrace("local-test"))
    private val noOldOcr = object : ScannerOcrService {
        override val available = true
        override suspend fun recognize(photo: PreparedScannerPhoto): OcrResult = error("Old OCR must not run")
    }

    @Test fun `blank field preserves list order and does not issue keyword search`() = runTest {
        val c = ScannerAnalysisController(this, noOldOcr, ScannerListService { result() }) { error("No keyword call") }
        c.analyze(photo)
        runCurrent()
        assertEquals(ScannerPhase.Results, c.state.value.phase)
        assertEquals("", c.state.value.search.query)
        assertEquals("", c.state.value.search.submittedQuery)
        assertEquals(products, c.state.value.search.products)
        assertEquals("NO_BARCODE_ROW_0", c.state.value.search.products[0].barcode)
        c.editQuery("직접검색")
        assertEquals(products, c.state.value.search.products)
    }

    @Test fun `manual submission uses old keyword search without another scan`() = runTest {
        var scans = 0
        var searches = 0
        val c = ScannerAnalysisController(this, noOldOcr, ScannerListService { scans++; result() }) {
            searches++; assertEquals("직접검색", it); Result.success(products.reversed())
        }
        c.analyze(photo); runCurrent()
        c.submitSearch("직접검색"); runCurrent()
        assertEquals(1, scans); assertEquals(1, searches)
        assertEquals(products.reversed(), c.state.value.search.products)
        assertEquals("직접검색", c.state.value.search.submittedQuery)
    }

    @Test fun `double capture starts once and failure is not automatically retried`() = runTest {
        var scans = 0
        val pending = CompletableDeferred<ScanListResult>()
        val c = ScannerAnalysisController(this, noOldOcr, ScannerListService { scans++; pending.await() }) { error("No query") }
        c.analyze(photo); c.analyze(photo); runCurrent()
        assertEquals(1, scans)
        pending.complete(ScanListResult.Failure(OcrResult.Failure(OcrFailure.OutcomeUnknown)))
        runCurrent()
        assertEquals(ScannerPhase.OcrError, c.state.value.phase)
        assertEquals(1, scans)
    }

    @Test fun `late scan does not overwrite manual search`() = runTest {
        val pending = CompletableDeferred<ScanListResult>()
        val c = ScannerAnalysisController(this, noOldOcr, ScannerListService {
            withContext(NonCancellable) { pending.await() }
        }) { Result.success(products.reversed()) }
        c.analyze(photo); runCurrent()
        c.manualSearch(); c.submitSearch("직접검색"); runCurrent()
        pending.complete(result()); runCurrent()
        assertEquals("직접검색", c.state.value.search.query)
        assertEquals(products.reversed(), c.state.value.search.products)
    }

    @Test fun `weak evidence goes to retake or manual route and never fabricates a match`() = runTest {
        val c = ScannerAnalysisController(this, noOldOcr,
            ScannerListService { ScanListResult.ManualSearchRequired(OcrTrace("test")) }) { error("No query") }
        c.analyze(photo); runCurrent()
        assertEquals(ScannerPhase.RecognitionFailure, c.state.value.phase)
        c.manualSearch()
        assertEquals(ScannerPhase.Results, c.state.value.phase)
        assertEquals("", c.state.value.search.query)
    }
}
