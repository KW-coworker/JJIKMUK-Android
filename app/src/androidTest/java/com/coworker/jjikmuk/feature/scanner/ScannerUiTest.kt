package com.coworker.jjikmuk.feature.scanner

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.core.view.WindowCompat
import androidx.test.espresso.Espresso.pressBack
import androidx.test.platform.app.InstrumentationRegistry
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerMode
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerRecognitionFailureBottomSheet
import com.coworker.jjikmuk.feature.scanner.presentation.ScannerScreen
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@Suppress("DEPRECATION")
class ScannerUiTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun focusMaskLeavesTargetBrightInBothScanModes() {
        val mode = mutableStateOf(ScannerMode.Normal)
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            JjikmukTheme {
                ScannerScreen(
                    mode = mode.value, onModeChange = { mode.value = it },
                    onBackClick = {}, onFlashClick = {}, onGalleryClick = {},
                    onShutterClick = {}, onCompareListClick = {},
                    cameraContent = { Box(Modifier.fillMaxSize().background(Color.White)) },
                )
            }
        }
        compose.onNodeWithText("제품 포장 앞면을 네모칸 안에 맞춰주세요").assertIsDisplayed()
        fun assertCutout() {
            val pixels = compose.onRoot().captureToImage().toPixelMap()
            val y = (pixels.height * 0.4f).toInt()
            assertTrue(pixels[pixels.width / 2, y].red > 0.98f)
            assertTrue(pixels[10, y].red in 0.70f..0.80f)
        }
        assertCutout()
        compose.onNodeWithText("비교 스캔").performClick()
        compose.onNodeWithText("비교 스캔 목록").assertIsDisplayed()
        assertCutout()
    }

    @Test fun failureSheetSupportsRetakeManualSearchAndDismissWithoutCapturing() {
        val showFailure = mutableStateOf(false)
        var retakes = 0
        var searches = 0
        var dismissals = 0
        compose.activityRule.scenario.onActivity { WindowCompat.setDecorFitsSystemWindows(it.window, false) }
        compose.setContent {
            JjikmukTheme {
                ScannerScreen(
                    mode = ScannerMode.Normal, onModeChange = {}, onBackClick = {},
                    onFlashClick = {}, onGalleryClick = {}, onShutterClick = {}, onCompareListClick = {},
                )
                if (showFailure.value) ScannerRecognitionFailureBottomSheet(
                    onDismissRequest = { dismissals++; showFailure.value = false },
                    onRetakeClick = { retakes++; showFailure.value = false },
                    onManualSearchClick = { searches++; showFailure.value = false },
                )
            }
        }
        compose.waitForIdle()
        screenshot("scanner-main", compose.onRoot().captureToImage().asAndroidBitmap())
        compose.runOnIdle { showFailure.value = true }
        compose.onNodeWithText("제품명 인식 불가").assertIsDisplayed()
        compose.onNodeWithText("빛 반사나 흔들림 없이 밝은 곳에서 찍어주세요").assertIsDisplayed()
        compose.waitForIdle()
        screenshot("scanner-recognition-failure")
        compose.onNodeWithText("다시 촬영하기").performClick()
        compose.runOnIdle {
            assertEquals(1, retakes); assertEquals(0, searches)
            assertTrue(!showFailure.value)
            showFailure.value = true
        }
        compose.onNodeWithText("제품명 직접 검색하기").performClick()
        compose.runOnIdle {
            assertEquals(1, searches); assertEquals(1, retakes)
            showFailure.value = true
        }
        compose.onNodeWithText("제품명 인식 불가").assertIsDisplayed()
        pressBack()
        compose.runOnIdle { assertEquals(1, dismissals); assertTrue(!showFailure.value) }
    }

    private fun screenshot(name: String, captured: Bitmap? = null) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val bitmap = captured ?: instrumentation.uiAutomation.takeScreenshot()
        File(instrumentation.targetContext.filesDir, "$name.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
        bitmap.recycle()
    }
}
