package com.coworker.jjikmuk.ui.component

import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlin.math.min

@Composable
fun JjikmukAnimatedLogo(
    modifier: Modifier = Modifier,
    contentDescription: String = "JJIKMUK",
) {
    val infiniteTransition = rememberInfiniteTransition(label = "jjikmukLogoTransition")
    val animationProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = LOGO_ANIMATION_DURATION_MILLIS,
                easing = LinearEasing,
            ),
            repeatMode = RepeatMode.Restart,
        ),
        label = "jjikmukLogoProgress",
    )

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .semantics { this.contentDescription = contentDescription },
    ) {
        val size = min(this.size.width, this.size.height)
        if (size <= 0f) return@Canvas

        val left = (this.size.width - size) / 2f
        val top = (this.size.height - size) / 2f
        val scale = size / FIGMA_ICON_SIZE

        logoBars.forEach { bar ->
            drawLogoBar(
                bar = bar,
                left = left,
                top = top,
                logoSize = size,
                scale = scale,
                progress = (animationProgress + bar.phase) % 1f,
            )
        }
    }
}

private fun DrawScope.drawLogoBar(
    bar: LogoBar,
    left: Float,
    top: Float,
    logoSize: Float,
    scale: Float,
    progress: Float,
) {
    val rectLeft = left + bar.x * scale
    val rectTop = top + bar.y * scale
    val rectRight = rectLeft + bar.width * scale
    val rectBottom = rectTop + bar.height * scale

    val shader = LinearGradient(
        0f,
        top - logoSize,
        0f,
        top + logoSize,
        intArrayOf(
            bar.startColor.darken().toArgb(),
            bar.startColor.toArgb(),
            bar.endColor.lighten().toArgb(),
            bar.endColor.toArgb(),
            bar.startColor.darken().toArgb(),
        ),
        floatArrayOf(0f, 0.25f, 0.5f, 0.75f, 1f),
        Shader.TileMode.REPEAT,
    ).apply {
        setLocalMatrix(
            Matrix().apply {
                setTranslate(0f, progress * logoSize * 2f)
            },
        )
    }

    drawIntoCanvas { canvas ->
        canvas.nativeCanvas.drawRect(
            rectLeft,
            rectTop,
            rectRight,
            rectBottom,
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                this.shader = shader
            },
        )
    }
}

private fun Color.darken(): Color = Color(
    red = red * DARKEN_FACTOR,
    green = green * DARKEN_FACTOR,
    blue = blue * DARKEN_FACTOR,
    alpha = alpha,
)

private fun Color.lighten(): Color = Color(
    red = red + (1f - red) * LIGHTEN_FACTOR,
    green = green + (1f - green) * LIGHTEN_FACTOR,
    blue = blue + (1f - blue) * LIGHTEN_FACTOR,
    alpha = alpha,
)

private data class LogoBar(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val startColor: Color,
    val endColor: Color,
    val phase: Float,
)

private val logoBars = listOf(
    LogoBar(28.0249f, 0f, 6.05957f, 89f, Color(0xFF009520), Color(0xFF8BE3A2), 0f),
    LogoBar(11.3613f, 51.8848f, 6.05957f, 37.1149f, Color(0xFF009520), Color(0xFF8BE3A2), 0.18f),
    LogoBar(58.7021f, 0f, 6.05957f, 37.1149f, Color(0xFF8BE3A2), Color(0xFF009A98), 0.36f),
    LogoBar(44.6897f, 0f, 6.05957f, 89f, Color(0xFF8BE3A2), Color(0xFF009A98), 0.54f),
    LogoBar(72.7151f, 0f, 6.05957f, 89f, Color(0xFF8BE3A2), Color(0xFF009A98), 0.72f),
)

private const val FIGMA_ICON_SIZE = 89f
private const val DARKEN_FACTOR = 0.72f
private const val LIGHTEN_FACTOR = 0.18f
private const val LOGO_ANIMATION_DURATION_MILLIS = 1_800

@Preview(showBackground = true)
@Composable
private fun JjikmukAnimatedLogoPreview() {
    JjikmukTheme {
        JjikmukAnimatedLogo(
            modifier = Modifier.size(89.dp),
        )
    }
}
