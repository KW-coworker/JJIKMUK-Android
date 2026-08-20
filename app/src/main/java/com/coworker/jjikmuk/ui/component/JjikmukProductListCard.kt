package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

data class JjikmukProductListCardUiModel(
    val brandName: String,
    val productName: String,
    val barcode: String?,
    val imageUrl: String?,
    val allergyLabels: List<String>,
)

@Composable
fun JjikmukProductListCard(
    product: JjikmukProductListCardUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(146.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(JjikmukTheme.colors.surface)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.border,
                shape = RoundedCornerShape(20.dp),
            )
            .clickable(onClick = onClick)
            .padding(start = 18.dp, top = 13.dp, end = 13.dp, bottom = 13.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = product.brandName,
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelM,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = product.productName,
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.titleL,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )

            Row(
                modifier = Modifier.padding(top = 38.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                product.allergyLabels.take(MAX_VISIBLE_ALLERGY_COUNT).forEach { label ->
                    JjikmukAllergyChip(label = label)
                }
            }
        }

        JjikmukProductCardImage(
            imageUrl = product.imageUrl,
            contentDescription = product.productName,
        )
    }
}

@Composable
fun JjikmukAllergyChip(
    label: String,
    modifier: Modifier = Modifier,
) {
    val emoji = when (label) {
        "우유" -> "🥛"
        "땅콩" -> "🥜"
        "새우" -> "🦐"
        "밀" -> "🌾"
        "대두" -> "🫘"
        "계란" -> "🥚"
        else -> "·"
    }

    Text(
        text = "$emoji $label",
        color = JjikmukTheme.colors.error,
        style = JjikmukTheme.typography.labelS,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(JjikmukTheme.colors.warning)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.warning,
                shape = RoundedCornerShape(6.dp),
            )
            .padding(horizontal = 8.dp, vertical = 6.dp),
    )
}

@Composable
private fun JjikmukProductCardImage(
    imageUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(89.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(JjikmukTheme.colors.disabled)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = RoundedCornerShape(20.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isNullOrBlank()) {
            Text(
                text = "상품\n이미지",
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelS,
            )
        } else {
            AsyncImage(
                model = imageUrl,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )
        }
    }
}

private const val MAX_VISIBLE_ALLERGY_COUNT = 3

@Preview(showBackground = true, widthDp = 375)
@Composable
private fun JjikmukProductListCardPreview() {
    JjikmukTheme {
        JjikmukProductListCard(
            product = JjikmukProductListCardUiModel(
                brandName = "해태제과",
                productName = "해태 포키 블루베리",
                barcode = "8801019311345",
                imageUrl = null,
                allergyLabels = listOf("우유", "땅콩"),
            ),
            onClick = {},
            modifier = Modifier.padding(20.dp),
        )
    }
}
