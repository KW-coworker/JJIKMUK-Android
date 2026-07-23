package com.coworker.jjikmuk.ui.component

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.ui.theme.JjikmukTheme

enum class MainTab(
    val label: String,
    @DrawableRes val iconRes: Int,
) {
    Home("Home", R.drawable.ic_home),
    Diet("Diet", R.drawable.ic_diet),
    Product("Product", R.drawable.ic_product),
    History("History", R.drawable.ic_history),
    My("MY", R.drawable.ic_my),
}

@Composable
fun JjikmukBottomNavigationBar(
    selectedTab: MainTab,
    onTabClick: (MainTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = JjikmukTheme.colors.surface,
        shadowElevation = 8.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            MainTab.entries.forEach { tab ->
                JjikmukBottomNavigationItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabClick(tab) },
                )
            }
        }
    }
}

@Composable
private fun JjikmukBottomNavigationItem(
    tab: MainTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val itemColor = if (selected) JjikmukTheme.colors.brandStrong else JjikmukTheme.colors.textSecondary

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Icon(
            painter = painterResource(tab.iconRes),
            contentDescription = tab.label,
            tint = itemColor,
            modifier = Modifier.size(24.dp),
        )
        Text(
            text = tab.label,
            color = itemColor,
            style = MaterialTheme.typography.labelSmall,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun JjikmukBottomNavigationBarPreview() {
    JjikmukTheme {
        JjikmukBottomNavigationBar(
            selectedTab = MainTab.Home,
            onTabClick = {},
        )
    }
}
