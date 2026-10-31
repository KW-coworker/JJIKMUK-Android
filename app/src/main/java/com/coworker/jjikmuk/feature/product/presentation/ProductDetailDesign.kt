package com.coworker.jjikmuk.feature.product.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import coil3.compose.SubcomposeAsyncImage
import com.coworker.jjikmuk.R
import com.coworker.jjikmuk.domain.model.*
import com.coworker.jjikmuk.data.repository.ProductCapacity
import com.coworker.jjikmuk.data.repository.ProductAlternative
import com.coworker.jjikmuk.feature.product.presentation.component.*
import com.coworker.jjikmuk.ui.theme.*

@Composable
internal fun ProductDetailDesign(
    state: ProductDetailUiState,
    isLiked: Boolean,
    onFavorite: () -> Unit,
    onCapacity: (String) -> Unit,
    onAsk: (String) -> Unit,
    onAlternative: (String?) -> Unit,
    onMoreAlternatives: () -> Unit,
    onRetryAlternatives: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val product = state.product ?: return
    Column(modifier.fillMaxSize().background(JjikmukTheme.colors.surface)
        .verticalScroll(rememberScrollState())) {
        Box(Modifier.fillMaxWidth().height(343.dp)
            .border(0.5.dp, JjikmukTheme.colors.borderSubtle), contentAlignment = Alignment.Center) {
            if (product.imageUrl.isNullOrBlank()) {
                Text("이미지 준비중", style = JjikmukTheme.typography.bodyM, color = JjikmukTheme.colors.textTertiary)
            } else {
                SubcomposeAsyncImage(model = product.imageUrl, contentDescription = product.productName,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 34.dp),
                    loading = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(Modifier.size(24.dp)) } },
                    error = { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("이미지 준비중", color = JjikmukTheme.colors.textTertiary) } })
            }
            // The current detail contract supplies one image; do not invent additional photos.
            if (!product.imageUrl.isNullOrBlank()) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
                .size(7.dp).background(JjikmukTheme.colors.textPrimary, CircleShape))
            Text(if (isLiked) "♥" else "♡", fontSize = 28.sp, lineHeight = 32.sp,
                color = if (isLiked) Color.Red else JjikmukTheme.colors.textSecondary,
                modifier = Modifier.align(Alignment.BottomEnd).padding(end = 20.dp, bottom = 14.dp)
                    .size(44.dp).wrapContentSize().clickable(onClick = onFavorite))
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 28.dp).padding(top = 30.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(35.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 81.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(Modifier.weight(1f).padding(start = 4.dp)) {
                    Text(product.brandName, style = JjikmukTheme.typography.h3,
                        color = JjikmukTheme.colors.textSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Spacer(Modifier.height(4.dp))
                    Text(product.productName, style = JjikmukTheme.typography.h1,
                        color = JjikmukTheme.colors.textPrimary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                Image(painterResource(when (product.safetyStatus) {
                    ProductSafetyStatus.Pass -> R.drawable.ic_face_pass
                    ProductSafetyStatus.Unknown -> R.drawable.ic_face_unknown
                    ProductSafetyStatus.Danger -> R.drawable.ic_face_danger
                }), contentDescription = when (product.safetyStatus) {
                    ProductSafetyStatus.Pass -> "등록 알레르기 충돌 없음"
                    ProductSafetyStatus.Unknown -> "사용자 적합성 확인 불가"
                    ProductSafetyStatus.Danger -> "사용자 맞춤 위험 성분 감지"
                }, modifier = Modifier.size(81.dp))
            }
            if (product.safetyStatus != ProductSafetyStatus.Pass) ProductDetailRiskWarningCard(product)
            CapacityPicker(state, onCapacity)
            Surface(onClick = { onAsk("${product.productName}의 성분과 알레르기 주의사항을 알려줘") },
                modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                color = JjikmukTheme.colors.surface, border = BorderStroke(0.672.dp, JjikmukTheme.colors.borderSubtle)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(13.dp)) {
                    Box(Modifier.size(42.dp).background(JjikmukTheme.colors.disabled, CircleShape), contentAlignment = Alignment.Center) {
                        Image(painterResource(R.drawable.ic_product_ask_chat), null, Modifier.size(21.dp, 20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text("이 상품 물어보기", style = JjikmukTheme.typography.titleM, color = JjikmukTheme.colors.textPrimary)
                        Spacer(Modifier.height(5.dp))
                        Text("성분부터 알레르기까지, AI에게 질문하세요", style = JjikmukTheme.typography.caption,
                            color = JjikmukTheme.colors.textSecondary)
                    }
                    Text("↗", fontSize = 20.sp, color = JjikmukTheme.colors.edit)
                }
            }
            ProductDetailNutritionCard(product)
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                ProductNutritionRows(product)
                Text("1일 영양성분 기준치에 대한 비율(%)은 2,000kcal 기준이므로 개인의 필요열량에 따라 다를 수 있습니다",
                    style = JjikmukTheme.typography.bodyS, color = JjikmukTheme.colors.textPrimary)
            }
            ProductRawMaterialsBox(product)
            if (product.safetyStatus == ProductSafetyStatus.Danger) AlternativeSection(
                state, onAlternative, onMoreAlternatives, onRetryAlternatives)
        }
    }
}

@Composable
private fun CapacityPicker(state: ProductDetailUiState, onSelect: (String) -> Unit) {
    val product = state.product ?: return
    var expanded by rememberSaveable(state.referenceBarcode) { mutableStateOf(false) }
    val selected = state.capacities.firstOrNull { it.key == state.selectedCapacityKey }
    val selectable = state.capacities.size > 1
    val maxAmount = state.capacities.filter { it.unit == selected?.unit }.mapNotNull { it.amount }.maxOrNull()
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(onClick = { if (selectable) expanded = !expanded }, enabled = selectable,
            modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
            color = JjikmukTheme.colors.surface,
            border = BorderStroke(0.672.dp, if (expanded) Color(0xFFB8EAB5) else Color(0xFFD9DFE8))) {
            Row(Modifier.padding(horizontal = 17.dp, vertical = 15.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                CapacityDescription(selected?.label ?: product.totalWeight?.takeIf(String::isNotBlank) ?: "중량 정보 없음",
                    product, selected?.amount, maxAmount, true, Modifier.weight(1f))
                if (state.capacitiesLoading) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                else if (selectable) Icon(painterResource(R.drawable.ic_chevron_right),
                    if (expanded) "중량 선택 접기" else "중량 선택 펼치기",
                    Modifier.size(18.dp,22.dp).graphicsLayer { rotationZ = if (expanded) -90f else 90f },
                    tint = JjikmukTheme.colors.textSecondary)
            }
        }
        if (expanded && selectable) Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            color = JjikmukTheme.colors.surface, border = BorderStroke(0.672.dp, Color(0xFFB8EAB5))) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                Text("같은 상품, 다른 용량 1개씩 비교해 보세요", Modifier.padding(10.dp),
                    style = JjikmukTheme.typography.caption, color = JjikmukTheme.colors.textSecondary)
                state.capacities.forEach { option ->
                    val active = option.key == state.selectedCapacityKey
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(9.dp))
                        .background(if (active) Color(0xFFEFFAF2) else JjikmukTheme.colors.surface)
                        .clickable { onSelect(option.key) }.padding(horizontal = 10.dp, vertical = 13.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CapacityDescription(option.label, option.product, option.amount, maxAmount, active, Modifier.weight(1f))
                        Box(Modifier.size(18.dp), contentAlignment = Alignment.Center) {
                            if (active) Text("✓", color = JjikmukTheme.colors.brand, fontSize = 18.sp)
                        }
                    }
                }
            }
        }
        if (state.capacityError != null) Text(state.capacityError, style = JjikmukTheme.typography.caption,
            color = JjikmukTheme.colors.textSecondary)
    }
}

@Composable
private fun CapacityDescription(label: String, product: ProductDetail, amount: Double?, maximum: Double?, active: Boolean,
    modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = if (active) JjikmukTheme.typography.titleM else JjikmukTheme.typography.bodyM,
            color = if (active) JjikmukTheme.colors.textPrimary else JjikmukTheme.colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color(0xFFF3F4F9))) {
            if (amount != null && maximum != null && maximum > 0) Box(Modifier.fillMaxHeight()
                .fillMaxWidth((amount / maximum).toFloat().coerceIn(0f,1f))
                .background(if (active) Brush.horizontalGradient(listOf(Color(0xFF009520), Color(0xFF009A98)))
                    else Brush.horizontalGradient(listOf(Color(0xFFB8EAB5), Color(0xFFB8EAB5)))))
        }
        Spacer(Modifier.height(6.dp))
        Text(product.nutrition.energyKcal?.let { "열량 약 ${"%.1f".format(java.util.Locale.ROOT,it)}kcal" } ?: "열량 정보 없음",
            style = JjikmukTheme.typography.caption, color = JjikmukTheme.colors.textSecondary)
    }
}

@Composable
private fun AlternativeSection(state: ProductDetailUiState, onProduct: (String?) -> Unit, onMore: () -> Unit, onRetry: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("대체 상품 추천", style = JjikmukTheme.typography.titleL, color = JjikmukTheme.colors.textPrimary)
            Text("전체보기 ›", Modifier.clickable(onClick = onMore), style = JjikmukTheme.typography.caption,
                color = JjikmukTheme.colors.textSecondary)
        }
        AlternativeStatus(state, onRetry)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(state.alternatives.take(6), key = { it.product.barcode.orEmpty() }) { item ->
                JjikmukProductCard(item.card(), Modifier.width(100.dp).clickable { onProduct(item.product.barcode) },
                    size = JjikmukProductCardSize.AlternativeCompact)
            }
        }
    }
}

@Composable
private fun AlternativeStatus(state: ProductDetailUiState, onRetry: () -> Unit) {
    when {
        state.alternativesLoading -> Box(Modifier.fillMaxWidth().height(60.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(Modifier.size(24.dp), color = JjikmukTheme.colors.brand)
        }
        state.alternativesError != null -> Text(state.alternativesError + " 다시 시도", Modifier.clickable(onClick = onRetry),
            style = JjikmukTheme.typography.bodyS, color = JjikmukTheme.colors.textSecondary)
        state.alternatives.isEmpty() -> Text("현재 조건에 맞는 대체 상품을 찾지 못했어요.",
            style = JjikmukTheme.typography.bodyS, color = JjikmukTheme.colors.textSecondary)
        state.alternatives.any { it.needsVerification } -> Text("표시 확인 필요 상품은 섭취 전 원재료를 확인해 주세요.",
            style = JjikmukTheme.typography.bodyS, color = JjikmukTheme.colors.textSecondary)
    }
}

@Composable
internal fun ProductAlternativeList(state: ProductDetailUiState, onProduct: (String?) -> Unit, onRetry: () -> Unit) {
    Column(Modifier.fillMaxSize().background(JjikmukTheme.colors.surface)) {
        Row(Modifier.fillMaxWidth().heightIn(min=53.dp).border(0.5.dp, JjikmukTheme.colors.borderSubtle)
            .padding(horizontal=20.dp, vertical=12.dp), verticalAlignment=Alignment.CenterVertically) {
            Text("‘${state.product?.productName.orEmpty()}’ ", style=JjikmukTheme.typography.labelM,
                color=JjikmukTheme.colors.textPrimary, maxLines=1, overflow=TextOverflow.Ellipsis, modifier=Modifier.weight(1f,false))
            Text("대체 상품 ${state.alternatives.size}개", style=JjikmukTheme.typography.labelM,
                color=JjikmukTheme.colors.brand)
        }
        AlternativeStatus(state,onRetry)
        LazyVerticalGrid(columns=GridCells.Fixed(2), contentPadding=PaddingValues(start=18.dp,end=18.dp,top=4.dp,bottom=20.dp),
            horizontalArrangement=Arrangement.spacedBy(13.dp), verticalArrangement=Arrangement.spacedBy(14.dp)) {
            items(state.alternatives,key={it.product.barcode.orEmpty()}) { item ->
                JjikmukProductCard(item.card(),Modifier.clickable { onProduct(item.product.barcode) },
                    size=JjikmukProductCardSize.FlexibleGrid, compactLabels=true)
            }
        }
    }
}
private fun ProductAlternative.card() = JjikmukProductCardUiModel(
    brand=product.brandName,name=product.productName,badge=if(needsVerification) "표시 확인 필요" else "프로필 기준",
    imageUrl=product.imageUrl,barcode=product.barcode)
