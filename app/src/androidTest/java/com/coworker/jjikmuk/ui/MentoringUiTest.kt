package com.coworker.jjikmuk.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.graphics.asAndroidBitmap
import android.graphics.Bitmap
import com.coworker.jjikmuk.feature.auth.presentation.permission.PermissionApprovalScreen
import com.coworker.jjikmuk.ui.component.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import java.io.File

class MentoringUiTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private fun shot(name: String) {
        rule.waitForIdle()
        File(rule.activity.filesDir, "$name.png").outputStream().use {
            rule.onRoot().captureToImage().asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }
    @Test fun permissionCardsHaveNoSelectionAndAllowRequestsAll() {
        var selected = false
        rule.setContent { JjikmukTheme { PermissionApprovalScreen(onAllowClick = {
            selected = it.camera && it.photo && it.notification
        }, onLaterClick = {}) } }
        rule.onNodeWithText("카메라").assertIsDisplayed()
        rule.onNodeWithText("허용하고 시작하기").performClick()
        rule.runOnIdle { assertTrue(selected) }
        shot("permission-updated")
    }
    @Test fun bottomBarContainsFourMenus() {
        rule.setContent { JjikmukTheme { JjikmukBottomNavigationBar(MainTab.Home, {}) } }
        for (label in listOf("Home", "Product", "Chat", "MY")) rule.onNodeWithText(label).assertIsDisplayed()
        rule.onNodeWithText("Diet").assertDoesNotExist()
        shot("navigation-updated")
    }
    @Test fun scannerDemoHasNoModeSelector() {
        rule.setContent { JjikmukTheme {
            com.coworker.jjikmuk.feature.scanner.presentation.ScannerScreen(
                mode = com.coworker.jjikmuk.feature.scanner.presentation.ScannerMode.Normal,
                onModeChange = {}, onBackClick = {}, onFlashClick = {}, onGalleryClick = {},
                onShutterClick = {}, onCompareListClick = {})
        } }
        rule.onNodeWithText("일반 스캔").assertDoesNotExist()
        rule.onNodeWithText("비교 스캔").assertDoesNotExist()
        shot("scanner-demo-updated")
    }
    @Test fun onboardingCompletionSurvivesNewStoreInstance() {
        val context = object : android.content.ContextWrapper(rule.activity) {
            override fun getSharedPreferences(name: String, mode: Int) =
                super.getSharedPreferences("ui-test-" + name, mode)
        }
        val prefs = context.getSharedPreferences("permission-onboarding", 0)
        try {
            com.coworker.jjikmuk.feature.auth.presentation.permission.PermissionOnboarding.complete(context)
            assertFalse(com.coworker.jjikmuk.feature.auth.presentation.permission.PermissionOnboarding.shouldShow(context))
            assertTrue(prefs.getBoolean("completed", false))
        } finally { prefs.edit().clear().commit() }
    }
}
