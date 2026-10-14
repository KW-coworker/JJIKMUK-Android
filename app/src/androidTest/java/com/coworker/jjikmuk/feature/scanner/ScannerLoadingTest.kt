package com.coworker.jjikmuk.feature.scanner

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowCompat
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.feature.scanner.data.ScannerImagePreparer
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerCaptureViewModel
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerLoadingScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
class ScannerLoadingTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun loadingAdvancesOncePerIntervalHoldsAtLastStepAndRestartsAfterCancel() {
        val visible = mutableStateOf(true)
        var cancels = 0
        compose.mainClock.autoAdvance = false
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            JjikmukTheme {
                if (visible.value) ScannerLoadingScreen(
                    photo = R.drawable.scanner_camera_preview,
                    onCancel = { cancels++; visible.value = false },
                ) else Text("촬영 화면")
            }
        }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("제품명을 인식하고 있어요").assertIsDisplayed()
        saveFrame("scanner-loading-recognizing")
        compose.mainClock.advanceTimeBy(500)
        compose.onNodeWithText("상품을 찾고 있어요").assertDoesNotExist()
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("상품을 찾고 있어요").assertIsDisplayed()
        saveFrame("scanner-loading-searching")
        compose.mainClock.advanceTimeBy(1_200)
        compose.onNodeWithText("목록을 준비하고 있어요").assertIsDisplayed()
        saveFrame("scanner-loading-preparing")
        compose.mainClock.advanceTimeBy(10_000)
        compose.onNodeWithText("목록을 준비하고 있어요").assertIsDisplayed()
        compose.onNodeWithText("취소").performClick()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("촬영 화면").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, cancels); visible.value = true }
        compose.mainClock.advanceTimeBy(600)
        compose.onNodeWithText("제품명을 인식하고 있어요").assertIsDisplayed()
        pressBack()
        compose.mainClock.advanceTimeByFrame()
        compose.onNodeWithText("촬영 화면").assertIsDisplayed()
        compose.runOnIdle { assertEquals(2, cancels) }
    }

    @Test fun cancellingDeletesCapturedFileAndLateCallbacksCannotReplaceNextPhoto() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        lateinit var viewModel: ScannerCaptureViewModel
        compose.runOnIdle { viewModel = ScannerCaptureViewModel(context, ScannerImagePreparer()) }
        try {
            fun capture(): File {
                lateinit var source: File
                compose.runOnIdle { source = checkNotNull(viewModel.beginCapture()) }
                val bitmap = Bitmap.createBitmap(80, 120, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.GREEN)
                source.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, 95, it) }
                bitmap.recycle()
                compose.runOnIdle { viewModel.captureSaved(source) }
                compose.waitUntil(5_000) { !viewModel.state.value.busy }
                assertFalse(source.exists())
                return checkNotNull(viewModel.state.value.photo).file
            }
            val first = capture()
            assertTrue(first.exists())
            compose.runOnIdle { viewModel.cancelScan() }
            assertFalse(first.exists())
            assertNull(viewModel.state.value.photo)
            val second = capture()
            compose.runOnIdle {
                viewModel.captureSaved(first)
                viewModel.captureFailed(first)
            }
            assertEquals(second, viewModel.state.value.photo?.file)
            assertTrue(second.exists())
            assertNull(viewModel.state.value.error)
            compose.runOnIdle { viewModel.cancelScan() }
            assertFalse(second.exists())
        } finally {
            compose.runOnIdle { viewModel.finishSession() }
        }
    }

    private fun saveFrame(name: String) {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
