package com.coworker.jjikmuk.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.repository.AnalysisTargetRepository
import com.coworker.jjikmuk.ui.component.toScanTargetMembers
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(private val targets: AnalysisTargetRepository) : ViewModel() {
    val scanTargetMembers = targets.state.map { it.toScanTargetMembers() }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), emptyList())
    fun updateScanTargetSelection(memberId: String, checked: Boolean) {
        val account = targets.captureAccount()
        viewModelScope.launch { targets.setSelected(memberId, checked, account) }
    }
}
