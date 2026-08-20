package com.coworker.jjikmuk.ui.component

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.FileProvider
import java.io.File

class JjikmukImageSourceLauncher internal constructor(
    private val openCameraAction: () -> Unit,
    private val openGalleryAction: () -> Unit,
) {
    fun openCamera() {
        openCameraAction()
    }

    fun openGallery() {
        openGalleryAction()
    }
}

@Composable
fun rememberJjikmukImageSourceLauncher(
    onCameraImageSelected: (Uri) -> Unit = {},
    onGalleryImageSelected: (Uri) -> Unit = {},
    onCameraFinished: () -> Unit = {},
    onGalleryFinished: () -> Unit = {},
): JjikmukImageSourceLauncher {
    val context = LocalContext.current
    val currentOnCameraImageSelected by rememberUpdatedState(onCameraImageSelected)
    val currentOnGalleryImageSelected by rememberUpdatedState(onGalleryImageSelected)
    val currentOnCameraFinished by rememberUpdatedState(onCameraFinished)
    val currentOnGalleryFinished by rememberUpdatedState(onGalleryFinished)
    var cameraImageUri by remember { mutableStateOf<Uri?>(null) }
    var cameraImageFile by remember { mutableStateOf<File?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
    ) { success ->
        val imageUri = cameraImageUri
        if (success && imageUri != null) {
            currentOnCameraImageSelected(imageUri)
        }
        cameraImageFile?.delete()
        cameraImageUri = null
        cameraImageFile = null
        currentOnCameraFinished()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { imageUri ->
        if (imageUri != null) {
            currentOnGalleryImageSelected(imageUri)
        }
        currentOnGalleryFinished()
    }

    return remember(context, cameraLauncher, galleryLauncher) {
        JjikmukImageSourceLauncher(
            openCameraAction = {
                val cameraImage = context.createCameraImage()
                cameraImageUri = cameraImage.uri
                cameraImageFile = cameraImage.file
                cameraLauncher.launch(cameraImage.uri)
            },
            openGalleryAction = {
                galleryLauncher.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                )
            },
        )
    }
}

private data class CameraImage(
    val uri: Uri,
    val file: File,
)

private fun Context.createCameraImage(): CameraImage {
    val imageDirectory = File(cacheDir, CAMERA_IMAGE_DIRECTORY_NAME).apply {
        mkdirs()
    }
    val imageFile = File.createTempFile(
        CAMERA_IMAGE_FILE_PREFIX,
        CAMERA_IMAGE_FILE_SUFFIX,
        imageDirectory,
    )

    return CameraImage(
        uri = FileProvider.getUriForFile(
            this,
            "$packageName.fileprovider",
            imageFile,
        ),
        file = imageFile,
    )
}

private const val CAMERA_IMAGE_DIRECTORY_NAME = "images"
private const val CAMERA_IMAGE_FILE_PREFIX = "jjikmuk_camera_"
private const val CAMERA_IMAGE_FILE_SUFFIX = ".jpg"
