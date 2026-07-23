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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.Neutral200
import com.coworker.jjikmuk.ui.theme.Primary50
import com.coworker.jjikmuk.ui.theme.White

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
            .clickable(onClick = onClick),
        contentAlignment = Alignment.CenterEnd,
    ) {
        val displayProfiles = selectedProfiles.take(MAX_VISIBLE_PROFILE_COUNT)
        val myProfile = displayProfiles.firstOrNull { profile -> profile.id == ME_PROFILE_ID }
        val otherProfiles = displayProfiles.filterNot { profile -> profile.id == ME_PROFILE_ID }

        if (displayProfiles.isEmpty()) {
            ScanTargetProfileImage(
                imageResId = R.drawable.ic_launcher_foreground,
                emoji = null,
                rightOffset = 0,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
            return@Box
        }

        otherProfiles.asReversed().forEachIndexed { index, profile ->
            ScanTargetProfileImage(
                imageResId = profile.imageResId,
                emoji = profile.emoji,
                rightOffset = index * PROFILE_OVERLAP_OFFSET_DP,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }

        myProfile?.let { profile ->
            ScanTargetProfileImage(
                imageResId = profile.imageResId,
                emoji = profile.emoji,
                rightOffset = otherProfiles.size * PROFILE_OVERLAP_OFFSET_DP,
                modifier = Modifier.align(Alignment.CenterEnd),
            )
        }
    }
}

@Composable
private fun ScanTargetProfileImage(
    @DrawableRes imageResId: Int,
    emoji: String?,
    rightOffset: Int,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .offset(x = (-rightOffset).dp)
            .size(40.dp)
            .clip(CircleShape)
            .background(Primary50)
            .border(width = 2.dp, color = White, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        if (emoji != null) {
            Text(
                text = emoji,
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            Image(
                painter = painterResource(imageResId),
                contentDescription = null,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Neutral200),
            )
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
