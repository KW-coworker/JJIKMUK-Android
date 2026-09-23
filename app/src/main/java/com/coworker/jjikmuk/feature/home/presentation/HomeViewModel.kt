package com.coworker.jjikmuk.feature.home.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.repository.FamilyProfileRepository
import com.coworker.jjikmuk.ui.component.ScanTargetMemberUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val familyProfileRepository: FamilyProfileRepository,
) : ViewModel() {

    private val selectedProfileIds = MutableStateFlow<Set<String>?>(null)

    val scanTargetMembers: StateFlow<List<ScanTargetMemberUiModel>> =
        familyProfileRepository.observeProfiles()
            .combine(selectedProfileIds) { profiles, selectedIds ->
                val nextSelectedIds = selectedIds.syncWithProfiles(profiles)
                if (nextSelectedIds != selectedIds) {
                    selectedProfileIds.value = nextSelectedIds
                }
                profiles.map { profile ->
                    profile.toScanTargetMemberUiModel(
                        isSelected = profile.id in nextSelectedIds,
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList(),
            )

    init {
        viewModelScope.launch {
            familyProfileRepository.ensureDefaultProfiles()
        }
    }

    fun updateScanTargetSelection(
        memberId: String,
        checked: Boolean,
    ) {
        selectedProfileIds.update { current ->
            val currentSelectedIds = current.orEmpty()
            if (checked) currentSelectedIds + memberId else currentSelectedIds - memberId
        }
    }

    private fun Set<String>?.syncWithProfiles(
        profiles: List<FamilyProfile>,
    ): Set<String> {
        val profileIds = profiles.map { profile -> profile.id }.toSet()
        if (this == null) return profileIds

        val existingSelectedIds = this.intersect(profileIds)
        val newProfileIds = profileIds - this
        return existingSelectedIds + newProfileIds
    }

    private fun FamilyProfile.toScanTargetMemberUiModel(
        isSelected: Boolean,
    ): ScanTargetMemberUiModel {
        return ScanTargetMemberUiModel(
            id = id,
            name = name,
            relation = if (isMe) "나" else relation,
            emoji = emoji,
            isSelected = isSelected,
            vegetarian = vegetarian,
            allergies = allergies,
            preferences = preferences,
        )
    }
}
