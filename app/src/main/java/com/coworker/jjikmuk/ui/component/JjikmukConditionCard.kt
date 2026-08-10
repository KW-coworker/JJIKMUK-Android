package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.toggleable
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
fun JjikmukConditionCard(
    title: String,
    description: String,
    selected: Boolean,
    onSelectedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = JjikmukTheme.colors
    val shape = RoundedCornerShape(16.dp)

    Box(
        modifier = modifier
            .height(80.dp)
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
            ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(17.dp),
        ) {
            Text(
                text = title,
                color = colors.textPrimary,
                style = JjikmukTheme.typography.titleL,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = description,
                color = colors.textSecondary,
                maxLines = 1,
                style = JjikmukTheme.typography.bodyS,
            )
        }

        if (selected) {
            Image(
                painter = painterResource(R.drawable.ic_condition_check),
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 6.dp, y = (-12).dp)
                    .size(32.dp),
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 190, heightDp = 110)
@Composable
private fun JjikmukConditionCardPreview() {
    JjikmukTheme {
        JjikmukConditionCard(
            title = "채식",
            description = "비건, 락토, 오보 등",
            selected = true,
            onSelectedChange = {},
            modifier = Modifier.padding(12.dp),
        )
    }
}
