package com.coworker.jjikmuk.feature.scanner.presentation

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

private enum class ReportStatus { Safe, Danger }
private data class ReportProduct(
    val status: ReportStatus,
    val brand: String,
    val name: String,
    @DrawableRes val image: Int,
    val details: List<Pair<String, String?>>,
)

private val reportProducts = listOf(
    ReportProduct(ReportStatus.Danger, "퀘이커", "오트밀 라이트", R.drawable.img_scanner_report_danger,
        listOf("밀" to "알레르기 유발", "우유" to "비건(Vegan) 부적합")),
    ReportProduct(ReportStatus.Safe, "켈로그", "유기농 콘푸로스트", R.drawable.img_scanner_report_safe,
        listOf("고단백" to null, "저지방" to null)),
    ReportProduct(ReportStatus.Danger, "퀘이커", "오트밀 라이트", R.drawable.img_scanner_report_danger,
        listOf("밀" to "알레르기 유발", "우유" to "비건(Vegan) 부적합")),
)

@Composable
fun ScannerAnalysisReportScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    onProductClick: () -> Unit = {},
    onSecondaryActionClick: () -> Unit = {},
) {
    BackHandler(onBack = onBackClick)
    val pagerState = rememberPagerState(pageCount = { reportProducts.size })
    val current = reportProducts[pagerState.currentPage]
    Box(modifier.fillMaxSize().background(JjikmukTheme.colors.background).systemBarsPadding()) {
        ReportTopBar(onBackClick)
        ReportHeading(Modifier.padding(start = 23.dp, top = 85.dp))
        ReportSummary(Modifier.padding(start = 23.dp, top = 157.dp))
        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(280.dp),
            contentPadding = PaddingValues(horizontal = 47.dp),
            pageSpacing = 20.dp,
            modifier = Modifier.fillMaxWidth().height(372.dp).offset(y = 252.dp),
        ) { page ->
            ReportProductCard(reportProducts[page], onProductClick, Modifier.padding(top = 48.dp))
        }
        PageDots(
            reportProducts.size,
            pagerState.currentPage,
            Modifier.align(Alignment.TopCenter).padding(top = 642.dp),
        )
        ReportActions(
            if (current.status == ReportStatus.Safe) "챗봇에게 질문하기" else "대체 상품 추천받기",
            onBackClick,
            onSecondaryActionClick,
            Modifier.align(Alignment.BottomCenter).padding(bottom = 36.dp),
        )
    }
}

@Composable
private fun ReportTopBar(onBackClick: () -> Unit) {
    Box(Modifier.fillMaxWidth().height(62.dp).border(1.dp, JjikmukTheme.colors.borderSubtle).padding(22.dp, 12.dp)) {
        Image(
            painterResource(R.drawable.ic_scanner_report_back), "뒤로가기",
            Modifier.align(Alignment.CenterStart).size(30.dp).clickable(role = Role.Button, onClick = onBackClick),
        )
        Text("분석 레포트", color = JjikmukTheme.colors.textPrimary, style = JjikmukTheme.typography.labelL,
            modifier = Modifier.align(Alignment.Center))
    }
}

@Composable
private fun ReportHeading(modifier: Modifier) {
    Text(
        buildAnnotatedString {
            append("총 ")
            withStyle(SpanStyle(color = JjikmukTheme.colors.brand)) { append("3개") }
            append("의 상품 중\n추천 상품은 ")
            withStyle(SpanStyle(color = JjikmukTheme.colors.brand)) { append("1개") }
            append(" 입니다.")
        },
        color = JjikmukTheme.colors.textPrimary,
        style = JjikmukTheme.typography.h2,
        modifier = modifier,
    )
}

@Composable
private fun ReportSummary(modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SummaryCard("추천 상품", "1건", R.drawable.ic_scanner_report_summary_safe, Color(0xFFE6F4E4))
        SummaryCard("주의 상품", "2건", R.drawable.ic_scanner_report_summary_warning, Color(0xFFFEF2F2))
    }
}

@Composable
private fun SummaryCard(label: String, count: String, @DrawableRes icon: Int, iconBackground: Color) {
    Row(
        Modifier.shadow(10.dp, RoundedCornerShape(16.dp), ambientColor = Color(0x0A000000))
            .width(157.5.dp).height(66.5.dp).clip(RoundedCornerShape(16.dp))
            .background(Color.White).padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).background(iconBackground), contentAlignment = Alignment.Center) {
            Image(painterResource(icon), null, Modifier.size(20.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(label, color = JjikmukTheme.colors.textSecondary, style = JjikmukTheme.typography.labelS)
            Text(count, color = JjikmukTheme.colors.textPrimary, style = JjikmukTheme.typography.labelL)
        }
    }
}

@Composable
private fun ReportProductCard(product: ReportProduct, onProductClick: () -> Unit, modifier: Modifier) {
    val safe = product.status == ReportStatus.Safe
    Box(
        modifier.width(280.dp).height(324.dp).border(
            2.dp, if (safe) Color(0x3316A635) else Color(0x33FB2C36), RoundedCornerShape(32.dp)
        )
    ) {
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp).padding(top = 64.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(product.brand, color = JjikmukTheme.colors.textSecondary, style = JjikmukTheme.typography.labelS)
            Row(
                Modifier.clickable(role = Role.Button, onClick = onProductClick),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(product.name, color = JjikmukTheme.colors.textPrimary, style = JjikmukTheme.typography.h2)
                Image(painterResource(R.drawable.ic_scanner_report_zoom), "상품 상세보기", Modifier.size(24.dp))
            }
            Spacer(Modifier.height(24.dp))
            DetailPanel(product)
        }
        // 배지와 그림자 영역 뒤에서는 카드 상단선 자체가 이어지지 않도록
        // 이미지보다 넓은 불투명 영역으로 상단 테두리를 끊습니다.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-5).dp)
                .zIndex(1f)
                .width(128.dp)
                .height(12.dp)
                .background(Color.White),
        )
        ProductBadge(
            product,
            Modifier
                .align(Alignment.TopCenter)
                .offset(y = (-48).dp)
                .zIndex(2f),
        )
    }
}

@Composable
private fun ProductBadge(product: ReportProduct, modifier: Modifier) {
    val safe = product.status == ReportStatus.Safe
    Box(modifier.width(112.dp).height(106.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .shadow(20.dp, CircleShape)
                .size(96.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(
                    4.dp,
                    if (safe) Color(0x8016A635) else Color(0x80FB2C36),
                    CircleShape,
                )
                .padding(8.dp),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(product.image),
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape),
            )
        }
        Row(
            Modifier.align(Alignment.BottomCenter).height(25.dp).clip(CircleShape)
                .background(if (safe) JjikmukTheme.colors.brandStrong else Color(0xFFE7000B))
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Image(
                painterResource(if (safe) R.drawable.ic_scanner_report_safe_check else R.drawable.ic_scanner_report_x),
                null, Modifier.size(12.dp),
            )
            Text(if (safe) "SAFE" else "DANGER", color = Color.White, style = JjikmukTheme.typography.labelS)
        }
    }
}

@Composable
private fun DetailPanel(product: ReportProduct) {
    val safe = product.status == ReportStatus.Safe
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
            .background(if (safe) JjikmukTheme.colors.brandSubtlest else JjikmukTheme.colors.warning)
            .border(.5.dp, if (safe) JjikmukTheme.colors.brandSubtle else JjikmukTheme.colors.error,
                RoundedCornerShape(16.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Image(
                painterResource(if (safe) R.drawable.ic_scanner_report_safe_leaf else R.drawable.ic_scanner_report_danger_icon),
                null, Modifier.size(16.dp),
            )
            Text(
                if (safe) "프로필 기반 안심 제품" else "주의가 필요한 항목 " + product.details.size + "개",
                color = if (safe) JjikmukTheme.colors.brandStrong else Color(0xFFE7000B),
                style = JjikmukTheme.typography.labelS,
            )
        }
        product.details.forEach { detail ->
            Row(
                Modifier.shadow(2.dp, RoundedCornerShape(12.dp)).fillMaxWidth().height(42.dp)
                    .clip(RoundedCornerShape(12.dp)).background(Color.White).padding(horizontal = 11.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(detail.first, color = JjikmukTheme.colors.textPrimary, style = JjikmukTheme.typography.labelS)
                detail.second?.let { reason ->
                    Spacer(Modifier.weight(1f))
                    Text(
                        reason, color = JjikmukTheme.colors.error, style = JjikmukTheme.typography.labelS,
                        modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(JjikmukTheme.colors.warning)
                            .padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PageDots(count: Int, selected: Int, modifier: Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { page ->
            Box(
                Modifier.size(10.dp).clip(CircleShape).then(
                    if (page == selected) Modifier.background(JjikmukTheme.colors.textSecondary)
                    else Modifier.border(1.dp, JjikmukTheme.colors.textSecondary, CircleShape)
                )
            )
        }
    }
}

@Composable
private fun ReportActions(label: String, onCancel: () -> Unit, onAction: () -> Unit, modifier: Modifier) {
    Row(modifier.width(342.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ReportButton("취소", onCancel, Modifier.width(100.dp), false)
        ReportButton(label, onAction, Modifier.weight(1f), true)
    }
}

@Composable
private fun ReportButton(label: String, onClick: () -> Unit, modifier: Modifier, primary: Boolean) {
    Button(
        onClick, modifier.height(47.dp), shape = RoundedCornerShape(17.dp),
        border = if (primary) null else androidx.compose.foundation.BorderStroke(1.dp, JjikmukTheme.colors.border),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (primary) JjikmukTheme.colors.brandStrong else Color.White,
            contentColor = if (primary) Color.White else JjikmukTheme.colors.textPrimary,
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
    ) { Text(label, style = JjikmukTheme.typography.labelM, maxLines = 1) }
}
