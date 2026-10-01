package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.ui.component.JjikmukBackButton
import com.coworker.jjikmuk.ui.catalog.FoodAllergy
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun DietConditionManagementScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DietConditionManagementViewModel = hiltViewModel(),
) {
    val profiles by viewModel.profiles.collectAsStateWithLifecycle()
    var selectedMemberId by rememberSaveable { mutableStateOf("me") }
    var editingProfile by remember { mutableStateOf<FamilyProfile?>(null) }
    var editedVegetarian by rememberSaveable { mutableStateOf("") }
    var editedAllergies by remember { mutableStateOf(emptySet<String>()) }
    var editedPreferences by remember { mutableStateOf(emptySet<String>()) }
    val allergyOptions = FoodAllergy.entries.map { allergy ->
        stringResource(allergy.labelRes)
    }
    val selectedMember = profiles.firstOrNull { profile -> profile.id == selectedMemberId }
        ?: profiles.firstOrNull()
    val hasDietConditionChanges = selectedMember != null &&
        (editedVegetarian != selectedMember.vegetarian ||
            editedAllergies != selectedMember.allergies ||
            editedPreferences != selectedMember.preferences)

    LaunchedEffect(profiles) {
        if (profiles.isNotEmpty() && profiles.none { profile -> profile.id == selectedMemberId }) {
            selectedMemberId = profiles.first().id
        }
    }

    LaunchedEffect(selectedMember?.id, selectedMember?.vegetarian, selectedMember?.allergies, selectedMember?.preferences) {
        if (selectedMember != null) {
            editedVegetarian = selectedMember.vegetarian
            editedAllergies = selectedMember.allergies
            editedPreferences = selectedMember.preferences
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            DietConditionTopBar(onBackClick = onBackClick)
        },
        bottomBar = {
            DietConditionBottomButton(
                enabled = hasDietConditionChanges,
                onClick = {
                    selectedMember?.let { member ->
                        viewModel.updateDietCondition(
                            profileId = member.id,
                            vegetarian = editedVegetarian,
                            allergies = editedAllergies,
                            preferences = editedPreferences,
                        )
                    }
                },
            )
        },
        containerColor = JjikmukTheme.colors.surface,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            FamilyMemberSelector(
                members = profiles,
                selectedMemberId = selectedMemberId,
                onMemberClick = { selectedMemberId = it },
            )
            Spacer(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(JjikmukTheme.colors.surfaceSecondary),
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
            ) {
                if (selectedMember != null) {
                    DietConditionHeader(
                        member = selectedMember,
                        onEditClick = { editingProfile = selectedMember },
                    )
                    Spacer(modifier = Modifier.height(28.dp))
                    DietSectionTitle(text = "채식 유형", description = "단일 선택")
                    Spacer(modifier = Modifier.height(14.dp))
                    VegetarianGrid(
                        selectedTitle = editedVegetarian,
                        onOptionClick = { option ->
                            editedVegetarian = if (editedVegetarian == option.title) {
                                ""
                            } else {
                                option.title
                            }
                        },
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    DietSectionTitle(text = "주의 성분 · 알레르기", description = "다중 선택")
                    Spacer(modifier = Modifier.height(14.dp))
                    DietChipFlow(
                        values = allergyOptions,
                        selectedValues = editedAllergies,
                        selectedColor = Color(0xFFFF6B6B),
                        selectedBackground = Color(0xFFFFF2F2),
                        onValueClick = { value ->
                            editedAllergies = editedAllergies.toggleValue(value)
                        },
                    )
                    Spacer(modifier = Modifier.height(30.dp))
                    DietSectionTitle(text = "선호 조건", description = "다중 선택")
                    Spacer(modifier = Modifier.height(14.dp))
                    DietChipFlow(
                        values = preferenceOptions,
                        selectedValues = editedPreferences,
                        selectedColor = JjikmukTheme.colors.brand,
                        selectedBackground = JjikmukTheme.colors.info,
                        onValueClick = { value ->
                            editedPreferences = editedPreferences.toggleValue(value)
                        },
                    )
                }
            }
        }
    }

    editingProfile?.let { profile ->
        FamilyProfileEditBottomSheet(
            profile = profile,
            onDismiss = { editingProfile = null },
            onSave = { name, emoji, relation ->
                viewModel.updateProfile(
                    profileId = profile.id,
                    name = name,
                    emoji = emoji,
                    relation = relation,
                )
                editingProfile = null
            },
            onDelete = {
                viewModel.deleteProfile(profile.id)
                editingProfile = null
            },
        )
    }
}

@Composable
private fun DietConditionTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = JjikmukTheme.colors.surface,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .border(1.dp, JjikmukTheme.colors.borderSubtle),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp),
            ) {
                JjikmukBackButton(onClick = onBackClick)
            }
            Text(
                text = "식이 조건 관리",
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.labelL,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun FamilyMemberSelector(
    members: List<FamilyProfile>,
    selectedMemberId: String,
    onMemberClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(112.dp)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        members.forEach { member ->
            FamilyMemberTab(
                member = member,
                selected = member.id == selectedMemberId,
                onClick = { onMemberClick(member.id) },
            )
        }
        AddMemberTab()
    }
}

@Composable
private fun FamilyMemberTab(
    member: FamilyProfile,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)

    Column(
        modifier = modifier
            .width(54.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(shape)
                .background(if (selected) JjikmukTheme.colors.surface else JjikmukTheme.colors.surfaceSecondary)
                .border(
                    width = if (selected) 2.dp else 0.dp,
                    color = if (selected) JjikmukTheme.colors.brand else Color.Transparent,
                    shape = shape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = member.emoji,
                style = JjikmukTheme.typography.h3,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = member.name,
            color = if (selected) JjikmukTheme.colors.brand else JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.labelS,
        )
    }
}

@Composable
private fun AddMemberTab(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(54.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .border(1.dp, JjikmukTheme.colors.borderSubtle, RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "+",
                color = JjikmukTheme.colors.textTertiary,
                style = JjikmukTheme.typography.h2,
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "추가",
            color = JjikmukTheme.colors.textTertiary,
            style = JjikmukTheme.typography.labelS,
        )
    }
}

@Composable
private fun DietConditionHeader(
    member: FamilyProfile,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${member.emoji} ${member.name} 의 식이 조건",
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.titleL,
        )
        if (!member.isMe) {
            Surface(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onEditClick,
                ),
                color = JjikmukTheme.colors.surface,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, JjikmukTheme.colors.borderSubtle),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile_edit),
                        contentDescription = null,
                        tint = JjikmukTheme.colors.textSecondary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "프로필 수정",
                        color = JjikmukTheme.colors.textSecondary,
                        style = JjikmukTheme.typography.labelS,
                    )
                }
            }
        }
    }
}

@Composable
private fun DietSectionTitle(
    text: String,
    description: String? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.titleL,
        )
        if (description != null) {
            Text(
                text = description,
                color = JjikmukTheme.colors.textTertiary,
                style = JjikmukTheme.typography.caption,
            )
        }
    }
}

@Composable
private fun VegetarianGrid(
    selectedTitle: String,
    onOptionClick: (VegetarianOptionUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        vegetarianOptions.chunked(3).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                rowItems.forEach { option ->
                    VegetarianOptionCard(
                        option = option,
                        selected = option.title == selectedTitle,
                        onClick = { onOptionClick(option) },
                        modifier = Modifier.weight(1f),
                    )
                }
                repeat(3 - rowItems.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun VegetarianOptionCard(
    option: VegetarianOptionUiModel,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .height(108.dp)
            .clip(shape)
            .background(if (selected) JjikmukTheme.colors.info else JjikmukTheme.colors.surface)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) JjikmukTheme.colors.brand else JjikmukTheme.colors.borderSubtle,
                shape = shape,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = option.emoji, style = JjikmukTheme.typography.h3)
        Spacer(modifier = Modifier.height(7.dp))
        Text(
            text = option.title,
            color = if (selected) JjikmukTheme.colors.brand else JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.labelS,
        )
        Text(
            text = option.description,
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.caption,
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DietChipFlow(
    values: List<String>,
    selectedValues: Set<String>,
    selectedColor: Color,
    selectedBackground: Color,
    onValueClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        values.forEach { value ->
            DietConditionChip(
                text = value,
                selected = value in selectedValues,
                selectedColor = selectedColor,
                selectedBackground = selectedBackground,
                onClick = { onValueClick(value) },
            )
        }
    }
}

@Composable
private fun DietConditionChip(
    text: String,
    selected: Boolean,
    selectedColor: Color,
    selectedBackground: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)

    Surface(
        modifier = modifier
            .height(42.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        color = if (selected) selectedBackground else JjikmukTheme.colors.surface,
        shape = shape,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) selectedColor else JjikmukTheme.colors.borderSubtle,
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Text(
                    text = "✓",
                    color = selectedColor,
                    style = JjikmukTheme.typography.labelS,
                )
            }
            Text(
                text = text,
                color = if (selected) selectedColor else JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelS,
            )
        }
    }
}

@Composable
private fun DietConditionBottomButton(
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = JjikmukTheme.colors.surface,
        shadowElevation = 8.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp, top = 14.dp, end = 24.dp, bottom = 18.dp),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    ),
                color = if (enabled) JjikmukTheme.colors.brand else JjikmukTheme.colors.disabled,
                shape = RoundedCornerShape(16.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "수정하기",
                        color = if (enabled) JjikmukTheme.colors.surface else JjikmukTheme.colors.textTertiary,
                        style = JjikmukTheme.typography.labelL,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun FamilyProfileEditBottomSheet(
    profile: FamilyProfile,
    onDismiss: () -> Unit,
    onSave: (name: String, emoji: String, relation: String) -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedEmoji by rememberSaveable(profile.id) { mutableStateOf(profile.emoji) }
    var selectedRelation by rememberSaveable(profile.id) { mutableStateOf(profile.relation) }
    var name by rememberSaveable(profile.id) { mutableStateOf(profile.name) }
    val canSave = name.isNotBlank() &&
        (selectedEmoji != profile.emoji || selectedRelation != profile.relation || name != profile.name)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = JjikmukTheme.colors.surface,
        dragHandle = null,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(74.dp)
                    .border(1.dp, JjikmukTheme.colors.borderSubtle)
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "가족 프로필 수정",
                    color = JjikmukTheme.colors.textPrimary,
                    style = JjikmukTheme.typography.titleL,
                )
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(JjikmukTheme.colors.surfaceSecondary)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismiss,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "×",
                        color = JjikmukTheme.colors.textTertiary,
                        style = JjikmukTheme.typography.h3,
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 25.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(JjikmukTheme.colors.info)
                        .border(2.dp, JjikmukTheme.colors.brand, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = selectedEmoji,
                        style = JjikmukTheme.typography.h1,
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = name.ifBlank { profile.name },
                    color = JjikmukTheme.colors.textPrimary,
                    style = JjikmukTheme.typography.bodyL,
                )
                Spacer(modifier = Modifier.height(26.dp))

                SheetSectionTitle(text = "아이콘")
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    profileEmojiOptions.forEach { emoji ->
                        ProfileEmojiOption(
                            emoji = emoji,
                            selected = selectedEmoji == emoji,
                            onClick = { selectedEmoji = emoji },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                SheetSectionTitle(text = "관계")
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    profileRelationOptions.forEach { relation ->
                        ProfileRelationChip(
                            text = relation,
                            selected = selectedRelation == relation,
                            onClick = { selectedRelation = relation },
                        )
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                ProfileNameTextField(
                    value = name,
                    onValueChange = { name = it },
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, JjikmukTheme.colors.borderSubtle)
                    .padding(start = 16.dp, top = 17.dp, end = 16.dp, bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BottomSheetActionButton(
                    text = "삭제",
                    textColor = Color(0xFFE7000B),
                    backgroundColor = Color(0xFFFEF8F8),
                    borderColor = Color(0xFFFFD6D6),
                    onClick = onDelete,
                    modifier = Modifier.weight(0.78f),
                )
                BottomSheetActionButton(
                    text = "수정하기",
                    textColor = if (canSave) JjikmukTheme.colors.surface else JjikmukTheme.colors.textTertiary,
                    backgroundColor = if (canSave) JjikmukTheme.colors.brand else JjikmukTheme.colors.disabled,
                    borderColor = if (canSave) JjikmukTheme.colors.brand else JjikmukTheme.colors.border,
                    enabled = canSave,
                    largeText = true,
                    onClick = {
                        onSave(name.trim(), selectedEmoji, selectedRelation)
                    },
                    modifier = Modifier.weight(1.8f),
                )
            }
        }
    }
}

@Composable
private fun SheetSectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = JjikmukTheme.colors.textSecondary,
        style = JjikmukTheme.typography.labelS,
        modifier = modifier.fillMaxWidth(),
    )
}

@Composable
private fun ProfileEmojiOption(
    emoji: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (selected) JjikmukTheme.colors.info else JjikmukTheme.colors.surfaceSecondary)
            .border(
                width = if (selected) 2.dp else 0.dp,
                color = if (selected) JjikmukTheme.colors.brand else Color.Transparent,
                shape = RoundedCornerShape(12.dp),
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = emoji, style = JjikmukTheme.typography.h3)
    }
}

@Composable
private fun ProfileRelationChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(39.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        color = if (selected) JjikmukTheme.colors.brand else JjikmukTheme.colors.surface,
        shape = CircleShape,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) JjikmukTheme.colors.brand else JjikmukTheme.colors.borderSubtle,
        ),
    ) {
        Box(
            modifier = Modifier.padding(horizontal = 15.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = if (selected) JjikmukTheme.colors.surface else JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelS,
            )
        }
    }
}

@Composable
private fun ProfileNameTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = JjikmukTheme.typography.bodyM.copy(color = JjikmukTheme.colors.textPrimary),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(JjikmukTheme.colors.surfaceSecondary)
            .border(1.dp, JjikmukTheme.colors.borderSubtle, RoundedCornerShape(8.dp))
            .padding(horizontal = 18.dp),
        decorationBox = { innerTextField ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.CenterStart,
            ) {
                innerTextField()
            }
        },
    )
}

@Composable
private fun BottomSheetActionButton(
    text: String,
    textColor: Color,
    backgroundColor: Color,
    borderColor: Color,
    enabled: Boolean = true,
    largeText: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .height(47.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        color = backgroundColor,
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, borderColor),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = textColor,
                style = if (largeText) JjikmukTheme.typography.labelL else JjikmukTheme.typography.labelM,
            )
        }
    }
}

private data class VegetarianOptionUiModel(
    val title: String,
    val description: String,
    val emoji: String,
)

private val vegetarianOptions = listOf(
    VegetarianOptionUiModel("해당 없음", "채식 안 함", "🍽️"),
    VegetarianOptionUiModel("비건", "완전 채식", "🌱"),
    VegetarianOptionUiModel("락토", "유제품 허용", "🥛"),
    VegetarianOptionUiModel("오보", "계란 허용", "🥚"),
    VegetarianOptionUiModel("락토오보", "유제품/계란 허용", "🧀"),
    VegetarianOptionUiModel("페스코", "해산물 허용", "🐟"),
    VegetarianOptionUiModel("폴로", "닭고기 허용", "🍗"),
)

private val preferenceOptions = listOf(
    "저염",
    "글루텐프리",
    "저칼로리",
    "저당",
    "고단백",
    "저지방",
)

private val profileEmojiOptions = listOf(
    "🙂",
    "👩🏻",
    "👨🏻",
    "👵🏻",
    "👴🏻",
    "👧🏻",
    "👦🏻",
    "👶🏻",
    "👱🏻‍♀️",
    "🧑🏻‍🦱",
)

private val profileRelationOptions = listOf(
    "엄마",
    "아빠",
    "할아버지",
    "할머니",
    "배우자",
    "딸",
    "아들",
    "기타",
)

private fun Set<String>.toggleValue(value: String): Set<String> {
    return if (value in this) {
        this - value
    } else {
        this + value
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun DietConditionManagementScreenPreview() {
    JjikmukTheme {
        DietConditionManagementScreen(onBackClick = {})
    }
}
