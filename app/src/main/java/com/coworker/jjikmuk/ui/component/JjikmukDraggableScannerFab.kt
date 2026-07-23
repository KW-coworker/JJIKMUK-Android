package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlin.math.roundToInt

@Composable
fun JjikmukDraggableScannerFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var offsetX by rememberSaveable { mutableFloatStateOf(0f) }
    var offsetY by rememberSaveable { mutableFloatStateOf(0f) }

    Image(
        painter = painterResource(R.drawable.ic_scan_camera),
        contentDescription = "음식 촬영",
        modifier = modifier
            .then(
                Modifier.pointerInput(Unit) {
                    detectTapGestures(onTap = { onClick() })
                },
            )
            .then(
                Modifier.pointerInput(Unit) {
                    detectDragGesturesAfterLongPress(
                        onDrag = { change, dragAmount: Offset ->
                            change.consume()
                            offsetX += dragAmount.x
                            offsetY += dragAmount.y
                        },
                    )
                },
            )
            .size(59.dp)
            .then(
                Modifier.offset {
                    IntOffset(
                        x = offsetX.roundToInt(),
                        y = offsetY.roundToInt(),
                    )
                },
            ),
    )
}

@Preview(showBackground = true)
@Composable
private fun JjikmukDraggableScannerFabPreview() {
    JjikmukTheme {
        Box(
            modifier = Modifier
                .size(160.dp)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            JjikmukDraggableScannerFab(
                onClick = {},
            )
        }
    }
}
