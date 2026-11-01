package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.feature.auth.presentation.signup.SignUpCondition
import com.coworker.jjikmuk.feature.auth.presentation.signup.VegetarianDiet
import com.coworker.jjikmuk.ui.catalog.DietaryCondition
import com.coworker.jjikmuk.ui.catalog.FoodAllergy
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun SelfDietConditionRoute(
    onBackClick: () -> Unit,
    viewModel: SelfDietConditionViewModel = hiltViewModel(),
    topContent: @Composable () -> Unit = {},
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var confirmLeave by remember { mutableStateOf(false) }
    LaunchedEffect(viewModel) { viewModel.open() }
    fun leave() { viewModel.leave(); onBackClick() }
    val back = {
        if (!state.busy) {
            if (state.hasChanges || state.needsReadback) confirmLeave = true else leave()
        }
    }
    BackHandler(onBack = back)
    val allergyLabels = FoodAllergy.entries.associate { stringResource(it.labelRes) to it.id }
    val nutritionLabels = nutritionCatalog.entries.associate { stringResource(it.value.labelRes) to it.key }
    Scaffold(
        topBar = {
            Column {
                DietConditionTopBar(onBackClick = back)
                topContent()
            }
        },
        bottomBar = {
            Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                state.message?.let { Text(it, style = JjikmukTheme.typography.bodyM) }
                if (state.needsReadback || state.saved == null)
                    JjikmukSecondaryButton("서버 정보 다시 확인", viewModel::reload, enabled = !state.busy)
                JjikmukPrimaryButton(if (state.busy) "처리 중…" else "수정하기", viewModel::save, enabled = state.canSave)
            }
        },
        containerColor = JjikmukTheme.colors.surface,
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("내 정보에 적용할 알레르기와 식이조건을 선택해 주세요.", style = JjikmukTheme.typography.bodyM)
            if (state.saved != null && !state.unsupported) {
                DietConditionHeader(FamilyProfile("me", state.nickname, "🙂", "나", true, "", emptySet(), emptySet()), {})
                DietSectionTitle("채식 유형", "단일 선택")
                VegetarianGrid(
                    enabled = state.editable,
                    selectedTitle = vegetarianLabels[state.draft.vegetarian] ?: "해당 없음",
                    onOptionClick = { option -> viewModel.vegetarian(vegetarianLabels.entries.firstOrNull { it.value == option.title }?.key) },
                )
                DietSectionTitle("주의 성분 · 알레르기", "다중 선택")
                DietChipFlow(allergyLabels.keys.toList(), allergyLabels.filterValues { it in state.draft.allergies }.keys,
                    Color(0xFFFF6B6B), Color(0xFFFFF2F2), { allergyLabels[it]?.let(viewModel::allergy) }, enabled = state.editable)
                DietSectionTitle("선호 조건", "다중 선택")
                DietChipFlow(nutritionLabels.keys.toList(), nutritionLabels.filterValues { it in state.draft.nutrition }.keys,
                    JjikmukTheme.colors.brand, JjikmukTheme.colors.info, { nutritionLabels[it]?.let(viewModel::nutrition) }, enabled = state.editable)
            }
        }
    }
    if (confirmLeave) AlertDialog(
        onDismissRequest = { confirmLeave = false },
        title = { Text("식이조건 수정을 닫을까요?") },
        text = { Text(if (state.needsReadback) "서버 저장 여부가 아직 확인되지 않았어요. 다시 열면 서버 정보를 조회합니다. 저장하지 못한 선택은 사라져요."
            else "저장하지 않은 선택은 사라져요.") },
        confirmButton = { TextButton(onClick = { confirmLeave = false; leave() }) { Text("닫기") } },
        dismissButton = { TextButton(onClick = { confirmLeave = false }) { Text("계속 수정하기") } },
    )
}

private val vegetarianLabels = mapOf(VegetarianDiet.Vegan to "비건", VegetarianDiet.Lacto to "락토",
    VegetarianDiet.Ovo to "오보", VegetarianDiet.LactoOvo to "락토오보", VegetarianDiet.Pesco to "페스코", VegetarianDiet.Pollo to "폴로")
private val nutritionCatalog = mapOf(SignUpCondition.LowSodium to DietaryCondition.LOW_SODIUM,
    SignUpCondition.GlutenFree to DietaryCondition.GLUTEN_FREE, SignUpCondition.LowCalorie to DietaryCondition.LOW_CALORIE,
    SignUpCondition.LowSugar to DietaryCondition.LOW_SUGAR, SignUpCondition.HighProtein to DietaryCondition.HIGH_PROTEIN,
    SignUpCondition.LowFat to DietaryCondition.LOW_FAT)
