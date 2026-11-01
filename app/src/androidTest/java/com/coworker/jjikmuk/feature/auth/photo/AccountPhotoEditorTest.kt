package com.coworker.jjikmuk.feature.auth.photo

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import java.io.ByteArrayOutputStream

@RunWith(AndroidJUnit4::class)
class AccountPhotoEditorTest {
    @get:Rule val compose = createComposeRule()
    @Test fun deletionRequiresConfirmationAndReturnsToDefaultPhoto() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val image = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)
        val bytes = ByteArrayOutputStream().also { image.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray()
        image.recycle()
        var deletes = 0
        val repository = object : AuthRepository by HttpAuthRepository.unconfigured() {
            override suspend fun photo(accessToken: String) = AuthResult.Success(ProfilePhoto(bytes, "image/png"))
            override suspend fun deletePhoto(accessToken: String): AuthResult<Unit> { deletes++; return AuthResult.Success(Unit) }
        }
        val sessions = AuthSessionManager(object : SessionStore {
            override suspend fun read() = SessionCredentials("test", 1, "1@example.com", System.currentTimeMillis()+900_000, true)
            override suspend fun write(credentials: SessionCredentials) = Unit
            override suspend fun clear() = Unit
        })
        runBlocking {
            sessions.restore()
            sessions.acceptProfile(sessions.state.value.generation, AuthProfile(1,"1@example.com","사용자",null,null,null,null,
                "USER",AuthProvider.LOCAL,true,true,"/api/users/me/photo"))
        }
        val photos = AuthPhotos(repository, sessions, AuthenticatedRequests(sessions, repository))
        lateinit var vm: ProfilePhotoViewModel
        compose.runOnIdle { vm = ProfilePhotoViewModel(photos, ProfilePhotoEncoder(context), sessions) }
        compose.setContent { JjikmukTheme { Column { AccountPhotoEditor(vm) } } }
        compose.waitUntil(5_000) { vm.state.value.photo != null && vm.state.value.canEdit }
        compose.onAllNodes(hasClickAction())[0].performClick()
        compose.onNodeWithText("기본 사진으로 변경").performClick()
        compose.onNodeWithText("프로필 사진 삭제").assertIsDisplayed()
        assertEquals(0, deletes)
        compose.onNodeWithText("취소").performClick()
        assertEquals(0, deletes)
        compose.onAllNodes(hasClickAction())[0].performClick()
        compose.onNodeWithText("기본 사진으로 변경").performClick()
        compose.onNodeWithText("삭제", useUnmergedTree = true).performClick()
        compose.waitUntil(5_000) { photos.state.value.photo == null && !photos.state.value.busy }
        assertEquals(1, deletes)
        assertNotNull(sessions.state.value.credentials)
        runBlocking { sessions.clear() }
    }
}
