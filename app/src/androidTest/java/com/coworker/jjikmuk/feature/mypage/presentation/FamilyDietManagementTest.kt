package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.repository.FamilyProfileRepository
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FamilyDietManagementTest {
    @get:Rule val compose = createComposeRule()
    private class Profiles : FamilyProfileRepository {
        val values = MutableStateFlow(listOf(
            FamilyProfile("me", "나", "🙂", "본인", true, "해당 없음", emptySet(), emptySet()),
            FamilyProfile("family", "가족A", "👨🏻", "배우자", false, "해당 없음", emptySet(), emptySet()),
        ))
        var lastDietId: String? = null
        override fun observeProfiles() = values
        override suspend fun ensureDefaultProfiles() = Unit
        override suspend fun addProfile(name: String, emoji: String, relation: String) {
            values.value = values.value + FamilyProfile("new", name, emoji, relation, false, "해당 없음", emptySet(), emptySet())
        }
        override suspend fun updateProfile(profileId: String, name: String, emoji: String, relation: String) {
            values.value = values.value.map { if (it.id == profileId) it.copy(name = name, emoji = emoji, relation = relation) else it }
        }
        override suspend fun updateDietCondition(profileId: String, vegetarian: String, allergies: Set<String>, preferences: Set<String>) {
            lastDietId = profileId
            values.value = values.value.map { if (it.id == profileId) it.copy(vegetarian = vegetarian, allergies = allergies, preferences = preferences) else it }
        }
        override suspend fun deleteProfile(profileId: String) { values.value = values.value.filterNot { it.id == profileId } }
    }

    @Test fun familySelectionEditsFamilyAndSelfUsesAccountEditor() {
        val profiles = Profiles()
        val vm = DietConditionManagementViewModel(profiles)
        compose.setContent { JjikmukTheme {
            DietConditionManagementScreen({}, viewModel = vm, selfEditor = { selector ->
                androidx.compose.foundation.layout.Column {
                    selector()
                    androidx.compose.material3.Text("본인 식이조건 편집")
                }
            })
        } }
        compose.onNodeWithText("본인 식이조건 편집").assertExists()
        compose.onNodeWithText("내 식이조건 수정").assertDoesNotExist()
        compose.onNodeWithText("가족A").performClick()
        compose.onNodeWithText("👨🏻 가족A 의 식이 조건").assertExists()
        compose.onNodeWithText("우유").performScrollTo().performClick()
        compose.onNodeWithText("수정하기").performClick()
        compose.waitUntil { profiles.lastDietId == "family" }
        assertEquals(setOf("우유"), profiles.values.value.single { it.id == "family" }.allergies)
        assertTrue(profiles.values.value.single { it.isMe }.allergies.isEmpty())
        compose.onNodeWithText("나").performScrollTo().performClick()
        compose.onNodeWithText("본인 식이조건 편집").assertExists()
    }

    @Test fun addEditAndDeleteFamilyAreReachable() {
        val profiles = Profiles()
        val vm = DietConditionManagementViewModel(profiles)
        compose.setContent { JjikmukTheme {
            DietConditionManagementScreen({}, viewModel = vm, selfEditor = { selector ->
                androidx.compose.foundation.layout.Column { selector(); androidx.compose.material3.Text("본인 식이조건 편집") }
            })
        } }
        compose.onNodeWithText("추가").performClick()
        compose.onNode(hasSetTextAction()).performTextInput("새가족")
        compose.onNodeWithText("추가하기").performClick()
        compose.waitUntil { profiles.values.value.any { it.name == "새가족" } }
        compose.onNodeWithText("새가족").performClick()
        compose.onNodeWithText("프로필 수정").performClick()
        compose.onNode(hasSetTextAction()).performTextReplacement("수정가족")
        compose.onAllNodesWithText("수정하기").filter(isEnabled()).onLast().performClick()
        compose.waitUntil { profiles.values.value.any { it.name == "수정가족" } }
        compose.onNodeWithText("프로필 수정").performClick()
        compose.onNodeWithText("삭제").performClick()
        compose.waitUntil { profiles.values.value.none { it.id == "new" } }
        assertTrue(profiles.values.value.any { it.isMe })
    }
}
