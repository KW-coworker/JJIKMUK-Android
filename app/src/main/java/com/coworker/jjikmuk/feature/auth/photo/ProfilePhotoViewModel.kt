package com.coworker.jjikmuk.feature.auth.photo

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

data class PhotoEditorState(val generation: Long = -1, val photo: ProfilePhoto? = null,
    val busy: Boolean = false, val canEdit: Boolean = false, val mustReload: Boolean = false, val message: String? = null)

@HiltViewModel
class ProfilePhotoViewModel @Inject constructor(
    private val photos: AuthPhotos,
    private val encoder: ProfilePhotoEncoder,
    private val sessions: AuthSessionManager,
) : ViewModel() {
    private val preparing = MutableStateFlow<Long?>(null)
    private val inputError = MutableStateFlow<Pair<Long, String>?>(null)
    private var selectionGeneration: Long? = null
    val state = combine(photos.state, sessions.state, preparing, inputError) { photo, session, prep, error ->
        val same = photo.generation == session.generation && session.credentials != null
        val busy = (same && photo.busy) || prep == session.generation
        PhotoEditorState(session.generation, photo.photo.takeIf { same }, busy,
            session.canEnterHome && !busy && !(same && photo.mustReload), same && photo.mustReload,
            error?.takeIf { it.first == session.generation }?.second ?: if (same) {
                if (photo.mustReload) "사진 저장 결과를 확인해야 해요. 최신 사진을 확인해 주세요"
                else photo.failure?.userMessage()
            } else null)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, PhotoEditorState())

    fun load() {
        if (sessions.state.value.credentials == null || state.value.busy) return
        inputError.value = null
        viewModelScope.launch { photos.load() }
    }

    fun beginSelection(): Boolean {
        if (!state.value.canEdit) return false
        selectionGeneration = sessions.state.value.generation
        return true
    }

    fun selected(uri: Uri?) {
        val generation = selectionGeneration ?: return
        selectionGeneration = null
        if (uri == null || !current(generation) || state.value.busy) return
        preparing.value = generation
        inputError.value = null
        viewModelScope.launch {
            try {
                val photo = encoder.encode(uri)
                ensureActive()
                if (current(generation)) photos.upload(photo)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (current(generation)) inputError.value = generation to "사진을 읽을 수 없어요. 다른 사진을 선택해 주세요"
            } finally { if (preparing.value == generation) preparing.value = null }
        }
    }

    fun delete() {
        if (!state.value.canEdit || state.value.photo == null) return
        inputError.value = null
        viewModelScope.launch { photos.delete() }
    }

    private fun current(generation: Long) = sessions.state.value.generation == generation && sessions.state.value.canEnterHome
}
