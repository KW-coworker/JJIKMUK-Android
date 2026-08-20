package com.coworker.jjikmuk.ui.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R

@Composable
fun JjikmukAuthTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // TODO: 공통 상단바 담당 팀원의 컴포넌트가 병합되면 이 임시 구현을 교체한다.
    Box(
        contentAlignment = Alignment.CenterStart,
        modifier = modifier
            .fillMaxWidth()
            .height(62.dp)
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_auth_back),
            contentDescription = "뒤로가기",
            modifier = Modifier
                .size(30.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onBackClick,
                ),
        )
    }
}
