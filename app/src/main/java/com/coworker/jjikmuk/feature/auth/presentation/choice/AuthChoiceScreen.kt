package com.coworker.jjikmuk.feature.auth.presentation.choice

import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.ScaleFactor
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import kotlinx.coroutines.delay

@Composable
fun AuthChoiceScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(JjikmukTheme.colors.surface),
    ) {
        AuthBannerSlideshow(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .height(BANNER_HEIGHT),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .offset(y = 464.dp),
        ) {
            Image(
                painter = painterResource(R.drawable.auth_logo),
                contentDescription = null,
                modifier = Modifier.size(width = 74.dp, height = 73.dp),
            )
            androidx.compose.material3.Text(
                text = stringResource(R.string.app_name),
                color = JjikmukTheme.colors.brand,
                style = JjikmukTheme.typography.section,
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp),
        ) {
            JjikmukPrimaryButton(
                text = stringResource(R.string.auth_login),
                onClick = onLoginClick,
            )
            JjikmukSecondaryButton(
                text = stringResource(R.string.auth_sign_up),
                onClick = onSignUpClick,
            )
        }

        Spacer(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(JjikmukTheme.colors.background),
        )
        Spacer(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
                .background(JjikmukTheme.colors.disabled),
        )
    }
}

@Composable
private fun AuthBannerSlideshow(
    modifier: Modifier = Modifier,
) {
    var bannerIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(BANNER_DISPLAY_MILLIS)
            bannerIndex = (bannerIndex + 1) % AUTH_BANNERS.size
        }
    }

    Box(modifier = modifier) {
        Crossfade(
            targetState = AUTH_BANNERS[bannerIndex],
            animationSpec = tween(BANNER_FADE_MILLIS),
            label = "authBannerCrossfade",
        ) { banner ->
            AuthBannerImage(
                banner = banner,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(BANNER_GRADIENT_HEIGHT)
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to JjikmukTheme.colors.surface.copy(alpha = 0f),
                            0.45f to JjikmukTheme.colors.surface.copy(alpha = 0.35f),
                            0.72f to JjikmukTheme.colors.surface.copy(alpha = 0.85f),
                            1f to JjikmukTheme.colors.surface,
                        ),
                    ),
                ),
        )
    }
}

@Composable
private fun AuthBannerImage(
    banner: AuthBanner,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(banner.imageRes),
        contentDescription = null,
        contentScale = banner.contentScale,
        alignment = banner.alignment,
        modifier = modifier,
    )
}

private val AUTH_BANNERS = listOf(
    AuthBanner(
        imageRes = R.drawable.auth_banner_1,
        contentScale = BannerOneContentScale,
        alignment = BiasAlignment(
            horizontalBias = BANNER_ONE_HORIZONTAL_BIAS,
            verticalBias = 1f,
        ),
    ),
    AuthBanner(
        imageRes = R.drawable.auth_banner_2,
        contentScale = ContentScale.FillWidth,
        alignment = Alignment.TopCenter,
    ),
    AuthBanner(
        imageRes = R.drawable.auth_banner_3,
        contentScale = ContentScale.FillHeight,
        alignment = Alignment.TopStart,
    ),
)

private data class AuthBanner(
    @DrawableRes val imageRes: Int,
    val contentScale: ContentScale,
    val alignment: Alignment,
)

private object BannerOneContentScale : ContentScale {
    override fun computeScaleFactor(
        srcSize: Size,
        dstSize: Size,
    ): ScaleFactor {
        val scale = dstSize.width * BANNER_ONE_WIDTH_SCALE / srcSize.width
        return ScaleFactor(scale, scale)
    }
}

private val BANNER_HEIGHT = 504.dp
private val BANNER_GRADIENT_HEIGHT = 112.dp
private const val BANNER_DISPLAY_MILLIS = 3_000L
private const val BANNER_FADE_MILLIS = 600
private const val BANNER_ONE_WIDTH_SCALE = 2.7573f
private const val BANNER_ONE_HORIZONTAL_BIAS = 0.357f

@Preview(showBackground = true, widthDp = 375, heightDp = 812)
@Composable
private fun AuthChoiceScreenPreview() {
    JjikmukTheme {
        AuthChoiceScreen(
            onLoginClick = {},
            onSignUpClick = {},
        )
    }
}
