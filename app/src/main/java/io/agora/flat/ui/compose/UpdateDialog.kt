package io.agora.flat.ui.compose

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.agora.flat.R
import io.agora.flat.common.version.VersionCheckResult
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.util.installApk
import kotlinx.coroutines.launch

@Composable
internal fun UpdateDialog(
    versionCheckResult: VersionCheckResult,
    downloadApp: suspend () -> Uri,
    onCancel: () -> Unit,
    onGotoMarket: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var downloading by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            scope.launch {
                context.installApk(downloadApp())
            }
        }
    }

    FlatTheme {
        Dialog(onDismissRequest = if (versionCheckResult.forceUpdate) fun() {} else onCancel) {
            Surface(shape = Shapes.large) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    FlatTextTitle(versionCheckResult.title)
                    FlatNormalVerticalSpacer()
                    FlatTextBodyOneSecondary(versionCheckResult.description)
                    FlatNormalVerticalSpacer()

                    if (downloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                            color = MaterialTheme.colors.primary,
                        )
                    } else {
                        Column {
                            FlatPrimaryTextButton(text = stringResource(R.string.update)) {
                                scope.launch {
                                    if (versionCheckResult.gotoMarket) {
                                        onGotoMarket()
                                        return@launch
                                    }
                                    downloading = true
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                        val haveInstallPermission = context.packageManager.canRequestPackageInstalls()
                                        if (!haveInstallPermission) {
                                            showPermissionDialog = true
                                            downloading = false
                                            return@launch
                                        }
                                    }
                                    context.installApk(downloadApp())
                                    downloading = false
                                }
                            }
                            if (!versionCheckResult.forceUpdate) {
                                FlatSmallVerticalSpacer()
                                FlatSecondaryTextButton(
                                    text = stringResource(id = R.string.cancel),
                                    onClick = { onCancel() }
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showPermissionDialog) {
            InstallPermissionDialog(
                onLaunch = {
                    showPermissionDialog = false
                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    launcher.launch(intent)
                },
                onCancel = {
                    showPermissionDialog = false
                }
            )
        }
    }
}

@Composable
fun InstallPermissionDialog(onLaunch: () -> Unit, onCancel: () -> Unit) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { FlatTextTitle(text = stringResource(R.string.app_name)) },
        text = { FlatTextBodyOne(text = stringResource(R.string.install_permission_description)) },
        confirmButton = {
            TextButton(onClick = onLaunch) {
                Text(text = stringResource(R.string.settings))
            }
        },
        dismissButton = {
            TextButton(onClick = onCancel) {
                Text(text = stringResource(R.string.cancel))
            }
        },
    )
}

@Composable
@Preview
@Preview(device = Devices.PIXEL_C)
private fun InstallPermissionDialogPreview() {
    FlatTheme {
        InstallPermissionDialog(
            onLaunch = {},
            onCancel = {},
        )
    }
}

@Composable
@Preview
@Preview(device = Devices.PIXEL_C)
private fun UpdateDialogPreview() {
    FlatTheme {
        UpdateDialog(
            VersionCheckResult(),
            downloadApp = { Uri.EMPTY },
            {}
        )
    }
}