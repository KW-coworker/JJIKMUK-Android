package com.coworker.jjikmuk.feature.scanner.presentation

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.coworker.jjikmuk.feature.scanner.data.PhotoTooLargeException
import com.coworker.jjikmuk.feature.scanner.data.ScannerPhotoSession
import com.coworker.jjikmuk.feature.scanner.data.ScannerImagePreparer
import com.coworker.jjikmuk.feature.scanner.domain.PreparedScannerPhoto
import com.coworker.jjikmuk.feature.scanner.domain.ScannerOcrService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ScannerCaptureState(
    val photo: PreparedScannerPhoto? = null,
    val busy: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class ScannerCaptureViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val imagePreparer: ScannerImagePreparer,
    ocrService: ScannerOcrService,
) : ViewModel() {
    private val analysis = ScannerAnalysisController(viewModelScope, ocrService)
    val analysisState = analysis.state
    val canAnalyze get() = analysis.available
    private val session = ScannerPhotoSession(context.cacheDir)
    private val mutableState = MutableStateFlow(ScannerCaptureState())
    val state = mutableState.asStateFlow()
    private var pendingCapture: File? = null
    private var closed = false

    fun beginCapture(): File? {
        if (closed || state.value.busy || analysisState.value is ScannerAnalysisState.Running) return null
        return try {
            session.createFile().also {
                pendingCapture = it
                mutableState.update { state -> state.copy(busy = true, error = null) }
            }
        } catch (_: Exception) {
            showError("사진을 저장할 공간을 확인한 뒤 다시 시도해 주세요.")
            null
        }
    }

    fun captureSaved(file: File) {
        if (closed || pendingCapture != file) {
            session.discard(file)
            return
        }
        pendingCapture = null
        loadPhoto { file }
    }

    fun captureFailed(file: File) {
        session.discard(file)
        if (!closed && pendingCapture == file) {
            pendingCapture = null
            mutableState.update { it.copy(busy = false, error = "촬영하지 못했어요. 다시 시도해 주세요.") }
        }
    }

    fun selectGalleryPhoto(uri: Uri) {
        if (closed || state.value.busy || analysisState.value is ScannerAnalysisState.Running) return
        mutableState.update { it.copy(busy = true, error = null) }
        loadPhoto {
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Image stream unavailable")
            input.use(session::copyFrom)
        }
    }

    private fun loadPhoto(create: () -> File) {
        viewModelScope.launch {
            var source: File? = null
            var output: File? = null
            var prepared: PreparedScannerPhoto? = null
            var accepted = false
            try {
                withContext(Dispatchers.IO) {
                    val file = create()
                    source = file
                    output = session.createFile()
                    prepared = imagePreparer.prepare(file, checkNotNull(output))
                }
                val photo = checkNotNull(prepared)
                if (!closed && session.select(photo.file)) {
                    accepted = true
                    analysis.reset()
                    mutableState.value = ScannerCaptureState(photo = photo)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: PhotoTooLargeException) {
                showError("50MiB 이하의 사진을 선택해 주세요.")
            } catch (_: Exception) {
                showError("사진을 읽을 수 없어요. 다른 사진을 선택하거나 다시 촬영해 주세요.")
            } finally {
                source?.let(session::discard)
                if (!accepted) output?.let(session::discard)
                if (!closed) mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun retake() {
        if (state.value.busy) return
        analysis.reset()
        session.clearSelection()
        mutableState.value = ScannerCaptureState()
    }

    fun showError(message: String) {
        if (!closed) mutableState.update { it.copy(error = message) }
    }

    fun dismissError() {
        mutableState.update { it.copy(error = null) }
    }

    fun analyze() {
        if (closed || state.value.busy) return
        state.value.photo?.let(analysis::analyze)
    }

    fun finishSession() {
        closed = true
        analysis.reset()
        session.close()
    }

    override fun onCleared() = finishSession()
}
