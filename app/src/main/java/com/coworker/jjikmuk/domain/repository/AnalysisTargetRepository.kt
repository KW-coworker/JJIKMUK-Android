package com.coworker.jjikmuk.domain.repository

import com.coworker.jjikmuk.domain.model.FamilyProfile
import kotlinx.coroutines.flow.StateFlow

data class AnalysisTargetSelection(val account: FamilyAccount, val includeSelf: Boolean,
    val familyProfileIds: List<Long>, val conditionsRevision: String) {
    val mode: String get() = when { familyProfileIds.isNotEmpty() -> "selected"; includeSelf -> "self"; else -> "none" }
    val queryIncludeSelf: Boolean? get() = includeSelf.takeIf { mode == "selected" }
    val queryFamilyIds: List<Long>? get() = familyProfileIds.takeIf { mode == "selected" }
}
data class AnalysisTargetsState(val account: FamilyAccount? = null, val profiles: List<FamilyProfile> = emptyList(),
    val selectedProfileIds: Set<String> = emptySet(), val selection: AnalysisTargetSelection? = null,
    val ready: Boolean = false, val message: String? = null)

interface AnalysisTargetRepository {
    val state: StateFlow<AnalysisTargetsState>
    fun captureAccount(): FamilyAccount?
    suspend fun setSelected(id: String, checked: Boolean, account: FamilyAccount? = captureAccount()): Boolean
    suspend fun awaitSelection(account: FamilyAccount? = captureAccount()): AnalysisTargetSelection?
    fun isCurrent(selection: AnalysisTargetSelection): Boolean
}
