package com.coworker.jjikmuk.feature.auth.navigation

import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpCondition
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpUiState
import org.junit.Assert.assertEquals
import org.junit.Test

class AuthNavigationPolicyTest {
    @Test
    fun `조건 미선택이면 Profile로 이동한다`() {
        assertEquals(AuthRoute.SignUpProfile, nextRouteAfterConditions(SignUpUiState()))
    }

    @Test
    fun `알레르기와 채식을 모두 선택하면 알레르기를 우선한다`() {
        val state = SignUpUiState(
            selectedConditions = setOf(SignUpCondition.Allergy, SignUpCondition.Vegetarian),
        )

        assertEquals(AuthRoute.SignUpAllergies, nextRouteAfterConditions(state))
    }

    @Test
    fun `채식만 선택하면 채식 화면으로 이동한다`() {
        val state = SignUpUiState(selectedConditions = setOf(SignUpCondition.Vegetarian))

        assertEquals(AuthRoute.SignUpVegetarian, nextRouteAfterConditions(state))
    }
}
