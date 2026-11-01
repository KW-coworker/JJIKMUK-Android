package com.coworker.jjikmuk.feature.auth.photo

import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.data.HttpAuthRepository
import com.coworker.jjikmuk.feature.auth.session.*
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PendingSignupPhotoRouteTest {
    @get:Rule val compose = createComposeRule()
    private val photo = ProfilePhoto(byteArrayOf(1, 2, 3), "image/jpeg")
    private var pending: PendingSignupPhoto? = PendingSignupPhoto("draft", 1, false, photo)
    private var uploads = 0
    private var fail = false
    private var serverPhoto: ProfilePhoto? = null
    private val done = mutableStateOf(false)
    private val sessions = AuthSessionManager(object : SessionStore {
        override suspend fun read(): SessionCredentials? = null
        override suspend fun write(credentials: SessionCredentials) = Unit
        override suspend fun clear() = Unit
    })
    private val store = object : PendingSignupPhotoStore {
        override suspend fun read(email: String) = pending
        override suspend fun stage(email: String, photo: ProfilePhoto): String = error("unused")
        override suspend fun update(email: String, record: PendingSignupPhoto) { pending = record }
        override suspend fun remove(email: String, id: String) { pending = null }
    }
    private val repo = object : AuthRepository by HttpAuthRepository.unconfigured() {
        override suspend fun photo(accessToken: String): AuthResult<ProfilePhoto> = serverPhoto?.let { AuthResult.Success(it) }
            ?: AuthResult.Failure(AuthFailure(AuthFailureKind.Server, 404, "PHOTO_NOT_FOUND"))
        override suspend fun uploadPhoto(accessToken: String, photo: ProfilePhoto): AuthResult<PhotoUploadReceipt> {
            uploads++; serverPhoto = photo
            return if (fail) AuthResult.Failure(AuthFailure(AuthFailureKind.Network))
                else AuthResult.Success(PhotoUploadReceipt("/api/users/me/photo", true))
        }
    }
    private val vm = PendingSignupPhotoViewModel(store, AuthPhotos(repo, sessions, AuthenticatedRequests(sessions, repo)), sessions)
    @Before fun show() {
        runBlocking {
            sessions.acceptLogin(sessions.beginLogin(), AuthLogin("test", 1, "test@example.com", "unused", 900, true), System.currentTimeMillis())
            sessions.acceptProfile(sessions.state.value.generation, AuthProfile(1, "test@example.com", "사용자", null, null, null,
                null, "USER", AuthProvider.LOCAL, true, true, null))
        }
        compose.setContent { JjikmukTheme {
            if (done.value) Text("홈으로 계속") else PendingSignupPhotoRoute(vm) { done.value = true }
        } }
    }
    @Test fun saveIsExplicitAndConfirmedServerPhotoRemovesDraft() {
        compose.onNodeWithText("선택한 사진 저장").performScrollTo().assertIsEnabled()
        compose.runOnIdle { assertEquals(0, uploads) }
        compose.onNodeWithText("선택한 사진 저장").performClick()
        compose.onNodeWithText("홈으로 계속").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, uploads); assertNull(pending) }
    }
    @Test fun laterKeepsDraftWithoutUpload() {
        compose.onNodeWithText("나중에 설정하기").performScrollTo().performClick()
        compose.onNodeWithText("홈으로 계속").assertIsDisplayed()
        compose.runOnIdle { assertEquals(0, uploads); assertNotNull(pending) }
    }
    @Test fun lostResponseReadbackOffersServerPhotoInsteadOfResending() {
        compose.runOnIdle { fail = true }
        compose.onNodeWithText("선택한 사진 저장").performScrollTo().performClick()
        compose.onNodeWithText("저장된 사진 다시 확인").performScrollTo().performClick()
        compose.onNodeWithText("현재 계정에 저장된 사진").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("임시 사진 삭제하고 계속").performScrollTo().performClick()
        compose.onNodeWithText("홈으로 계속").assertIsDisplayed()
        compose.runOnIdle { assertEquals(1, uploads); assertNull(pending) }
    }
}
