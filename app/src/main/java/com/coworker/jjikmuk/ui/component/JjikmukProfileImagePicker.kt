package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukProfileImagePicker(
    imageModel: Any?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(100.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .size(100.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.info)
                .border(4.dp, colors.surface, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            if (imageModel == null) {
                Image(
                    painter = painterResource(R.drawable.ic_profile_default),
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                )
            } else {
                AsyncImage(
                    model = imageModel,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize(),
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(32.dp)
                .shadow(4.dp, CircleShape)
                .clip(CircleShape)
                .background(colors.textPrimary)
                .border(2.dp, colors.surface, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                ),
        ) {
            Image(
                painter = painterResource(R.drawable.ic_profile_camera),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukProfileImagePickerPreview() {
    JjikmukTheme {
        JjikmukProfileImagePicker(
            imageModel = null,
            onClick = {},
        )
    }
}
