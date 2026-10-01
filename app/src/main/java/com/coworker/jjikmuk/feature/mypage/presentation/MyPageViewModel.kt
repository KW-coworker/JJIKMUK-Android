package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.LikedProduct
import com.coworker.jjikmuk.domain.repository.LikedProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class MyPageViewModel @Inject constructor(
    likedProductRepository: LikedProductRepository,
) : ViewModel() {

    val likedProducts: StateFlow<List<LikedProduct>> =
        likedProductRepository.observeLikedProducts()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )
}
