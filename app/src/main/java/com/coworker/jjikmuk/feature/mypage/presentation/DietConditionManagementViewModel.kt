package com.coworker.jjikmuk.feature.mypage.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@HiltViewModel
class DietConditionManagementViewModel @Inject constructor(private val familyProfileRepository: FamilyProfileRepository) : ViewModel() {
    val state = familyProfileRepository.observeState().stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), FamilyProfilesState(loaded = false))
    val profiles = state.map { it.profiles }.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000, replayExpirationMillis = 0), emptyList())
    fun refresh() {
        val account = familyProfileRepository.captureAccount()
        viewModelScope.launch { familyProfileRepository.refresh(account) }
    }
    fun addProfile(name: String, emoji: String, relation: String, account: FamilyAccount? = state.value.account, onSuccess: () -> Unit = {}) {
        viewModelScope.launch { if (familyProfileRepository.addProfile(name, emoji, relation, account)) onSuccess() }
    }
    fun updateProfile(profileId: String, name: String, emoji: String, relation: String,
        version: Long? = null, account: FamilyAccount? = state.value.account, onSuccess: () -> Unit = {}) {
        viewModelScope.launch { if (familyProfileRepository.updateProfile(profileId, name, emoji, relation, version, account)) onSuccess() }
    }
    fun updateDietCondition(profileId: String, vegetarian: String, allergies: Set<String>, preferences: Set<String>,
        version: Long? = null, account: FamilyAccount? = state.value.account) {
        viewModelScope.launch { familyProfileRepository.updateDietCondition(profileId, vegetarian, allergies, preferences, version, account) }
    }
    fun deleteProfile(profileId: String, version: Long? = null, account: FamilyAccount? = state.value.account, onSuccess: () -> Unit = {}) {
        viewModelScope.launch { if (familyProfileRepository.deleteProfile(profileId, version, account)) onSuccess() }
    }
}
