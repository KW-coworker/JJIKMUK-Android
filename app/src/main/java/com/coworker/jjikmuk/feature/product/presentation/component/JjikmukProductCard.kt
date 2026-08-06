package com.coworker.jjikmuk.feature.product.presentation.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

data class JjikmukProductCardUiModel(
    val brand: String,
    val name: String,
    val badge: String,
    @DrawableRes val imageResId: Int,
)

enum class JjikmukProductCardSize {
    Compact,
    Grid,
}

@Composable
fun JjikmukProductCard(
    product: JjikmukProductCardUiModel,
    modifier: Modifier = Modifier,
    size: JjikmukProductCardSize = JjikmukProductCardSize.Grid,
    showFavorite: Boolean = size == JjikmukProductCardSize.Grid,
) {
    val spec = productCardSpec(size)

    Surface(
        modifier = modifier
            .width(spec.cardWidth)
            .height(spec.cardHeight),
        color = JjikmukTheme.colors.surface,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = spec.shadowElevation,
        border = BorderStroke(
            width = 1.dp,
            color = JjikmukTheme.colors.borderSubtle,
        ),
    ) {
        Column(
            modifier = Modifier.padding(spec.contentPadding),
        ) {
            Box(
                modifier = Modifier
                    .width(spec.imageWidth)
                    .height(spec.imageHeight)
                    .clip(RoundedCornerShape(spec.imageCornerRadius))
                    .background(JjikmukTheme.colors.surfaceSecondary),
            ) {
                Image(
                    painter = painterResource(product.imageResId),
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop,
                )

                ProductBadge(
                    text = product.badge,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(start = 8.dp, top = 8.dp),
                )

                if (showFavorite) {
                    FavoritePlaceholderButton(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 8.dp, end = 8.dp),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .width(spec.textWidth)
                    .padding(top = spec.textTopPadding)
                    .padding(horizontal = spec.textHorizontalPadding),
            ) {
                Text(
                    text = product.brand,
                    color = JjikmukTheme.colors.textSecondary,
                    style = JjikmukTheme.typography.caption.asEnglish(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = product.name,
                    color = JjikmukTheme.colors.textPrimary,
                    style = when (size) {
                        JjikmukProductCardSize.Compact -> JjikmukTheme.typography.labelM
                        JjikmukProductCardSize.Grid -> JjikmukTheme.typography.labelS
                    },
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }
    }
}

@Composable
private fun ProductBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        color = JjikmukTheme.colors.surface.copy(alpha = 0.9f),
        shape = RoundedCornerShape(999.dp),
        shadowElevation = 2.dp,
    ) {
        Text(
            text = text,
            color = JjikmukTheme.colors.brand,
            style = JjikmukTheme.typography.labelS,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}

@Composable
private fun FavoritePlaceholderButton(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.size(28.dp),
        color = JjikmukTheme.colors.surface.copy(alpha = 0.8f),
        shape = CircleShape,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "♡",
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelS,
            )
        }
    }
}

private data class ProductCardSpec(
    val cardWidth: Dp,
    val cardHeight: Dp,
    val contentPadding: Dp,
    val imageWidth: Dp,
    val imageHeight: Dp,
    val imageCornerRadius: Dp,
    val textWidth: Dp,
    val textTopPadding: Dp,
    val textHorizontalPadding: Dp,
    val shadowElevation: Dp,
)

private fun productCardSpec(size: JjikmukProductCardSize): ProductCardSpec =
    when (size) {
        JjikmukProductCardSize.Compact -> ProductCardSpec(
            cardWidth = 150.dp,
            cardHeight = 238.dp,
            contentPadding = 11.dp,
            imageWidth = 128.dp,
            imageHeight = 130.dp,
            imageCornerRadius = 12.dp,
            textWidth = 128.dp,
            textTopPadding = 12.dp,
            textHorizontalPadding = 4.dp,
            shadowElevation = 8.dp,
        )

        JjikmukProductCardSize.Grid -> ProductCardSpec(
            cardWidth = 162.dp,
            cardHeight = 262.dp,
            contentPadding = 1.dp,
            imageWidth = 160.dp,
            imageHeight = 191.dp,
            imageCornerRadius = 15.dp,
            textWidth = 160.dp,
            textTopPadding = 12.dp,
            textHorizontalPadding = 12.dp,
            shadowElevation = 4.dp,
        )
    }

@Preview(showBackground = true)
@Composable
private fun JjikmukProductCardCompactPreview() {
    JjikmukTheme {
        JjikmukProductCard(
            product = previewProduct,
            size = JjikmukProductCardSize.Compact,
            showFavorite = false,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukProductCardGridPreview() {
    JjikmukTheme {
        JjikmukProductCard(
            product = previewProduct,
            size = JjikmukProductCardSize.Grid,
        )
    }
}

private val previewProduct = JjikmukProductCardUiModel(
    brand = "아이얌",
    name = "글루텐프리 유기농 쌀과자",
    badge = "밀가루 무첨가",
    imageResId = R.drawable.img_gluten_free_rice_cookie,
)
