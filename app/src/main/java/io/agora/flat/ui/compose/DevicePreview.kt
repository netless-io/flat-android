package io.agora.flat.ui.compose

import android.Manifest
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Card
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberPermissionState
import io.agora.flat.R
import io.agora.flat.ui.theme.Blue_0
import io.agora.flat.ui.theme.Gray_8
import io.agora.flat.ui.theme.isDarkTheme

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun DevicePreviewLayout(
    cameraOn: Boolean,
    onCameraChanged: (Boolean) -> Unit,
    micOn: Boolean,
    onMicChanged: (Boolean) -> Unit,
    avatar: String?
) {
    val cameraIcon = if (cameraOn) R.drawable.ic_class_room_camera_on else R.drawable.ic_class_room_camera_off
    val cameraColor = if (cameraOn) MaterialTheme.colors.primary else MaterialTheme.colors.error
    val micIcon = if (micOn) R.drawable.ic_class_room_mic_on else R.drawable.ic_class_room_mic_off
    val micColor = if (micOn) MaterialTheme.colors.primary else MaterialTheme.colors.error
    val bgColor = if (isDarkTheme()) Gray_8 else Blue_0

    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    val recordPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)

    Card(
        Modifier
            .padding(horizontal = 48.dp)
            .aspectRatio(1f)
            .clip(MaterialTheme.shapes.large)
    ) {
        if (cameraOn) {
            SimpleCameraPreview(Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                FlatAvatar(avatar, 64.dp)
            }
        }
    }
    Row {
        IconButton({
            if (cameraOn || cameraPermissionState.hasPermission) {
                onCameraChanged(!cameraOn)
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        }) {
            Icon(
                painterResource(cameraIcon),
                null,
                tint = cameraColor
            )
        }
        Spacer(Modifier.width(8.dp))
        IconButton({
            if (micOn || recordPermissionState.hasPermission) {
                onMicChanged(!micOn)
            } else {
                recordPermissionState.launchPermissionRequest()
            }
        }) {
            Icon(
                painterResource(micIcon),
                null,
                tint = micColor
            )
        }
    }
}


@Composable
fun SimpleCameraPreview(modifier: Modifier) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val context = LocalContext.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(Unit) {
        onDispose {
            cameraProviderFuture.get()?.unbindAll()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val executor = ContextCompat.getMainExecutor(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                bindPreview(lifecycleOwner, previewView, cameraProvider)
            }, executor)
            previewView
        },
        // update = { previewView ->
        //     val cameraProvider = cameraProviderFuture.get()
        //     cameraProvider.unbindAll()
        //     if (previewOn) bindPreview(lifecycleOwner, previewView, cameraProvider)
        // },
        modifier = modifier,
    )
}


fun bindPreview(
    lifecycleOwner: LifecycleOwner,
    previewView: PreviewView,
    cameraProvider: ProcessCameraProvider
) {
    val preview: Preview = Preview.Builder().build().also {
        it.setSurfaceProvider(previewView.surfaceProvider)
    }

    val cameraSelector: CameraSelector = CameraSelector.Builder()
        .requireLensFacing(CameraSelector.LENS_FACING_FRONT)
        .build()

    cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview)
}