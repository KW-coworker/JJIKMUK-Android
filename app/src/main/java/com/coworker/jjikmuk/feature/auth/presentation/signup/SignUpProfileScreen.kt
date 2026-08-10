package com.coworker.jjikmuk.feature.auth.presentation.signup

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAuthTopBar
import com.coworker.jjikmuk.ui.component.JjikmukNicknameTextField
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukProfileImagePicker
import com.coworker.jjikmuk.ui.component.JjikmukSelectedItemChip
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpProfileRoute(
    viewModel: SignUpViewModel,
    onBackClick: () -> Unit,
    onEditConditionsClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showPhotoOptions by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let { viewModel.updateProfilePhoto(uri = it.toString(), bitmap = null) }
    }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview(),
    ) { bitmap: Bitmap? ->
        bitmap?.let { viewModel.updateProfilePhoto(uri = null, bitmap = it) }
    }

    SignUpProfileScreen(
        uiState = uiState,
        onNicknameChange = viewModel::updateNickname,
        onPhotoClick = { showPhotoOptions = true },
        onEditConditionsClick = onEditConditionsClick,
        onBackClick = onBackClick,
        onNextClick = onNextClick,
        modifier = modifier,
    )

    if (showPhotoOptions) {
        ModalBottomSheet(onDismissRequest = { showPhotoOptions = false }) {
            Text(
                text = stringResource(R.string.sign_up_profile_photo_title),
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.h3,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 12.dp),
            )
            PhotoOption(
                text = stringResource(R.string.sign_up_profile_take_photo),
                onClick = {
                    showPhotoOptions = false
                    cameraLauncher.launch(null)
                },
            )
            PhotoOption(
                text = stringResource(R.string.sign_up_profile_choose_gallery),
                onClick = {
                    showPhotoOptions = false
                    galleryLauncher.launch("image/*")
                },
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PhotoOption(
    text: String,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = JjikmukTheme.colors.textPrimary,
        style = JjikmukTheme.typography.bodyL,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SignUpProfileScreen(
    uiState: SignUpUiState,
    onNicknameChange: (String) -> Unit,
    onPhotoClick: () -> Unit,
    onEditConditionsClick: () -> Unit,
    onBackClick: () -> Unit,
    onNextClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val selectedItems = profileSelectedItems(uiState)
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
                    text = stringResource(R.string.sign_up_profile_title),
                    color = colors.textPrimary,
                    style = JjikmukTheme.typography.h1,
                )
                Text(
                    text = stringResource(R.string.sign_up_profile_description),
                    color = colors.textSecondary,
                    style = JjikmukTheme.typography.bodyL,
                    modifier = Modifier.padding(top = 18.dp),
                )
            }

            JjikmukProfileImagePicker(
                imageModel = uiState.profilePhotoBitmap ?: uiState.profilePhotoUri,
                onClick = onPhotoClick,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 134.dp),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(27.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = 271.dp)
                    .padding(horizontal = 22.dp),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.sign_up_profile_nickname_label),
                        color = colors.textPrimary,
                        style = JjikmukTheme.typography.labelM,
                        modifier = Modifier.padding(start = 4.dp),
                    )
                    JjikmukNicknameTextField(
                        value = uiState.nickname,
                        onValueChange = onNicknameChange,
                        placeholder = stringResource(R.string.sign_up_nickname_placeholder),
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.sign_up_profile_selected_label),
                            color = colors.textPrimary,
                            style = JjikmukTheme.typography.labelM,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onEditConditionsClick,
                                )
                                .padding(4.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.ic_profile_edit),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = stringResource(R.string.sign_up_profile_edit),
                                color = colors.textSecondary,
                                style = JjikmukTheme.typography.labelS,
                            )
                        }
                    }

                    if (selectedItems.isEmpty()) {
                        Box(
                            contentAlignment = Alignment.CenterStart,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .background(colors.surfaceSecondary, RoundedCornerShape(8.dp))
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
                                .padding(horizontal = 18.dp),
                        ) {
                            Text(
                                text = stringResource(R.string.sign_up_profile_no_selected_items),
                                color = colors.textSecondary,
                                style = JjikmukTheme.typography.bodyM,
                            )
                        }
                    } else {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 73.dp, max = 140.dp)
                                .verticalScroll(rememberScrollState())
                                .background(colors.surfaceSecondary, RoundedCornerShape(16.dp))
                                .border(1.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                                .padding(17.dp),
                        ) {
                            selectedItems.forEach { item ->
                                JjikmukSelectedItemChip(
                                    emoji = item.emoji,
                                    label = item.label,
                                )
                            }
                        }
                    }
                }
            }

            JjikmukPrimaryButton(
                text = stringResource(R.string.sign_up_email_next),
                onClick = onNextClick,
                enabled = uiState.nickname.isNotBlank(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(horizontal = 22.dp)
                    .offset(y = 569.dp)
                    .zIndex(1f),
            )
        }
    }
}

private data class ProfileSelectedItem(
    val emoji: String,
    val label: String,
)

@Composable
private fun profileSelectedItems(uiState: SignUpUiState): List<ProfileSelectedItem> = buildList {
    allergyItems
        .filter { it.id in uiState.allergies }
        .forEach { add(ProfileSelectedItem(it.emoji, stringResource(it.labelRes))) }

    if (uiState.hasVegetarianCondition) {
        val dietLabel = when (uiState.vegetarianDiet) {
            VegetarianDiet.Vegan -> R.string.sign_up_profile_vegan
            VegetarianDiet.Lacto -> R.string.sign_up_profile_lacto
            VegetarianDiet.Ovo -> R.string.sign_up_profile_ovo
            VegetarianDiet.LactoOvo -> R.string.sign_up_profile_lacto_ovo
            VegetarianDiet.Pesco -> R.string.sign_up_profile_pesco
            VegetarianDiet.Pollo -> R.string.sign_up_profile_pollo
            null -> null
        }
        dietLabel?.let { add(ProfileSelectedItem("🥗", stringResource(it))) }
    }

    val conditions = listOf(
        SignUpCondition.LowSugar to ProfileSelectedItem("📉", stringResource(R.string.sign_up_condition_low_sugar)),
        SignUpCondition.LowSodium to ProfileSelectedItem("🧂", stringResource(R.string.sign_up_condition_low_sodium)),
        SignUpCondition.GlutenFree to ProfileSelectedItem("🌾", stringResource(R.string.sign_up_condition_gluten_free)),
        SignUpCondition.LowCalorie to ProfileSelectedItem("🏃", stringResource(R.string.sign_up_condition_low_calorie)),
        SignUpCondition.LowFat to ProfileSelectedItem("🥑", stringResource(R.string.sign_up_condition_low_fat)),
        SignUpCondition.HighProtein to ProfileSelectedItem("💪", stringResource(R.string.sign_up_condition_high_protein)),
    )
    conditions.filter { it.first in uiState.selectedConditions }.forEach { add(it.second) }
}

private val AUTH_STATUS_BAR_COLOR = Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpProfileSelectedPreview() {
    JjikmukTheme {
        SignUpProfileScreen(
            uiState = SignUpUiState(
                nickname = "코워커",
                selectedConditions = setOf(SignUpCondition.Allergy),
                allergies = setOf("beef", "milk"),
            ),
            onNicknameChange = {},
            onPhotoClick = {},
            onEditConditionsClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun SignUpProfileEmptyPreview() {
    JjikmukTheme {
        SignUpProfileScreen(
            uiState = SignUpUiState(nickname = "코워커"),
            onNicknameChange = {},
            onPhotoClick = {},
            onEditConditionsClick = {},
            onBackClick = {},
            onNextClick = {},
        )
    }
}
