package com.coworker.jjikmuk.feature.auth.presentation.permission

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.PERMISSION_GRANTED
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukAnimatedLogo
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.theme.Info
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun PermissionApprovalRoute(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        onContinue()
    }

    PermissionApprovalScreen(
        onAllowClick = { selection ->
            val selectedPermissions = buildRequiredPermissions(selection)
            val missingPermissions = selectedPermissions.filter { permission ->
                ContextCompat.checkSelfPermission(context, permission) != PERMISSION_GRANTED
            }
            if (missingPermissions.isEmpty()) {
                onContinue()
            } else {
                permissionLauncher.launch(missingPermissions.toTypedArray())
            }
        },
        onLaterClick = onContinue,
        modifier = modifier,
    )
}

@Composable
fun PermissionApprovalScreen(
    onAllowClick: (PermissionSelection) -> Unit,
    onLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cameraSelected by remember { mutableStateOf(true) }
    var photoSelected by remember { mutableStateOf(false) }
    var notificationSelected by remember { mutableStateOf(false) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(PermissionBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 20.dp),
    ) {
        Spacer(modifier = Modifier.height(53.dp))
        JjikmukAnimatedLogo(
            contentDescription = "JJIKMUK",
            modifier = Modifier.size(89.dp),
        )
        Spacer(modifier = Modifier.height(31.dp))
        PermissionHeading()
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "더 정확한 성분 분석을 위해\n아래 권한을 허용해 주세요",
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.bodyM,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(38.dp))
        PermissionCard(
            title = "카메라",
            description = "상품 바코드와 음식을 촬영해 성분을\n분석해요",
            iconRes = R.drawable.ic_permission_camera,
            iconBackground = Color(0xFFE9F9EE),
            checked = cameraSelected,
            onClick = { cameraSelected = !cameraSelected },
        )
        Spacer(modifier = Modifier.height(14.dp))
        PermissionCard(
            title = "사진",
            description = "갤러리에 저장된 사진으로도 성분을\n확인해요",
            iconRes = R.drawable.ic_permission_photo,
            iconBackground = Info,
            checked = photoSelected,
            onClick = { photoSelected = !photoSelected },
        )
        Spacer(modifier = Modifier.height(14.dp))
        PermissionCard(
            title = "알림",
            description = "맞춤 안심 상품과 여러 이벤트 알림을\n보내드려요",
            iconRes = R.drawable.ic_permission_bell,
            iconBackground = Color(0xFFFFF7ED),
            checked = notificationSelected,
            onClick = { notificationSelected = !notificationSelected },
        )
        Spacer(modifier = Modifier.weight(1f))
        JjikmukPrimaryButton(
            text = "허용하고 시작하기",
            onClick = {
                onAllowClick(
                    PermissionSelection(
                        camera = cameraSelected,
                        photo = photoSelected,
                        notification = notificationSelected,
                    ),
                )
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            text = "나중에 설정하기",
            color = JjikmukTheme.colors.textSecondary,
            style = JjikmukTheme.typography.titleM,
            modifier = Modifier
                .padding(top = 17.dp, bottom = 27.dp)
                .clickable(onClick = onLaterClick),
        )
    }
}

@Composable
private fun PermissionHeading() {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = JjikmukTheme.colors.brand)) {
                append("찍먹")
            }
            append("을 시작하기 전에")
        },
        color = JjikmukTheme.colors.textPrimary,
        style = JjikmukTheme.typography.h1,
    )
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    iconRes: Int,
    iconBackground: Color,
    checked: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .height(86.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(JjikmukTheme.colors.surface)
            .border(1.dp, JjikmukTheme.colors.borderSubtle, RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconBackground),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(24.dp),
            )
        }
        Spacer(modifier = Modifier.width(18.dp))
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.titleM,
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                color = JjikmukTheme.colors.textTertiary,
                style = JjikmukTheme.typography.bodyS,
            )
        }
        PermissionCheckIndicator(checked = checked)
    }
}

@Composable
private fun PermissionCheckIndicator(
    checked: Boolean,
    modifier: Modifier = Modifier,
) {
    if (checked) {
        Image(
            painter = painterResource(R.drawable.ic_condition_check),
            contentDescription = null,
            modifier = modifier.size(26.dp),
        )
    } else {
        Box(
            modifier = modifier
                .size(24.dp)
                .clip(CircleShape)
                .border(2.dp, JjikmukTheme.colors.border, CircleShape),
        )
    }
}

data class PermissionSelection(
    val camera: Boolean,
    val photo: Boolean,
    val notification: Boolean,
)

private fun buildRequiredPermissions(selection: PermissionSelection): List<String> {
    return buildList {
        if (selection.camera) {
            add(Manifest.permission.CAMERA)
        }
        if (selection.photo) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                add(Manifest.permission.READ_MEDIA_IMAGES)
            } else {
                add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }
        if (selection.notification && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private val PermissionBackground = Color(0xFFFCFCFF)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun PermissionApprovalScreenPreview() {
    JjikmukTheme {
        PermissionApprovalScreen(
            onAllowClick = {},
            onLaterClick = {},
        )
    }
}
