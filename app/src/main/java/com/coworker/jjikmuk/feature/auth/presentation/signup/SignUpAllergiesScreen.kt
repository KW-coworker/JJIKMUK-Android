package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAllergyChip
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukVerticalScrollIndicator
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun SignUpAllergiesRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignUpAllergiesScreen(
        uiState = uiState,
        onAllergyClick = viewModel::toggleAllergy,
        onBackClick = onBackClick,
        onNextClick = onNextClick,
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignUpAllergiesScreen(
    uiState: SignUpUiState,
    onAllergyClick: (String) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val scrollState = rememberScrollState()
    val selectedCount = uiState.allergies.size
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

        Box(modifier = Modifier.weight(1f)) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(horizontal = 22.dp)
                    .offset(y = 6.dp),
            ) {
                Text(
                    text = stringResource(R.string.sign_up_allergies_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Text(
                    text = stringResource(R.string.sign_up_allergies_description),
                    color = colors.textSecondary,
                    style = JjikmukTheme.typography.bodyL,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 140.dp)
                    .fillMaxWidth()
                    .height(403.dp)
                    .clipToBounds(),
            ) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scrollState)
                        .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
                ) {
                    allergyItems.forEach { item ->
                        JjikmukAllergyChip(
                            emoji = item.emoji,
                            label = stringResource(item.labelRes),
                            selected = item.id in uiState.allergies,
                            onSelectedChange = { onAllergyClick(item.id) },
                        )
                    }
                }

                JjikmukVerticalScrollIndicator(
                    scrollValue = scrollState.value,
                    scrollMaxValue = scrollState.maxValue,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp),
                )
            }

            JjikmukPrimaryButton(
                text = stringResource(R.string.sign_up_allergies_selected_count, selectedCount),
                onClick = onNextClick,
                enabled = selectedCount > 0,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 22.dp)
                    .offset(y = 569.dp)
                    .zIndex(1f),
            )
        }
    }
}

data class AllergyItem(
    val id: String,
    val emoji: String,
    @StringRes val labelRes: Int,
)

val allergyItems = listOf(
    AllergyItem("egg", "🥚", R.string.sign_up_allergy_egg),
    AllergyItem("milk", "🥛", R.string.sign_up_allergy_milk),
    AllergyItem("soy", "🫘", R.string.sign_up_allergy_soy),
    AllergyItem("wheat", "🍞", R.string.sign_up_allergy_wheat),
    AllergyItem("pork", "🥓", R.string.sign_up_allergy_pork),
    AllergyItem("chicken", "🍗", R.string.sign_up_allergy_chicken),
    AllergyItem("shrimp", "🦐", R.string.sign_up_allergy_shrimp),
    AllergyItem("crab", "🦀", R.string.sign_up_allergy_crab),
    AllergyItem("squid", "🦑", R.string.sign_up_allergy_squid),
    AllergyItem("mackerel", "🐟", R.string.sign_up_allergy_mackerel),
    AllergyItem("shellfish", "🐚", R.string.sign_up_allergy_shellfish),
    AllergyItem("oyster", "🦪", R.string.sign_up_allergy_oyster),
    AllergyItem("mussel", "🦪", R.string.sign_up_allergy_mussel),
    AllergyItem("abalone", "🐚", R.string.sign_up_allergy_abalone),
    AllergyItem("peach", "🍑", R.string.sign_up_allergy_peach),
    AllergyItem("tomato", "🍅", R.string.sign_up_allergy_tomato),
    AllergyItem("peanut", "🥜", R.string.sign_up_allergy_peanut),
    AllergyItem("walnut", "🌰", R.string.sign_up_allergy_walnut),
    AllergyItem("buckwheat", "🍜", R.string.sign_up_allergy_buckwheat),
    AllergyItem("pine_nut", "🫘", R.string.sign_up_allergy_pine_nut),
    AllergyItem("sulfites", "🧪", R.string.sign_up_allergy_sulfites),
    AllergyItem("sesame", "🧂", R.string.sign_up_allergy_sesame),
    AllergyItem("almond", "🫘", R.string.sign_up_allergy_almond),
    AllergyItem("mustard", "🍯", R.string.sign_up_allergy_mustard),
    AllergyItem("celery", "🥒", R.string.sign_up_allergy_celery),
    AllergyItem("beef", "🥩", R.string.sign_up_allergy_beef),
)

private val AUTH_STATUS_BAR_COLOR = Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpAllergiesEmptyPreview() {
    JjikmukTheme {
        SignUpAllergiesScreen(
            uiState = SignUpUiState(),
            onAllergyClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpAllergiesSelectedPreview() {
    JjikmukTheme {
        SignUpAllergiesScreen(
            uiState = SignUpUiState(allergies = setOf("crab", "squid")),
            onAllergyClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}
