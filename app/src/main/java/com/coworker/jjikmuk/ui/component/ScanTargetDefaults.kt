package com.coworker.jjikmuk.ui.component

import androidx.annotation.DrawableRes

fun List<ScanTargetMemberUiModel>.toScanTargetProfiles(
    @DrawableRes defaultImageResId: Int,
): List<ScanTargetProfileUiModel> {
    // Members not loaded yet: show "me" so the account photo appears instead of a generic icon.
    if (isEmpty()) return listOf(ScanTargetProfileUiModel(id = "me", imageResId = defaultImageResId))
    return filter { member -> member.isSelected }
        .map { member ->
            ScanTargetProfileUiModel(
                id = member.id,
                imageResId = defaultImageResId,
                emoji = member.emoji,
            )
        }
}
