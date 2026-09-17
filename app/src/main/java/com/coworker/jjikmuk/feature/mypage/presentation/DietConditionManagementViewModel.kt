package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.model.FamilyProfile
import com.coworker.jjikmuk.domain.repository.FamilyProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class DietConditionManagementViewModel @Inject constructor(
    private val familyProfileRepository: FamilyProfileRepository,
) : ViewModel() {

    val profiles: StateFlow<List<FamilyProfile>> =
        familyProfileRepository.observeProfiles()
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

    fun updateProfile(
        profileId: String,
        name: String,
        emoji: String,
        relation: String,
    ) {
        viewModelScope.launch {
            familyProfileRepository.updateProfile(
                profileId = profileId,
                name = name,
                emoji = emoji,
                relation = relation,
            )
        }
    }

    fun updateDietCondition(
        profileId: String,
        vegetarian: String,
        allergies: Set<String>,
        preferences: Set<String>,
    ) {
        viewModelScope.launch {
            familyProfileRepository.updateDietCondition(
                profileId = profileId,
                vegetarian = vegetarian,
                allergies = allergies,
                preferences = preferences,
            )
        }
    }

    fun deleteProfile(profileId: String) {
        viewModelScope.launch {
            familyProfileRepository.deleteProfile(profileId)
        }
    }
}
