package com.coworker.jjikmuk.feature.scanner.presentation

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.CameraState
import androidx.camera.view.CameraController
import androidx.camera.view.LifecycleCameraController
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.Observer
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.coworker.jjikmuk.ui.component.JjikmukBackButton
import com.coworker.jjikmuk.ui.component.JjikmukPrimaryButton
import com.coworker.jjikmuk.ui.component.JjikmukSecondaryButton
import com.coworker.jjikmuk.ui.theme.JjikmukTheme
import java.io.File

@SuppressLint("MissingPermission") // Binding/capture are gated by the current runtime permission.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerMainRoute(
    onBackClick: () -> Unit,
    onCompareListClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScannerCaptureViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val state by viewModel.state.collectAsStateWithLifecycle()
    val analysisState by viewModel.analysisState.collectAsStateWithLifecycle()
    val analyzing = analysisState is ScannerAnalysisState.Running
    var flashOn by rememberSaveable { mutableStateOf(false) }
    var hasFlash by remember { mutableStateOf(false) }
    var mode by rememberSaveable { mutableStateOf(ScannerMode.Normal) }
    var showCompareList by rememberSaveable { mutableStateOf(false) }
    var permissionGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
    var cameraAttempt by remember { mutableIntStateOf(0) }
    var cameraReady by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val controller = remember(context, cameraAttempt) {
        LifecycleCameraController(context).apply {
            setEnabledUseCases(CameraController.IMAGE_CAPTURE)
            imageCaptureMode = ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> permissionGranted = granted }
    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::selectGalleryPhoto) }
    val openGallery: () -> Unit = {
        if (!state.busy && !analyzing) {
            try {
                galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } catch (_: Exception) {
                viewModel.showError("사진 선택기를 열 수 없어요. 다시 시도해 주세요.")
            }
        }
    }

    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = context.hasCameraPermission()
            }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    LaunchedEffect(Unit) {
        if (!permissionGranted && !permissionRequested) {
            permissionRequested = true
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Surface(modifier = modifier.fillMaxSize()) {
        if (state.photo != null) {
            ScannerPhotoReview(
                photo = checkNotNull(state.photo).file,
                busy = state.busy || analyzing,
                analysisState = analysisState,
                canAnalyze = viewModel.canAnalyze,
                onAnalyze = viewModel::analyze,
                onRetake = viewModel::retake,
                onGallery = openGallery,
            )
        } else {
            ScannerScreen(
                mode = mode,
                onModeChange = { if (!state.busy) mode = it },
                onBackClick = {
                    viewModel.finishSession()
                    onBackClick()
                },
                onFlashClick = { if (hasFlash && cameraReady && !state.busy) flashOn = !flashOn },
                flashEnabled = hasFlash && cameraReady && !state.busy,
                flashOn = flashOn && hasFlash,
                onGalleryClick = openGallery,
                galleryEnabled = !state.busy,
                shutterEnabled = permissionGranted && cameraReady && !state.busy && cameraError == null,
                onShutterClick = {
                    if (context.hasCameraPermission() && cameraReady) {
                        viewModel.beginCapture()?.let { file ->
                            try {
                                controller.imageCaptureFlashMode = if (flashOn && hasFlash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
                                controller.takePicture(
                                    ImageCapture.OutputFileOptions.Builder(file).build(),
                                    ContextCompat.getMainExecutor(context),
                                    object : ImageCapture.OnImageSavedCallback {
                                        override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                            viewModel.captureSaved(file)
                                        }

                                        override fun onError(exception: ImageCaptureException) {
                                            viewModel.captureFailed(file)
                                        }
                                    },
                                )
                            } catch (_: Exception) {
                                viewModel.captureFailed(file)
                            }
                        }
                    } else {
                        permissionGranted = context.hasCameraPermission()
                    }
                },
                onCompareListClick = { if (!state.busy) showCompareList = true },
                cameraContent = {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        if (permissionGranted) {
                            ScannerCameraPreview(
                                controller = controller,
                                onReady = { cameraReady = it },
                                onFlashAvailable = { hasFlash = it },
                                onError = { cameraError = "카메라를 사용할 수 없어요. 다른 앱에서 사용 중인지 확인해 주세요." },
                            )
                        }
                        when {
                            !permissionGranted -> ScannerCameraMessage(
                                message = "상품을 촬영하려면 카메라 권한이 필요해요.\n아래 갤러리 버튼으로 사진을 선택할 수도 있어요.",
                                action = "카메라 권한 설정",
                                onAction = {
                                    val activity = context.findActivity()
                                    if (permissionRequested && activity != null &&
                                        !ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.CAMERA)
                                    ) {
                                        try {
                                            context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                                "package:${context.packageName}".toUri()))
                                        } catch (_: Exception) {
                                            viewModel.showError("앱 설정에서 카메라 권한을 허용해 주세요.")
                                        }
                                    } else {
                                        permissionRequested = true
                                        permissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                            )
                            cameraError != null -> ScannerCameraMessage(
                                message = checkNotNull(cameraError),
                                action = "다시 연결",
                                onAction = {
                                    cameraError = null
                                    cameraReady = false
                                    cameraAttempt++
                                },
                            )
                            !cameraReady || state.busy -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                        }
                    }
                },
            )
        }
    }

    state.error?.let { message ->
        AlertDialog(
            onDismissRequest = viewModel::dismissError,
            title = { Text("사진 확인") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = viewModel::dismissError) { Text("확인") } },
        )
    }
    if (showCompareList) {
        ScannerCompareListBottomSheet(
            productCount = 0, // Photos are not identified products; no fabricated comparison results.
            onDismissRequest = { showCompareList = false },
            onRemoveProduct = {},
            onCompareClick = onCompareListClick,
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ScannerCameraPreview(
    controller: LifecycleCameraController,
    onReady: (Boolean) -> Unit,
    onFlashAvailable: (Boolean) -> Unit,
    onError: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val preview = remember(context, controller) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    AndroidView(factory = { preview }, modifier = Modifier.fillMaxSize())
    DisposableEffect(controller, lifecycleOwner, preview) {
        var disposed = false
        var cameraStates: androidx.lifecycle.LiveData<CameraState>? = null
        val cameraObserver = Observer<CameraState> { state ->
            if (state.error != null) {
                onReady(false)
                onError()
            }
        }
        val streamObserver = Observer<PreviewView.StreamState> {
            onReady(it == PreviewView.StreamState.STREAMING)
        }
        preview.previewStreamState.observe(lifecycleOwner, streamObserver)
        try {
            preview.controller = controller
            controller.bindToLifecycle(lifecycleOwner)
            controller.initializationFuture.addListener({
                if (!disposed) {
                    try {
                        controller.initializationFuture.get()
                        if (!controller.hasCamera(controller.cameraSelector)) onError()
                        onFlashAvailable(controller.cameraInfo?.hasFlashUnit() == true)
                        cameraStates = controller.cameraInfo?.cameraState
                        cameraStates?.observe(lifecycleOwner, cameraObserver)
                    } catch (_: Exception) {
                        onError()
                    }
                }
            }, ContextCompat.getMainExecutor(context))
        } catch (_: Exception) {
            onError()
        }
        onDispose {
            disposed = true
            preview.previewStreamState.removeObserver(streamObserver)
            cameraStates?.removeObserver(cameraObserver)
            preview.controller = null
            controller.unbind()
            onReady(false)
            onFlashAvailable(false)
        }
    }
}

@Composable
private fun ScannerCameraMessage(message: String, action: String, onAction: () -> Unit) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(message, color = Color.White, textAlign = TextAlign.Center,
                style = JjikmukTheme.typography.bodyM)
            JjikmukPrimaryButton(text = action, onClick = onAction)
        }
    }
}

@Composable
private fun ScannerPhotoReview(
    photo: File,
    busy: Boolean,
    analysisState: ScannerAnalysisState,
    canAnalyze: Boolean,
    onAnalyze: () -> Unit,
    onRetake: () -> Unit,
    onGallery: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler { if (!busy) onRetake() }
    BoxWithConstraints(
        modifier.fillMaxSize().background(JjikmukTheme.colors.background)
            .systemBarsPadding().padding(22.dp),
    ) {
        val photoHeight = (maxHeight - 315.dp).coerceAtLeast(160.dp)
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            JjikmukBackButton(onClick = { if (!busy) onRetake() })
            Text("사진 확인", style = JjikmukTheme.typography.labelL)
            Box(Modifier.height(photoHeight).fillMaxWidth(), contentAlignment = Alignment.Center) {
                var imageFailed by remember(photo) { mutableStateOf(false) }
                AsyncImage(
                    model = photo,
                    contentDescription = "선택한 상품 사진",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    onError = { imageFailed = true },
                )
                if (imageFailed) Text("사진을 표시할 수 없어요. 다른 사진을 선택해 주세요.")
                if (busy) CircularProgressIndicator()
            }
            Text(scannerAnalysisMessage(analysisState, canAnalyze), style = JjikmukTheme.typography.bodyM,
                color = JjikmukTheme.colors.textSecondary)
            JjikmukPrimaryButton(text = "분석하기", onClick = onAnalyze, enabled = canAnalyze && !busy)
            JjikmukSecondaryButton(text = "다시 촬영", onClick = onRetake, enabled = !busy)
            JjikmukSecondaryButton(text = "다른 사진 선택", onClick = onGallery, enabled = !busy)
        }
    }
}

private fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
