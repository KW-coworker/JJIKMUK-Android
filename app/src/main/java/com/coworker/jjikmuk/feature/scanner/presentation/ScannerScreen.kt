package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.Neutral350
import com.coworker.jjikmuk.ui.theme.Neutral600

enum class ScannerMode {
    Normal,
    Compare,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerMainRoute(
    onBackClick: () -> Unit,
    onCompareListClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by rememberSaveable { mutableStateOf(ScannerMode.Normal) }
    var normalResult by rememberSaveable { mutableStateOf<ScannerResultPlaceholder?>(null) }
    var nextNormalResult by rememberSaveable { mutableStateOf(ScannerResultPlaceholder.Safe) }
    var comparedProductCount by rememberSaveable { mutableStateOf(1) }
    var showCompareList by rememberSaveable { mutableStateOf(false) }

    ScannerScreen(
        mode = mode,
        onModeChange = { mode = it },
        onBackClick = onBackClick,
        onFlashClick = {},
        onGalleryClick = {},
        onShutterClick = {
            if (mode == ScannerMode.Normal) {
                normalResult = nextNormalResult
                nextNormalResult = if (nextNormalResult == ScannerResultPlaceholder.Safe) {
                    ScannerResultPlaceholder.Warning
                } else {
                    ScannerResultPlaceholder.Safe
                }
            } else {
                comparedProductCount = (comparedProductCount + 1).coerceAtMost(3)
            }
        },
        onCompareListClick = { showCompareList = true },
        modifier = modifier,
    )

    normalResult?.let { result ->
        ScannerResultPlaceholderSheet(
            result = result,
            onDismissRequest = { normalResult = null },
        )
    }

    if (showCompareList) {
        ScannerCompareListPlaceholderSheet(
            productCount = comparedProductCount,
            onDismissRequest = { showCompareList = false },
            onCompareClick = {
                showCompareList = false
                onCompareListClick()
            },
        )
    }
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
) {
    BackHandler(onBack = onBackClick)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.background)
            .systemBarsPadding(),
    ) {
        Image(
            painter = painterResource(R.drawable.scanner_camera_preview),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        ScannerTopBar(
            onBackClick = onBackClick,
            onFlashClick = onFlashClick,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp, start = 22.dp, end = 22.dp),
        )

        ScannerGuideFrame(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 120.dp)
                .size(width = 295.dp, height = 353.dp),
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

@Composable
private fun ScannerTopBar(
    onBackClick: () -> Unit,
    onFlashClick: () -> Unit,
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
            contentDescription = "플래시",
            modifier = Modifier
                .size(41.dp)
                .clickable(role = Role.Button, onClick = onFlashClick),
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
                .clickable(role = Role.Button, onClick = onGalleryClick),
        )
        Spacer(modifier = Modifier.weight(1f))
        ScannerShutterButton(onClick = onShutterClick)
        Spacer(modifier = Modifier.weight(1f))
        Spacer(modifier = Modifier.size(52.dp))
    }
}

@Composable
private fun ScannerShutterButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(R.drawable.ic_scanner_shutter),
        contentDescription = "상품 촬영",
        modifier = modifier
            .size(90.dp)
            .clickable(role = Role.Button, onClick = onClick),
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
