package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.ui.component.JjikmukProfileImagePicker
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBar
import com.coworker.jjikmuk.ui.component.JjikmukTopAppBarLeading
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import com.coworker.jjikmuk.ui.theme.asEnglish

@Composable
fun ProfileEditScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var nickname by rememberSaveable { mutableStateOf("코워커") }

    Scaffold(
        topBar = {
            JjikmukTopAppBar(
                leading = JjikmukTopAppBarLeading.Back(onClick = onBackClick),
                showBottomDivider = true,
                centerContent = {
                    Text(
                        text = "프로필 수정",
                        color = JjikmukTheme.colors.textPrimary,
                        style = JjikmukTheme.typography.labelL,
                    )
                },
            )
        },
        containerColor = JjikmukTheme.colors.surface,
        modifier = modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(modifier = Modifier.height(42.dp))
            JjikmukProfileImagePicker(
                imageModel = null,
                onClick = {},
                containerSize = 108.dp,
                imageSize = 88.dp,
                imageBackgroundColor = JjikmukTheme.colors.textTertiary,
                imageBorderWidth = 3.dp,
                actionIconEndPadding = 3.dp,
                actionIconBottomPadding = 14.dp,
                showDefaultImage = false,
            )
            Spacer(modifier = Modifier.height(62.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                ProfileEditFieldGroup(
                    label = "이름(닉네임)",
                    value = nickname,
                    onValueChange = { nickname = it },
                    textStyle = JjikmukTheme.typography.labelM,
                    textColor = JjikmukTheme.colors.textPrimary,
                )
                Spacer(modifier = Modifier.height(20.dp))
                ProfileEditFieldGroup(
                    label = "이메일 계정",
                    value = "coworker@example.com",
                    onValueChange = {},
                    textStyle = JjikmukTheme.typography.labelM.asEnglish(),
                    textColor = JjikmukTheme.colors.textSecondary,
                    enabled = false,
                    helperText = "이메일 주소는 변경할 수 없습니다.",
                )
            }
        }
    }
}

@Composable
private fun ProfileEditFieldGroup(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    textStyle: TextStyle,
    textColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    helperText: String? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.labelM,
            modifier = Modifier.padding(start = 4.dp),
        )
        Spacer(modifier = Modifier.height(8.dp))
        ProfileEditTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            textStyle = textStyle.copy(color = textColor),
        )
        helperText?.let { text ->
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.bodyM,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun ProfileEditTextField(
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
    textStyle: TextStyle,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(8.dp)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = textStyle,
        cursorBrush = SolidColor(JjikmukTheme.colors.brand),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions.Default,
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(shape)
            .background(JjikmukTheme.colors.surfaceSecondary)
            .border(
                width = 1.dp,
                color = JjikmukTheme.colors.borderSubtle,
                shape = shape,
            )
            .padding(horizontal = 18.dp),
        decorationBox = { innerTextField ->
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxSize(),
            ) {
                innerTextField()
            }
        },
    )
}

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun ProfileEditScreenPreview() {
    JjikmukTheme {
        ProfileEditScreen(onBackClick = {})
    }
}
