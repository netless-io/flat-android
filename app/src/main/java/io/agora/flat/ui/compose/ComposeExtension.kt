package io.agora.flat.ui.compose

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import io.agora.flat.util.ContentInfo
import io.agora.flat.util.contentInfo
import io.agora.flat.util.hasPermission
import io.agora.flat.util.showToast

@Composable
fun LifecycleHandler(
    onCreate: () -> Unit = {},
    onStart: () -> Unit = {},
    onResume: () -> Unit = {},
    onPause: () -> Unit = {},
    onStop: () -> Unit = {},
    onDestroy: () -> Unit = {},
) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val lifecycleObserver = object : DefaultLifecycleObserver {
            override fun onCreate(owner: LifecycleOwner) {
                onCreate()
            }

            override fun onStart(owner: LifecycleOwner) {
                onStart()
            }

            override fun onResume(owner: LifecycleOwner) {
                onResume()
            }

            override fun onPause(owner: LifecycleOwner) {
                onPause()
            }

            override fun onStop(owner: LifecycleOwner) {
                onStop()
            }

            override fun onDestroy(owner: LifecycleOwner) {
                onDestroy()
            }
        }
        lifecycle.addObserver(lifecycleObserver)
        onDispose {
            lifecycle.removeObserver(lifecycleObserver)
        }
    }
}

@Composable
fun launcherPickContent(
    onPickContent: (ContentInfo) -> Unit,
): (String) -> Unit {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(context.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE))
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.also {
            val info = context.contentInfo(it) ?: return@rememberLauncherForActivityResult
            onPickContent(info)
        }
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            hasPermission = true
        } else {
            context.showToast("Permission Not Granted")
        }
    }
    val launcherCheckPermission: (String) -> Unit = {
        if (hasPermission) {
            launcher.launch(it)
        } else {
            permissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }
    return launcherCheckPermission
}
