package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukConditionCard
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun SignUpConditionsRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SignUpConditionsScreen(
        uiState = uiState,
        onConditionClick = viewModel::toggleCondition,
        onBackClick = onBackClick,
        onNextClick = onNextClick,
        modifier = modifier,
    )
}

@Composable
fun SignUpConditionsScreen(
    uiState: SignUpUiState,
    onConditionClick: (SignUpCondition) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val conditions = listOf(
        ConditionItem(
            type = SignUpCondition.Allergy,
            title = stringResource(R.string.sign_up_condition_allergy),
            description = stringResource(R.string.sign_up_condition_allergy_description),
        ),
        ConditionItem(
            type = SignUpCondition.Vegetarian,
            title = stringResource(R.string.sign_up_condition_vegetarian),
            description = stringResource(R.string.sign_up_condition_vegetarian_description),
        ),
        ConditionItem(
            type = SignUpCondition.LowSugar,
            title = stringResource(R.string.sign_up_condition_low_sugar),
            description = stringResource(R.string.sign_up_condition_low_sugar_description),
        ),
        ConditionItem(
            type = SignUpCondition.LowSodium,
            title = stringResource(R.string.sign_up_condition_low_sodium),
            description = stringResource(R.string.sign_up_condition_low_sodium_description),
        ),
        ConditionItem(
            type = SignUpCondition.GlutenFree,
            title = stringResource(R.string.sign_up_condition_gluten_free),
            description = stringResource(R.string.sign_up_condition_gluten_free_description),
        ),
        ConditionItem(
            type = SignUpCondition.LowCalorie,
            title = stringResource(R.string.sign_up_condition_low_calorie),
            description = stringResource(R.string.sign_up_condition_low_calorie_description),
        ),
        ConditionItem(
            type = SignUpCondition.LowFat,
            title = stringResource(R.string.sign_up_condition_low_fat),
            description = stringResource(R.string.sign_up_condition_low_fat_description),
        ),
        ConditionItem(
            type = SignUpCondition.HighProtein,
            title = stringResource(R.string.sign_up_condition_high_protein),
            description = stringResource(R.string.sign_up_condition_high_protein_description),
        ),
    )

    BackHandler(onBack = onBackClick)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.surface),
    ) {
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(AUTH_STATUS_BAR_COLOR),
        )
        JjikmukAuthTopBar(onBackClick = onBackClick)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 22.dp)
                    .offset(y = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.sign_up_conditions_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Text(
                    text = stringResource(R.string.sign_up_conditions_description),
                    color = colors.textSecondary,
                    style = JjikmukTheme.typography.bodyL,
                )
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 22.dp)
                    .offset(y = 140.dp),
            ) {
                conditions.chunked(2).forEach { rowConditions ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(17.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowConditions.forEach { condition ->
                            JjikmukConditionCard(
                                title = condition.title,
                                description = condition.description,
                                selected = condition.type in uiState.selectedConditions,
                                onSelectedChange = { onConditionClick(condition.type) },
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            JjikmukPrimaryButton(
                text = stringResource(R.string.sign_up_email_next),
                onClick = onNextClick,
                enabled = true,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 22.dp, end = 18.dp)
                    .offset(y = 569.dp),
            )
        }
    }
}

private data class ConditionItem(
    val type: SignUpCondition,
    val title: String,
    val description: String,
)

private val AUTH_STATUS_BAR_COLOR = Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpConditionsEmptyPreview() {
    JjikmukTheme {
        SignUpConditionsScreen(
            uiState = SignUpUiState(),
            onConditionClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpConditionsSelectedPreview() {
    JjikmukTheme {
        SignUpConditionsScreen(
            uiState = SignUpUiState(
                selectedConditions = setOf(SignUpCondition.Vegetarian),
            ),
            onConditionClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}
