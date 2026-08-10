package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukAllergyChip(
    emoji: String,
    label: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Row(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .background(
                color = if (selected) colors.info else colors.surface,
                shape = shape,
            )
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.edit else colors.border,
                shape = shape,
            )
            .toggleable(
                value = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Checkbox,
                onValueChange = onSelectedChange,
            )
            .padding(
                horizontal = if (selected) 18.dp else 17.dp,
                vertical = if (selected) 14.dp else 13.dp,
            ),
    ) {
        Text(
            text = emoji,
            color = colors.textPrimary,
            fontSize = 18.sp,
            lineHeight = 18.sp,
        )
        Text(
            text = label,
            color = colors.textPrimary,
            style = JjikmukTheme.typography.titleL,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukAllergyChipPreview() {
    JjikmukTheme {
        JjikmukAllergyChip(
            emoji = "🦀",
            label = "게",
            selected = true,
            onSelectedChange = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
