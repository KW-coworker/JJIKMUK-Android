package com.coworker.jjikmuk.feature.auth.presentation.placeholder

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class PlaceholderAction(
    val label: String,
    val onClick: () -> Unit,
)

@Composable
fun AuthPlaceholderScreen(
    title: String,
    primaryActions: List<PlaceholderAction>,
    modifier: Modifier = Modifier,
    description: String = "Figma UI 적용 예정",
    onBackClick: (() -> Unit)? = null,
    blockSystemBack: Boolean = false,
) {
    when {
        blockSystemBack -> BackHandler(enabled = true) {}
        onBackClick != null -> BackHandler(onBack = onBackClick)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = title)
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = description)
        Spacer(modifier = Modifier.height(32.dp))

        primaryActions.forEach { action ->
            Button(
                onClick = action.onClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = action.label)
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        if (onBackClick != null) {
            OutlinedButton(
                onClick = onBackClick,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "뒤로가기")
            }
        }
    }
}

@Composable
fun ConditionsPlaceholderScreen(
    hasVegetarianCondition: Boolean,
    hasAllergyCondition: Boolean,
    onConditionsChange: (hasVegetarian: Boolean, hasAllergy: Boolean) -> Unit,
    onNextClick: () -> Unit,
    onBackClick: () -> Unit,
) {
    AuthPlaceholderScreen(
        title = "회원가입 - 맞춤 조건 선택",
        description = "현재 선택: 채식=$hasVegetarianCondition, 알레르기=$hasAllergyCondition",
        primaryActions = listOf(
            PlaceholderAction("채식 조건 전환") {
                onConditionsChange(!hasVegetarianCondition, hasAllergyCondition)
            },
            PlaceholderAction("알레르기 조건 전환") {
                onConditionsChange(hasVegetarianCondition, !hasAllergyCondition)
            },
            PlaceholderAction("선택 완료", onNextClick),
        ),
        onBackClick = onBackClick,
    )
}
