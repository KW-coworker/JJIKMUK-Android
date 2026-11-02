package com.coworker.jjikmuk.ui.component

import com.coworker.jjikmuk.domain.repository.AnalysisTargetsState

fun AnalysisTargetsState.toScanTargetMembers() = profiles.map { profile ->
    ScanTargetMemberUiModel(id = profile.id, name = profile.name, relation = if (profile.isMe) "나" else profile.relation,
        emoji = profile.emoji, isSelected = profile.id in selectedProfileIds, vegetarian = profile.vegetarian,
        allergies = profile.allergies, preferences = profile.preferences)
}
