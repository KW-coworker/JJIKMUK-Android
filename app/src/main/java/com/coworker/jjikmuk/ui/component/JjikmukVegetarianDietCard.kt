package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun JjikmukVegetarianDietCard(
    title: String,
    description: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .background(if (selected) colors.info else colors.surface, shape)
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) colors.edit else colors.border,
                shape = shape,
            )
            .selectable(
                selected = selected,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(20.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = JjikmukTheme.typography.titleL,
            )
            Text(
                text = description,
                color = colors.textSecondary,
                style = JjikmukTheme.typography.bodyS,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        if (selected) {
            Image(
                painter = painterResource(R.drawable.ic_condition_check),
                contentDescription = null,
                modifier = Modifier.size(32.dp),
            )
        } else {
            Spacer(
                modifier = Modifier
                    .size(24.dp)
                    .border(2.dp, colors.border, CircleShape),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun JjikmukVegetarianDietCardPreview() {
    JjikmukTheme {
        JjikmukVegetarianDietCard(
            title = "비건 (Vegan)",
            description = "오직 식물성 음식만 섭취해요",
            selected = true,
            onClick = {},
            modifier = Modifier.padding(22.dp),
        )
    }
}
