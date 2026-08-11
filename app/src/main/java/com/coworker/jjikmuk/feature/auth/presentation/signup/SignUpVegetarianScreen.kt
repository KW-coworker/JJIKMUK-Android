package com.coworker.jjikmuk.feature.auth.presentation.signup

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukVerticalScrollIndicator
import com.coworker.jjikmuk.ui.component.JjikmukVegetarianDietCard
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun SignUpVegetarianRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SignUpVegetarianScreen(
        uiState = uiState,
        onDietClick = viewModel::selectVegetarianDiet,
        onBackClick = onBackClick,
        onNextClick = onNextClick,
        modifier = modifier,
    )
}

@Composable
fun SignUpVegetarianScreen(
    uiState: SignUpUiState,
    onDietClick: (VegetarianDiet) -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val diets = vegetarianDietItems()
    val dietScrollState = rememberScrollState()
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
                    text = stringResource(R.string.sign_up_vegetarian_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Text(
                    text = stringResource(R.string.sign_up_vegetarian_description),
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
                    .height(429.dp)
                    .clipToBounds(),
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(dietScrollState)
                        .padding(start = 22.dp, end = 22.dp, bottom = 16.dp),
                ) {
                    diets.forEach { item ->
                        JjikmukVegetarianDietCard(
                            title = stringResource(item.titleRes),
                            description = stringResource(item.descriptionRes),
                            selected = uiState.vegetarianDiet == item.type,
                            onClick = { onDietClick(item.type) },
                        )
                    }
                }

                JjikmukVerticalScrollIndicator(
                    scrollValue = dietScrollState.value,
                    scrollMaxValue = dietScrollState.maxValue,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 6.dp),
                )
            }

            JjikmukPrimaryButton(
                text = stringResource(R.string.sign_up_email_next),
                onClick = onNextClick,
                enabled = uiState.vegetarianDiet != null,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(start = 22.dp, end = 18.dp)
                    .offset(y = 569.dp)
                    .zIndex(1f),
            )
        }
    }
}

private data class VegetarianDietItem(
    val type: VegetarianDiet,
    @StringRes val titleRes: Int,
    @StringRes val descriptionRes: Int,
)

private fun vegetarianDietItems() = listOf(
    VegetarianDietItem(VegetarianDiet.Vegan, R.string.sign_up_vegetarian_vegan, R.string.sign_up_vegetarian_vegan_description),
    VegetarianDietItem(VegetarianDiet.Lacto, R.string.sign_up_vegetarian_lacto, R.string.sign_up_vegetarian_lacto_description),
    VegetarianDietItem(VegetarianDiet.Ovo, R.string.sign_up_vegetarian_ovo, R.string.sign_up_vegetarian_ovo_description),
    VegetarianDietItem(VegetarianDiet.LactoOvo, R.string.sign_up_vegetarian_lacto_ovo, R.string.sign_up_vegetarian_lacto_ovo_description),
    VegetarianDietItem(VegetarianDiet.Pesco, R.string.sign_up_vegetarian_pesco, R.string.sign_up_vegetarian_pesco_description),
    VegetarianDietItem(VegetarianDiet.Pollo, R.string.sign_up_vegetarian_pollo, R.string.sign_up_vegetarian_pollo_description),
)

private val AUTH_STATUS_BAR_COLOR = Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 1014)
@Composable
private fun SignUpVegetarianEmptyPreview() {
    JjikmukTheme {
        SignUpVegetarianScreen(
            uiState = SignUpUiState(),
            onDietClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 1014)
@Composable
private fun SignUpVegetarianPreview() {
    JjikmukTheme {
        SignUpVegetarianScreen(
            uiState = SignUpUiState(vegetarianDiet = VegetarianDiet.Vegan),
            onDietClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}
