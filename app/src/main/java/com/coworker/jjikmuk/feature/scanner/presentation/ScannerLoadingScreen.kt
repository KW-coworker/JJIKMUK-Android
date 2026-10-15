package com.coworker.jjikmuk.feature.scanner.presentation

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

private val LoadingMessages = listOf(
    "제품명을 인식하고 있어요",
    "상품을 찾고 있어요",
    "목록을 준비하고 있어요",
)
private val LoadingAccent = Color(0xFF8BE3A2)
private val CssEaseOut = CubicBezierEasing(0f, 0f, 0.58f, 1f)
private val CssTransitionEase = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
internal enum class ScannerLoadingStage { Recognizing, Searching, Preparing }

/**
 * Loading presentation only. The caller supplies the actual operation stage; no progress timer.
 * The route owns completion/cancellation. [photo] is the captured/imported local file;
 * a drawable resource is also accepted for previews.
 */
@Composable
internal fun ScannerLoadingScreen(
    photo: Any,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    stage: ScannerLoadingStage = ScannerLoadingStage.Recognizing,
) {
    val step = stage.ordinal
    val opacity = remember(photo) { Animatable(0f) }
    LaunchedEffect(photo) {
        opacity.animateTo(1f, tween(300, easing = CssTransitionEase))
    }
    BackHandler(onBack = onCancel)

    Box(
        modifier.fillMaxSize().clipToBounds()
            .graphicsLayer { alpha = opacity.value }
            // Block the scanner controls underneath, including while fading in.
            .pointerInput(Unit) { detectTapGestures {} },
    ) {
        AsyncImage(
            model = photo,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
                .graphicsLayer { scaleX = 1.1f; scaleY = 1.1f }
                .then(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) Modifier.blur(14.dp) else Modifier),
        )
        Box(Modifier.fillMaxSize().background(Color(0xFF0A140E).copy(alpha = 0.72f)))
        Box(Modifier.fillMaxSize().systemBarsPadding()) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 230.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                ScannerLoadingIndicator()
                Spacer(Modifier.height(32.dp))
                LoadingMessage(step)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "잠시만 기다려 주세요",
                    style = JjikmukTheme.typography.bodyM,
                    color = Color.White.copy(alpha = 0.6f),
                )
                Spacer(Modifier.height(28.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    LoadingMessages.indices.forEach { index ->
                        val width by animateDpAsState(
                            targetValue = if (index == step) 22.dp else 6.dp,
                            animationSpec = tween(300, easing = CssTransitionEase),
                            label = "Loading dot width",
                        )
                        val color by animateColorAsState(
                            targetValue = if (index <= step) LoadingAccent else Color.White.copy(alpha = 0.25f),
                            animationSpec = tween(300, easing = CssTransitionEase),
                            label = "Loading dot color",
                        )
                        Box(Modifier.width(width).height(6.dp).background(
                            color,
                            CircleShape,
                        ))
                    }
                }
            }
            Box(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 48.dp)
                    .height(44.dp).clip(CircleShape)
                    .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    .clickable(role = Role.Button, onClick = onCancel)
                    .padding(horizontal = 22.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text("취소", style = JjikmukTheme.typography.titleM, color = Color.White.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun LoadingMessage(step: Int) {
    // Re-enter only the new message, matching CSS key={step} + fade-up (no outgoing crossfade).
    key(step) {
        val progress = remember { Animatable(0f) }
        LaunchedEffect(Unit) { progress.animateTo(1f, tween(350, easing = CssEaseOut)) }
        Text(
            text = LoadingMessages[step],
            style = JjikmukTheme.typography.h2,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
                .graphicsLayer {
                    alpha = progress.value
                    translationY = 6.dp.toPx() * (1f - progress.value)
                }
                .semantics { liveRegion = LiveRegionMode.Polite },
        )
    }
}

@Composable
private fun ScannerLoadingIndicator() {
    val brand = JjikmukTheme.colors.brand
    val transition = rememberInfiniteTransition(label = "Scanner loading")
    // Tailwind animate-ping reaches scale(2)/opacity(0) at 75%, then holds until restarting.
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(keyframes {
            durationMillis = 1_800
            0f at 0 using CubicBezierEasing(0f, 0f, 0.2f, 1f)
            1f at 1_350
            1f at 1_800
        }),
        label = "Expanding pulse",
    )
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1_100, easing = LinearEasing)),
        label = "Search ring rotation",
    )
    Box(Modifier.size(120.dp), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize()
            .graphicsLayer { scaleX = 1f + pulse; scaleY = 1f + pulse; alpha = 1f - pulse }
            .background(brand.copy(alpha = 0.25f), CircleShape))
        Canvas(Modifier.size(100.dp).graphicsLayer { rotationZ = rotation }) {
            val stroke = 3.dp.toPx()
            drawCircle(Color.White.copy(alpha = 0.1f), radius = (size.minDimension - stroke) / 2f, style = Stroke(stroke))
            drawArc(
                color = LoadingAccent,
                startAngle = -135f,
                sweepAngle = 90f,
                useCenter = false,
                topLeft = Offset(stroke / 2f, stroke / 2f),
                size = Size(size.width - stroke, size.height - stroke),
                style = Stroke(stroke),
            )
        }
        Box(
            Modifier.size(72.dp)
                .dropShadow(CircleShape) {
                    radius = 24.dp.toPx()
                    offset = Offset(0f, 8.dp.toPx())
                    color = brand.copy(alpha = 0.5f)
                }
                .background(JjikmukTheme.colors.brandStrong, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(R.drawable.ic_scanner_loading_search), contentDescription = null,
                tint = Color.White, modifier = Modifier.size(32.dp))
        }
    }
}

@Preview(name = "스캐너 로딩 애니메이션", widthDp = 375, heightDp = 812)
@Composable
private fun ScannerLoadingPreview() {
    JjikmukTheme {
        ScannerLoadingScreen(photo = R.drawable.scanner_camera_preview, onCancel = {})
    }
}
