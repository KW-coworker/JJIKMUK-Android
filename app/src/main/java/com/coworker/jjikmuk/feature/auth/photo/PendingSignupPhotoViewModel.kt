package com.coworker.jjikmuk.feature.auth.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingPhotoState(val busy: Boolean = false, val done: Boolean = false,
    val message: String? = null)

/** Sync the confirmed signup draft without blocking Home or replacing an existing server photo. */
@HiltViewModel
class PendingSignupPhotoViewModel @Inject constructor(
    private val store: PendingSignupPhotoStore, private val photos: AuthPhotos, private val sessions: AuthSessionManager,
) : ViewModel() {
    private val mutable = MutableStateFlow(PendingPhotoState())
    val state = mutable.asStateFlow()
    private var owner: Long? = null
    private var job: Job? = null
    private var uploadedDraft: String? = null

    init {
        viewModelScope.launch { sessions.state.collect {
            if (owner != null && (owner != it.generation || it.credentials == null)) {
                job?.cancel(); owner = null; uploadedDraft = null; mutable.value = PendingPhotoState()
            }
        } }
    }
    private fun current(generation: Long) = owner == generation && sessions.state.value.generation == generation &&
        sessions.state.value.canEnterHome

    fun open() {
        val snapshot = sessions.state.value
        if (owner == snapshot.generation || !snapshot.canEnterHome) return
        job?.cancel(); owner = snapshot.generation; uploadedDraft = null
        sync(explicitRetry = false)
    }

    fun retry() { if (!state.value.busy && !state.value.done) sync(explicitRetry = true) }

    private fun sync(explicitRetry: Boolean) {
        val generation = owner ?: return
        val credentials = sessions.state.value.credentials ?: return
        if (!current(generation)) return
        mutable.value = PendingPhotoState(busy = true)
        job = viewModelScope.launch {
            fun failed() {
                if (current(generation)) mutable.value = PendingPhotoState(
                    message = "가입 사진을 저장하지 못했어요. 다시 시도해 주세요.")
            }
            try {
                val pending = store.read(credentials.email)
                if (!current(generation)) return@launch
                // A draft must be bound to the successfully created account, never just its email.
                if (pending == null || pending.userId != credentials.userId) {
                    mutable.value = PendingPhotoState(done = true); return@launch
                }
                if (uploadedDraft != pending.id) {
                    val existing = photos.load(generation)
                    if (!current(generation)) return@launch
                    if (existing is AuthResult.Failure) { failed(); return@launch }
                    if ((existing as AuthResult.Success).value == null) {
                        // An interrupted request may still be completing; do not replay it on login.
                        if (pending.sending && !explicitRetry) { failed(); return@launch }
                        store.update(credentials.email, PendingSignupPhoto(pending.id, credentials.userId, true, pending.photo))
                        if (!current(generation)) return@launch
                        val result = photos.upload(pending.photo, generation)
                        if (!current(generation)) return@launch
                        if (result is AuthResult.Failure) { failed(); return@launch }
                    }
                    uploadedDraft = pending.id // Existing server photo wins; cleanup can be retried alone.
                }
                store.remove(credentials.email, pending.id)
                if (current(generation)) mutable.value = PendingPhotoState(done = true)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { failed() }
        }
    }
}
