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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
    logoModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
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
        logoModifier = logoModifier,
        titleModifier = titleModifier,
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun PermissionApprovalScreen(
    onAllowClick: (PermissionSelection) -> Unit,
    onLaterClick: () -> Unit,
    modifier: Modifier = Modifier,
    logoModifier: Modifier = Modifier,
    titleModifier: Modifier = Modifier,
) {
    var cameraSelected by remember { mutableStateOf(true) }
    var photoSelected by remember { mutableStateOf(false) }
    var notificationSelected by remember { mutableStateOf(false) }
    var showPermissionReminder by rememberSaveable { mutableStateOf(false) }

    if (showPermissionReminder) {
        ModalBottomSheet(
            onDismissRequest = { showPermissionReminder = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            containerColor = JjikmukTheme.colors.surface,
            scrimColor = Color.Black.copy(alpha = 0.25f),
            dragHandle = null,
        ) {
            PermissionReminderContent(
                onAllowClick = {
                    showPermissionReminder = false
                    onAllowClick(PermissionSelection(camera = true, photo = true, notification = true))
                },
                onLaterClick = {
                    showPermissionReminder = false
                    onLaterClick()
                },
            )
        }
    }

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
            modifier = logoModifier.size(73.dp),
        )
        Spacer(modifier = Modifier.height(31.dp))
        PermissionHeading(modifier = titleModifier)
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
                .clickable { showPermissionReminder = true },
        )
    }
}

@Composable
private fun PermissionReminderContent(
    onAllowClick: () -> Unit,
    onLaterClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Box(Modifier.size(width = 36.dp, height = 4.dp)
            .clip(RoundedCornerShape(4.dp)).background(Color(0xFFBDBDBD)))
        Spacer(Modifier.height(48.dp))
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(56.dp).background(Color(0xFFFFF4E5), CircleShape),
        ) {
            Icon(
                painterResource(R.drawable.ic_warning_triangle_yellow),
                contentDescription = null,
                tint = Color(0xFFFF8A00),
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "권한이 없으면\n서비스 이용이 어려워요",
            style = JjikmukTheme.typography.h3,
            color = JjikmukTheme.colors.textPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "권한을 허용하지 않으면 아래 기능을 사용할 수 없어요",
            style = JjikmukTheme.typography.bodyS,
            color = JjikmukTheme.colors.textSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(36.dp))
        Column(
            modifier = Modifier.fillMaxWidth()
                .background(Color(0xFFF9FAFB), RoundedCornerShape(18.dp))
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            PermissionUnavailableRow(R.drawable.ic_permission_camera, "스캐너 촬영으로 성분 분석")
            PermissionUnavailableRow(R.drawable.ic_permission_photo, "갤러리 사진으로 성분 확인")
            PermissionUnavailableRow(R.drawable.ic_permission_bell, "맞춤 안심 상품·이벤트 알림")
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "나중에 마이페이지 › 설정 › 앱 설정에서 다시 허용할 수 있어요",
            style = JjikmukTheme.typography.caption,
            color = Color(0xFFBDBDBD),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(28.dp))
        JjikmukPrimaryButton(
            text = "권한 허용하기",
            onClick = onAllowClick,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "그래도 나중에 할게요",
            style = JjikmukTheme.typography.titleM,
            color = JjikmukTheme.colors.textSecondary,
            modifier = Modifier.padding(top = 16.dp, bottom = 28.dp)
                .clickable(onClick = onLaterClick),
        )
    }
}

@Composable
private fun PermissionUnavailableRow(iconRes: Int, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.size(32.dp).background(Color.White, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(painterResource(iconRes), null,
                tint = JjikmukTheme.colors.textTertiary, modifier = Modifier.size(20.dp))
        }
        Text(text, modifier = Modifier.weight(1f),
            style = JjikmukTheme.typography.bodyM, color = JjikmukTheme.colors.textSecondary)
        Text("×", color = JjikmukTheme.colors.error, fontSize = 20.sp)
    }
}

@Composable
private fun PermissionHeading(modifier: Modifier = Modifier) {
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = JjikmukTheme.colors.brand)) {
                append("찍먹")
            }
            append("을 시작하기 전에")
        },
        color = JjikmukTheme.colors.textPrimary,
        style = JjikmukTheme.typography.h1,
        modifier = modifier,
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
