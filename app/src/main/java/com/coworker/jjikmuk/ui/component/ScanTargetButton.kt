package com.coworker.jjikmuk.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

data class ScanTargetProfileUiModel(
    val id: String,
    @DrawableRes val imageResId: Int,
    val emoji: String? = null,
)

@Composable
fun ScanTargetButton(
    selectedProfiles: List<ScanTargetProfileUiModel>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(width = 100.dp, height = 40.dp)
            .clickable(onClick = onClick)
            .semantics { contentDescription = "분석 대상 선택" },
        contentAlignment = Alignment.CenterEnd,
    ) {
        val displayProfiles = selectedProfiles.take(MAX_VISIBLE_PROFILE_COUNT)
        val myProfile = displayProfiles.firstOrNull { profile -> profile.id == ME_PROFILE_ID }
        val otherProfiles = displayProfiles.filterNot { profile -> profile.id == ME_PROFILE_ID }

        if (displayProfiles.isEmpty()) {
            ScanTargetProfileImage(
                imageResId = R.drawable.ic_launcher_foreground,
                emoji = null,
                rightOffset = 0.dp,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            return@Box
        }

        otherProfiles.asReversed().forEachIndexed { index, profile ->
            ScanTargetProfileImage(
                imageResId = profile.imageResId,
                emoji = profile.emoji,
                rightOffset = (index * PROFILE_OVERLAP_OFFSET_DP).dp,
                emojiOffsetX = COVERED_PROFILE_EMOJI_OFFSET_DP,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        myProfile?.let { profile ->
            ScanTargetProfileImage(
                imageResId = profile.imageResId,
                emoji = profile.emoji,
                rightOffset = (otherProfiles.size * PROFILE_OVERLAP_OFFSET_DP).dp,
                emojiOffsetX = 0,
                isMe = true,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

/**
 * 선택된 분석 대상 프로필을 작게 겹쳐 보여 준다. ScanTargetButton과 같은 순서(나는 맨 왼쪽·맨 위)로 그린다.
 * 선택된 대상이 없으면 아무것도 그리지 않는다.
 */
@Composable
fun ScanTargetProfileStack(
    selectedProfiles: List<ScanTargetProfileUiModel>,
    modifier: Modifier = Modifier,
    avatarSize: Dp = 28.dp,
    overlapStep: Dp = 20.dp,
) {
    val displayProfiles = selectedProfiles.take(MAX_VISIBLE_PROFILE_COUNT)
    if (displayProfiles.isEmpty()) return
    val myProfile = displayProfiles.firstOrNull { profile -> profile.id == ME_PROFILE_ID }
    val otherProfiles = displayProfiles.filterNot { profile -> profile.id == ME_PROFILE_ID }

    Box(
        modifier = modifier
            .size(width = avatarSize + overlapStep * (displayProfiles.size - 1), height = avatarSize)
            .semantics { contentDescription = "선택한 분석 대상 ${displayProfiles.size}명" },
    ) {
        otherProfiles.asReversed().forEachIndexed { index, profile ->
            ScanTargetProfileImage(
                imageResId = profile.imageResId,
                emoji = profile.emoji,
                rightOffset = overlapStep * index,
                avatarSize = avatarSize,
                emojiFontSize = STACK_EMOJI_FONT_SIZE,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
        myProfile?.let { profile ->
            ScanTargetProfileImage(
                imageResId = profile.imageResId,
                emoji = profile.emoji,
                rightOffset = overlapStep * otherProfiles.size,
                avatarSize = avatarSize,
                emojiFontSize = STACK_EMOJI_FONT_SIZE,
                isMe = true,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun ScanTargetProfileImage(
    @DrawableRes imageResId: Int,
    emoji: String?,
    rightOffset: Dp,
    emojiOffsetX: Int = 0,
    isMe: Boolean = false,
    avatarSize: Dp = 40.dp,
    emojiFontSize: TextUnit? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset(x = -rightOffset)
            .size(avatarSize)
            .clip(CircleShape)
            .background(JjikmukTheme.colors.brandSubtlest)
            .border(width = 2.dp, color = JjikmukTheme.colors.surface, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        com.coworker.jjikmuk.feature.auth.photo.AccountAvatarContent(isMe) {
            if (emoji != null) {
                Text(
                    text = emoji,
                    style = emojiFontSize?.let { MaterialTheme.typography.headlineMedium.copy(fontSize = it, lineHeight = it) }
                        ?: MaterialTheme.typography.headlineMedium,
                    modifier = Modifier.offset(x = emojiOffsetX.dp),
                )
            } else {
                Image(
                    painter = painterResource(imageResId),
                    contentDescription = null,
                    modifier = Modifier
                        .size(avatarSize - 4.dp)
                        .clip(CircleShape)
                        .background(JjikmukTheme.colors.borderSubtle),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ScanTargetButtonPreview() {
    JjikmukTheme {
        ScanTargetButton(
            selectedProfiles = listOf(
                ScanTargetProfileUiModel(
                    id = "me",
                    imageResId = R.drawable.ic_launcher_foreground,
                ),
                ScanTargetProfileUiModel(
                    id = "spouse",
                    imageResId = R.drawable.ic_launcher_foreground,
                    emoji = "👨🏻",
                ),
                ScanTargetProfileUiModel(
                    id = "child",
                    imageResId = R.drawable.ic_launcher_foreground,
                    emoji = "👶🏻",
                ),
            ),
            onClick = {},
        )
    }
}

private const val ME_PROFILE_ID = "me"
private const val MAX_VISIBLE_PROFILE_COUNT = 5
private const val PROFILE_OVERLAP_OFFSET_DP = 12
private const val COVERED_PROFILE_EMOJI_OFFSET_DP = 8
private val STACK_EMOJI_FONT_SIZE = 18.sp
