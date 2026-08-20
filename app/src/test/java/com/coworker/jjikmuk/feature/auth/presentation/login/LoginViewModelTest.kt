package com.coworker.jjikmuk.feature.auth.presentation.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LoginViewModelTest {
    private val viewModel = LoginViewModel()

    @Test
    fun `mock 계정으로 로그인하면 성공한다`() {
        viewModel.updateEmail("email@example.com")
        viewModel.updatePassword("password123")

        assertTrue(viewModel.login())
        assertNull(viewModel.uiState.value.loginError)
    }

    @Test
    fun `잘못된 계정으로 로그인하면 통합 오류를 표시한다`() {
        viewModel.updateEmail("wrong@example.com")
        viewModel.updatePassword("wrong-password")

        assertFalse(viewModel.login())
        assertEquals("이메일 또는 비밀번호를 다시 확인해 주세요", viewModel.uiState.value.loginError)
    }

    @Test
    fun `입력값을 수정하면 로그인 오류가 사라진다`() {
        viewModel.login()

        viewModel.updateEmail("email@example.com")

        assertNull(viewModel.uiState.value.loginError)
    }
}
