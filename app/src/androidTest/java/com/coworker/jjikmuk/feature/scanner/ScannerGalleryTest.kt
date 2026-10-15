package com.coworker.jjikmuk.feature.scanner

import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.domain.model.ProductDetail
import com.coworker.jjikmuk.domain.model.ProductSearchResult
import com.coworker.jjikmuk.domain.repository.ProductRepository
import com.coworker.jjikmuk.feature.scanner.data.ScannerImagePreparer
import com.coworker.jjikmuk.feature.scanner.domain.*
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerCaptureViewModel
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerGalleryConfirmation
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerPhase
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ScannerGalleryTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun galleryRequiresConfirmationAndCancelDeletesOnlyImportedCopy() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val original = File.createTempFile("gallery-original", ".jpg", context.cacheDir)
        val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
        original.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
        bitmap.recycle()
        var ocrCalls = 0
        var searches = 0
        val ocr = object : ScannerOcrService {
            override val available = true
            override suspend fun recognize(photo: PreparedScannerPhoto): OcrResult {
                ocrCalls++
                return OcrResult.Extracted(OcrExtraction("포키", null, null, null, "포키", emptyList(), emptyMap()), OcrTrace())
            }
        }
        val repository = object : ProductRepository {
            override suspend fun searchProducts(keyword: String, allergies: List<String>): Result<List<ProductSearchResult>> {
                assertEquals("포키", keyword)
                searches++
                return Result.success(emptyList())
            }
            override suspend fun getSafeRecommendations(filters: List<String>, allergies: List<String>, limit: Int) = Result.success(emptyList<ProductSearchResult>())
            override suspend fun getProductDetail(barcode: String, allergies: List<String>): Result<ProductDetail> = error("Detail is deferred")
        }
        lateinit var model: ScannerCaptureViewModel
        compose.runOnIdle { model = ScannerCaptureViewModel(context, ScannerImagePreparer(), ocr, repository) }
        try {
            compose.setContent {
                val state by model.state.collectAsState()
                JjikmukTheme {
                    Text("촬영 화면")
                    if (state.galleryConfirmation) ScannerGalleryConfirmation(
                        checkNotNull(state.photo).file, model::confirmGalleryScan, model::cancelScan)
                }
            }
            fun select() {
                compose.runOnIdle { model.selectGalleryPhoto(Uri.fromFile(original)) }
                compose.waitUntil(5_000) { model.state.value.galleryConfirmation }
            }
            select()
            compose.onNodeWithText("선택한 사진 확인").assertIsDisplayed()
            val imported = checkNotNull(model.state.value.photo).file
            assertEquals(0, ocrCalls)
            compose.onNodeWithText("취소").performClick()
            compose.runOnIdle {
                assertFalse(imported.exists())
                assertTrue(original.exists())
                assertEquals(0, ocrCalls)
            }
            select()
            compose.onNodeWithText("이 사진으로 스캔").performClick()
            compose.runOnIdle { model.confirmGalleryScan() }
            compose.waitUntil(5_000) { model.analysisState.value.phase == ScannerPhase.Results }
            compose.runOnIdle {
                assertEquals(1, ocrCalls)
                assertEquals(1, searches)
                assertEquals("포키", model.analysisState.value.search.query)
                assertTrue(model.analysisState.value.search.products.isEmpty())
                assertNull(model.analysisState.value.search.errorMessage)
                model.cancelScan()
                // Compare mode remains unchanged and must never send OCR requests.
                model.selectGalleryPhoto(Uri.fromFile(original), normalScan = false)
            }
            compose.waitUntil(5_000) { !model.state.value.busy && model.state.value.photo != null }
            compose.runOnIdle {
                assertFalse(model.state.value.galleryConfirmation)
                assertEquals(1, ocrCalls)
            }
        } finally {
            compose.runOnIdle { model.finishSession() }
            original.delete()
        }
    }
}
