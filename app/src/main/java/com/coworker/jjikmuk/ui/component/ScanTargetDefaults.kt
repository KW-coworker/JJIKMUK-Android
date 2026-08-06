package com.coworker.jjikmuk.ui.component

import androidx.annotation.DrawableRes

fun defaultScanTargetMembers(): List<ScanTargetMemberUiModel> {
    return listOf(
        ScanTargetMemberUiModel(
            id = "me",
            name = "코워커",
            relation = "나",
            isSelected = true,
        ),
        ScanTargetMemberUiModel(
            id = "spouse",
            name = "김철수",
            relation = "배우자",
            emoji = "👨🏻",
            isSelected = true,
        ),
        ScanTargetMemberUiModel(
            id = "child",
            name = "김아기",
            relation = "자녀",
            emoji = "👶🏻",
            isSelected = false,
        ),
    )
}

fun List<ScanTargetMemberUiModel>.toScanTargetProfiles(
    @DrawableRes defaultImageResId: Int,
): List<ScanTargetProfileUiModel> {
    return filter { member -> member.isSelected }
        .map { member ->
            ScanTargetProfileUiModel(
                id = member.id,
                imageResId = defaultImageResId,
                emoji = member.emoji,
            )
        }
}
