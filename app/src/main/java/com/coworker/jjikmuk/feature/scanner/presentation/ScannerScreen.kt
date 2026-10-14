package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish
import com.coworker.jjikmuk.ui.theme.Neutral350
import com.coworker.jjikmuk.ui.theme.Neutral600

enum class ScannerMode {
    Normal,
    Compare,
}

@Composable
fun ScannerScreen(
    mode: ScannerMode,
    onModeChange: (ScannerMode) -> Unit,
    onBackClick: () -> Unit,
    onFlashClick: () -> Unit,
    onGalleryClick: () -> Unit,
    onShutterClick: () -> Unit,
    onCompareListClick: () -> Unit,
    modifier: Modifier = Modifier,
    shutterEnabled: Boolean = true,
    galleryEnabled: Boolean = true,
    flashEnabled: Boolean = true,
    flashOn: Boolean = false,
    cameraContent: @Composable () -> Unit = {
        Image(
            painter = painterResource(R.drawable.scanner_camera_preview),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
    },
) {
    BackHandler(onBack = onBackClick)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.background)
            .systemBarsPadding()
            .clipToBounds(),
    ) {
        // Figma's 375×812 frame has 38px/20px mock system bars; Android supplies real insets.
        val guideScaleX = maxWidth / 375.dp
        val guideScaleY = maxHeight / 754.dp
        cameraContent()
        ScannerFocusMask(Modifier.fillMaxSize())

        ScannerGuidance(
            modifier = Modifier.align(Alignment.TopCenter)
                .padding(top = 65.dp * guideScaleY, start = 16.dp, end = 16.dp),
        )

        ScannerTopBar(
            onBackClick = onBackClick,
            onFlashClick = onFlashClick,
            flashEnabled = flashEnabled,
            flashOn = flashOn,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp, start = 22.dp, end = 22.dp),
        )

        ScannerGuideFrame(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp * guideScaleY)
                .size(width = 295.dp * guideScaleX, height = 353.dp * guideScaleY),
        )

        ScannerModeSelector(
            mode = mode,
            onModeChange = onModeChange,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 191.dp),
        )

        ScannerControls(
            onGalleryClick = onGalleryClick,
            onShutterClick = onShutterClick,
            shutterEnabled = shutterEnabled,
            galleryEnabled = galleryEnabled,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 65.dp),
        )

        if (mode == ScannerMode.Compare) {
            CompareScanListTab(
                onClick = onCompareListClick,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
    }
}

/** Render the exported Figma mask, leaving the camera inside its cutout untouched. */
@Composable
private fun ScannerFocusMask(modifier: Modifier = Modifier) {
    val mask = painterResource(R.drawable.scanner_focus_mask)
    Canvas(modifier) {
        val scaleY = size.height / 754f
        translate(top = -38f * scaleY) {
            with(mask) { draw(Size(size.width, 812f * scaleY)) }
        }
    }
}

@Composable
private fun ScannerGuidance(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .widthIn(min = 270.dp)
            .background(JjikmukTheme.colors.textPrimary.copy(alpha = 0.6f), CircleShape)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally),
    ) {
        Image(painterResource(R.drawable.ic_scanner_guide), null, Modifier.size(14.dp))
        Text(
            text = "제품 포장 앞면을 네모칸 안에 맞춰주세요",
            style = JjikmukTheme.typography.bodyS.asEnglish(),
            color = Color.White,
            modifier = Modifier.heightIn(min = 18.dp),
        )
    }
}

@Composable
private fun ScannerTopBar(
    onBackClick: () -> Unit,
    onFlashClick: () -> Unit,
    flashEnabled: Boolean,
    flashOn: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.width(331.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_scanner_back),
            contentDescription = "뒤로가기",
            modifier = Modifier
                .size(41.dp)
                .clickable(role = Role.Button, onClick = onBackClick),
        )
        Spacer(modifier = Modifier.weight(1f))
        Image(
            painter = painterResource(R.drawable.ic_scanner_flash),
            contentDescription = "촬영 플래시",
            modifier = Modifier
                .size(41.dp)
                .border(if (flashOn) 2.dp else 0.dp, if (flashOn) Color.Yellow else Color.Transparent, CircleShape)
                .semantics { stateDescription = if (!flashEnabled) "사용 불가" else if (flashOn) "켜짐" else "꺼짐" }
                .alpha(if (flashEnabled) 1f else 0.4f)
                .clickable(enabled = flashEnabled, role = Role.Button, onClick = onFlashClick),
        )
    }
}

@Composable
private fun ScannerGuideFrame(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val cornerRadius = 16.dp.toPx()
        val horizontalLength = 82.dp.toPx()
        val verticalLength = 55.dp.toPx()
        val width = size.width
        val height = size.height
        val path = Path().apply {
            moveTo(0f, verticalLength)
            lineTo(0f, cornerRadius)
            quadraticTo(0f, 0f, cornerRadius, 0f)
            lineTo(horizontalLength, 0f)

            moveTo(width - horizontalLength, 0f)
            lineTo(width - cornerRadius, 0f)
            quadraticTo(width, 0f, width, cornerRadius)
            lineTo(width, verticalLength)

            moveTo(width, height - verticalLength)
            lineTo(width, height - cornerRadius)
            quadraticTo(width, height, width - cornerRadius, height)
            lineTo(width - horizontalLength, height)

            moveTo(horizontalLength, height)
            lineTo(cornerRadius, height)
            quadraticTo(0f, height, 0f, height - cornerRadius)
            lineTo(0f, height - verticalLength)
        }
        drawPath(
            path = path,
            color = Color.White,
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
    }
}

@Composable
private fun ScannerModeSelector(
    mode: ScannerMode,
    onModeChange: (ScannerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        ScannerModeLabel(
            label = "일반 스캔",
            selected = mode == ScannerMode.Normal,
            onClick = { onModeChange(ScannerMode.Normal) },
        )
        Spacer(modifier = Modifier.width(35.dp))
        ScannerModeLabel(
            label = "비교 스캔",
            selected = mode == ScannerMode.Compare,
            onClick = { onModeChange(ScannerMode.Compare) },
        )
    }
}

@Composable
private fun ScannerModeLabel(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (selected) Color.White else Neutral350,
        style = JjikmukTheme.typography.labelL,
        modifier = Modifier.clickable(role = Role.Tab, onClick = onClick),
    )
}

@Composable
private fun ScannerControls(
    onGalleryClick: () -> Unit,
    onShutterClick: () -> Unit,
    shutterEnabled: Boolean,
    galleryEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .width(303.dp)
            .height(90.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.ic_scanner_gallery),
            contentDescription = "갤러리에서 선택",
            modifier = Modifier
                .size(52.dp)
                .alpha(if (galleryEnabled) 1f else 0.4f)
                .clickable(enabled = galleryEnabled, role = Role.Button, onClick = onGalleryClick),
        )
        Spacer(modifier = Modifier.weight(1f))
        ScannerShutterButton(onClick = onShutterClick, enabled = shutterEnabled)
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(52.dp))
    }
}

@Composable
private fun ScannerShutterButton(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_scanner_shutter),
        contentDescription = "상품 촬영",
        modifier = modifier
            .size(90.dp)
            .alpha(if (enabled) 1f else 0.4f)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick),
    )
}

@Composable
private fun CompareScanListTab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .shadow(
                elevation = 7.dp,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
                clip = false,
            )
            .background(
                color = JjikmukTheme.colors.borderSubtle,
                shape = RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp),
            )
            .size(width = 135.dp, height = 42.dp)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_scanner_chevron_up),
            contentDescription = null,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 8.dp)
                .size(width = 11.dp, height = 7.dp),
        )
        Text(
            text = "비교 스캔 목록",
            color = Neutral600,
            style = JjikmukTheme.typography.labelS,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp),
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ScannerScreenNormalPreview() {
    JjikmukTheme {
        ScannerScreen(
            mode = ScannerMode.Normal,
            onModeChange = {},
            onBackClick = {},
            onFlashClick = {},
            onGalleryClick = {},
            onShutterClick = {},
            onCompareListClick = {},
        )
    }
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ScannerScreenComparePreview() {
    JjikmukTheme {
        ScannerScreen(
            mode = ScannerMode.Compare,
            onModeChange = {},
            onBackClick = {},
            onFlashClick = {},
            onGalleryClick = {},
            onShutterClick = {},
            onCompareListClick = {},
        )
    }
}
