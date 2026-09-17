package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukBottomNavigationBar
import com.coworker.jjikmuk.ui.component.JjikmukBackButton
import com.coworker.jjikmuk.ui.component.MainTab
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

@Composable
fun MyPageScreen(
    selectedTab: MainTab = MainTab.My,
    onTabClick: (MainTab) -> Unit = {},
    onBackClick: () -> Unit = {},
    onFamilyDietSettingClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            MyPageTopBar(
                onBackClick = onBackClick,
            )
        },
        bottomBar = {
            JjikmukBottomNavigationBar(
                selectedTab = selectedTab,
                onTabClick = onTabClick,
            )
        },
        containerColor = JjikmukTheme.colors.surfaceSecondary,
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            MyPageProfileSection()
            MyPageFamilyShareSection(
                onSettingClick = onFamilyDietSettingClick,
            )
            MyPageProductSection(
                title = "찜한 상품",
                products = likedProducts,
            )
            MyPageProductSection(
                title = "최근 본 상품",
                products = recentProducts,
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MyPageTopBar(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding(),
        color = JjikmukTheme.colors.surface,
        shadowElevation = 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .border(
                    width = 1.dp,
                    color = JjikmukTheme.colors.borderSubtle,
                ),
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 22.dp),
            ) {
                JjikmukBackButton(onClick = onBackClick)
            }
            Text(
                text = "마이 페이지",
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.labelL,
                modifier = Modifier.align(Alignment.Center),
            )
            Icon(
                painter = painterResource(R.drawable.ic_settings),
                contentDescription = "설정",
                tint = Color.Unspecified,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 30.dp)
                    .size(24.dp),
            )
        }
    }
}

@Composable
private fun MyPageProfileSection(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(234.dp)
            .background(JjikmukTheme.colors.surface),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(31.dp))
        Box(
            modifier = Modifier.size(108.dp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(JjikmukTheme.colors.textTertiary)
                    .border(
                        width = 3.dp,
                        color = JjikmukTheme.colors.surface,
                        shape = CircleShape,
                    ),
            )
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 3.dp, bottom = 14.dp)
                    .size(32.dp),
                color = JjikmukTheme.colors.textPrimary,
                shape = CircleShape,
                border = androidx.compose.foundation.BorderStroke(
                    width = 2.dp,
                    color = JjikmukTheme.colors.surface,
                ),
                shadowElevation = 4.dp,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(R.drawable.ic_profile_edit),
                        contentDescription = "프로필 수정",
                        tint = JjikmukTheme.colors.surface,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "코워커",
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.h1,
        )
        Text(
            text = "coworker@example.com",
            color = JjikmukTheme.colors.textTertiary,
            style = JjikmukTheme.typography.bodyL,
        )
    }
}

@Composable
private fun MyPageFamilyShareSection(
    onSettingClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(153.dp)
            .background(JjikmukTheme.colors.surface)
            .padding(start = 27.dp, top = 26.dp, end = 22.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_family_share),
                        contentDescription = null,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "가족 식이조건 공유",
                        color = JjikmukTheme.colors.textPrimary,
                        style = JjikmukTheme.typography.titleL,
                    )
                }
                Text(
                    text = "식품 스캔 시 가족의 식이조건도 함께 관리해요",
                    color = JjikmukTheme.colors.textSecondary,
                    style = JjikmukTheme.typography.bodyS,
                )
            }
            MyPageSmallButton(
                text = "설정",
                onClick = onSettingClick,
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MyPageFamilyAvatars()
            Text(
                text = "나 외 2명 참여 중",
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelM,
            )
        }
    }
}

@Composable
private fun MyPageSmallButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .size(width = 46.dp, height = 32.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
        color = JjikmukTheme.colors.info,
        shape = RoundedCornerShape(8.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text,
                color = JjikmukTheme.colors.edit,
                style = JjikmukTheme.typography.labelS,
            )
        }
    }
}

@Composable
private fun MyPageFamilyAvatars(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(92.dp)
            .height(36.dp),
    ) {
        FamilyAvatar(
            backgroundColor = Color(0xFFE5E7EB),
            modifier = Modifier.align(Alignment.CenterStart),
        )
        FamilyAvatar(
            emoji = "👨🏻",
            backgroundColor = Color(0xFFDBEAFE),
            modifier = Modifier.padding(start = 28.dp),
        )
        FamilyAvatar(
            emoji = "👶🏻",
            backgroundColor = Color(0xFFFEF9C2),
            modifier = Modifier.padding(start = 56.dp),
        )
    }
}

@Composable
private fun FamilyAvatar(
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    emoji: String? = null,
) {
    Surface(
        modifier = modifier.size(36.dp),
        color = backgroundColor,
        shape = CircleShape,
        border = androidx.compose.foundation.BorderStroke(
            width = 2.dp,
            color = JjikmukTheme.colors.surface,
        ),
        shadowElevation = 2.dp,
    ) {
        if (emoji != null) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = emoji,
                    style = JjikmukTheme.typography.bodyL,
                )
            }
        }
    }
}

@Composable
private fun MyPageProductSection(
    title: String,
    products: List<MyPageProductUiModel>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(JjikmukTheme.colors.surface)
            .padding(top = 22.dp, bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                color = JjikmukTheme.colors.textPrimary,
                style = JjikmukTheme.typography.titleL,
            )
            Text(
                text = "전체보기 ›",
                color = JjikmukTheme.colors.textSecondary,
                style = JjikmukTheme.typography.labelS,
            )
        }
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            products.forEach { product ->
                MyPageProductCard(product = product)
            }
        }
    }
}

@Composable
private fun MyPageProductCard(
    product: MyPageProductUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.width(112.dp),
    ) {
        Image(
            painter = painterResource(product.imageResId),
            contentDescription = product.name,
            modifier = Modifier
                .size(112.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFFF1F2F4)),
            contentScale = ContentScale.Crop,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.brand,
            color = JjikmukTheme.colors.textTertiary,
            style = JjikmukTheme.typography.caption,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = product.name,
            color = JjikmukTheme.colors.textPrimary,
            style = JjikmukTheme.typography.bodyS,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private data class MyPageProductUiModel(
    val brand: String,
    val name: String,
    @DrawableRes val imageResId: Int,
)

private val likedProducts = listOf(
    MyPageProductUiModel(
        brand = "아이얌",
        name = "글루텐프리 쌀과자",
        imageResId = R.drawable.img_gluten_free_rice_cookie,
    ),
    MyPageProductUiModel(
        brand = "널담",
        name = "비건 초코 쿠키",
        imageResId = R.drawable.img_vegan_choco_cookie,
    ),
    MyPageProductUiModel(
        brand = "매일유업",
        name = "무첨가 두유 99.9",
        imageResId = R.drawable.img_soy_milk,
    ),
)

private val recentProducts = listOf(
    MyPageProductUiModel(
        brand = "농심",
        name = "새우깡",
        imageResId = R.drawable.img_shrimp_snack,
    ),
    MyPageProductUiModel(
        brand = "오리온",
        name = "포키 블루베리",
        imageResId = R.drawable.img_pocky_blueberry,
    ),
    MyPageProductUiModel(
        brand = "해태",
        name = "허니버터칩",
        imageResId = R.drawable.img_honey_butter_chip,
    ),
)

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun MyPageScreenPreview() {
    JjikmukTheme {
        MyPageScreen()
    }
}
