package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.Neutral200
import com.coworker.jjikmuk.ui.theme.Neutral300
import com.coworker.jjikmuk.ui.theme.Neutral900
import com.coworker.jjikmuk.ui.theme.Primary50
import com.coworker.jjikmuk.ui.theme.White

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageSourceBottomSheet(
    onDismissRequest: () -> Unit,
    onCameraClick: () -> Unit,
    onGalleryClick: () -> Unit,
    modifier: Modifier = Modifier,
    sheetState: SheetState = rememberModalBottomSheetState(),
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            ImageSourceBottomSheetHandle()
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ImageSourceOptionItem(
                icon = {
                    ImageSourceIcon(
                        iconRes = R.drawable.ic_product_camera,
                        contentDescription = "상품 사진 찍기",
                    )
                },
                text = "상품 사진 찍기",
                onClick = onCameraClick,
            )
            ImageSourceOptionItem(
                icon = {
                    ImageSourceIcon(
                        iconRes = R.drawable.ic_product_image_upload,
                        contentDescription = "상품 이미지 업로드",
                    )
                },
                text = "상품 이미지 업로드",
                onClick = onGalleryClick,
            )
        }
    }
}

@Composable
private fun ImageSourceBottomSheetHandle(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 6.dp)
                .background(
                    color = Neutral300,
                    shape = RoundedCornerShape(100.dp),
                ),
        )
    }
}

@Composable
private fun ImageSourceOptionItem(
    icon: @Composable () -> Unit,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .background(
                color = White,
                shape = RoundedCornerShape(16.dp),
            )
            .border(
                width = 1.dp,
                color = Neutral200,
                shape = RoundedCornerShape(16.dp),
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(
                    color = Primary50,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            icon()
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = text,
            color = Neutral900,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun ImageSourceIcon(
    iconRes: Int,
    contentDescription: String,
    modifier: Modifier = Modifier,
) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = contentDescription,
        tint = androidx.compose.ui.graphics.Color.Unspecified,
        modifier = modifier.size(28.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(showBackground = true)
@Composable
private fun ImageSourceBottomSheetPreview() {
    JjikmukTheme {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Neutral200),
        ) {
            ImageSourceBottomSheetHandle()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(White)
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ImageSourceOptionItem(
                    icon = {
                        ImageSourceIcon(
                            iconRes = R.drawable.ic_product_camera,
                            contentDescription = "상품 사진 찍기",
                        )
                    },
                    text = "상품 사진 찍기",
                    onClick = {},
                )
                ImageSourceOptionItem(
                    icon = {
                        ImageSourceIcon(
                            iconRes = R.drawable.ic_product_image_upload,
                            contentDescription = "상품 이미지 업로드",
                        )
                    },
                    text = "상품 이미지 업로드",
                    onClick = {},
                )
            }
        }
    }
}
