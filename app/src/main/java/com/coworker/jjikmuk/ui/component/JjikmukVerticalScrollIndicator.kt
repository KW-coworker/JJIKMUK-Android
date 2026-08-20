package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukVerticalScrollIndicator(
    scrollValue: Int,
    scrollMaxValue: Int,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val scrollFraction by remember(scrollValue, scrollMaxValue) {
        derivedStateOf {
            if (scrollMaxValue == 0) 0f else scrollValue.toFloat() / scrollMaxValue
        }
    }

    BoxWithConstraints(
        modifier = modifier
            .width(4.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(colors.border.copy(alpha = 0.55f)),
    ) {
        val thumbHeight = 44.dp
        val thumbTravel = maxHeight - thumbHeight
        Box(
            modifier = Modifier
                .offset(y = thumbTravel * scrollFraction)
                .size(width = 4.dp, height = thumbHeight)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.textSecondary),
        )
    }
}
