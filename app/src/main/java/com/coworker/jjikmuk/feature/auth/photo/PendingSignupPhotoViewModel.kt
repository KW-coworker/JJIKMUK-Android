package com.coworker.jjikmuk.feature.auth.photo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.auth.domain.*
import com.coworker.jjikmuk.feature.auth.presentation.common.userMessage
import com.coworker.jjikmuk.feature.auth.session.AuthSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class PendingPhotoState(val pending: ProfilePhoto? = null, val server: ProfilePhoto? = null,
    val busy: Boolean = false, val loaded: Boolean = false, val mustReload: Boolean = false,
    val uploaded: Boolean = false, val done: Boolean = false, val message: String? = null) {
    val canUpload get() = pending != null && loaded && !busy && !mustReload && !uploaded
}

/** Offers only the signed-in email's draft. Never silently replaces a photo saved on another device. */
@HiltViewModel
class PendingSignupPhotoViewModel @Inject constructor(
    private val store: PendingSignupPhotoStore, private val photos: AuthPhotos, private val sessions: AuthSessionManager,
) : ViewModel() {
    private val mutable = MutableStateFlow(PendingPhotoState())
    val state = mutable.asStateFlow()
    private var owner: Long? = null
    private var email: String? = null
    private var record: PendingSignupPhoto? = null
    init {
        viewModelScope.launch { sessions.state.collect {
            if (owner != null && owner != it.generation) { owner = null; email = null; record = null; mutable.value = PendingPhotoState() }
        } }
    }
    private fun current(generation: Long) = owner == generation && sessions.state.value.generation == generation &&
        sessions.state.value.credentials != null

    fun open() {
        val snapshot = sessions.state.value
        if (owner == snapshot.generation || !snapshot.canEnterHome) return
        val credentials = snapshot.credentials ?: return
        owner = snapshot.generation; email = credentials.email; record = null
        mutable.value = PendingPhotoState(busy = true)
        viewModelScope.launch {
            try {
                val pending = store.read(credentials.email)
                if (!current(snapshot.generation)) return@launch
                if (pending == null || (pending.userId != null && pending.userId != credentials.userId)) {
                    mutable.value = PendingPhotoState(done = true); return@launch
                }
                record = pending
                mutable.value = PendingPhotoState(pending = pending.photo, mustReload = pending.sending)
                reload()
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (current(snapshot.generation)) mutable.value = PendingPhotoState(message = "임시 사진을 읽지 못했어요. 나중에 다시 확인할 수 있어요.") }
        }
    }

    fun reload() {
        val generation = owner ?: return
        val pending = record ?: return
        val address = email ?: return
        if (state.value.busy || !current(generation)) return
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                when (val result = photos.load(generation)) {
                    is AuthResult.Success -> {
                        if (!current(generation)) return@launch
                        val updated = PendingSignupPhoto(pending.id, pending.userId, false, pending.photo)
                        store.update(address, updated)
                        if (!current(generation)) return@launch
                        record = updated
                        mutable.update { it.copy(server = result.value, busy = false, loaded = true, mustReload = false) }
                    }
                    is AuthResult.Failure -> if (current(generation)) mutable.update { it.copy(busy = false,
                        mustReload = true, message = result.error.userMessage()) }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (current(generation)) mutable.update { it.copy(busy = false, mustReload = true,
                message = "사진 확인 상태를 저장하지 못했어요. 다시 확인해 주세요.") } }
        }
    }

    fun upload() {
        val generation = owner ?: return
        val pending = record ?: return
        val address = email ?: return
        if (!state.value.canUpload || !current(generation)) return
        val userId = sessions.state.value.credentials!!.userId
        mutable.update { it.copy(busy = true, message = null) }
        viewModelScope.launch {
            try {
                // Persist before dispatch; an interrupted upload is read back on the next login.
                val sending = PendingSignupPhoto(pending.id, userId, true, pending.photo)
                store.update(address, sending)
                if (!current(generation)) return@launch
                record = sending
                when (val result = photos.upload(pending.photo, generation)) {
                    is AuthResult.Success -> if (current(generation)) {
                        mutable.update { it.copy(busy = false, server = result.value, uploaded = true) }
                        discard()
                    }
                    is AuthResult.Failure -> if (current(generation)) mutable.update { it.copy(busy = false,
                        mustReload = true, message = "사진 저장 결과를 다시 확인해 주세요. " + result.error.userMessage()) }
                }
            } catch (cancelled: CancellationException) {
                if (current(generation)) mutable.update { it.copy(busy = false, mustReload = true) }
                throw cancelled
            } catch (_: Exception) { if (current(generation)) mutable.update { it.copy(busy = false, mustReload = true,
                message = "사진 보관 상태를 확인하지 못했어요. 다시 확인한 후 저장해 주세요.") } }
        }
    }

    /** Keep the server photo, remove only this matching local draft. Also retries local cleanup after success. */
    fun discard() {
        val generation = owner ?: return
        val pending = record ?: return
        val address = email ?: return
        if (state.value.busy || !current(generation)) return
        mutable.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                store.remove(address, pending.id)
                if (current(generation)) { record = null; mutable.value = PendingPhotoState(done = true) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (current(generation)) mutable.update { it.copy(busy = false,
                message = "기기의 임시 사진을 정리하지 못했어요. 다시 정리하거나 나중에 확인해 주세요.") } }
        }
    }
    fun later() { if (!state.value.busy) mutable.update { it.copy(pending = null, server = null, done = true) } }
}
