package io.agora.flat.ui.compose

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.agora.flat.R
import io.agora.flat.ui.theme.FlatTheme

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun StreamCollectDialog(onOpen: () -> Unit, onClose: () -> Unit, onCancel: () -> Unit) {
    val openDialog = remember { mutableStateOf(true) }

    if (openDialog.value) {
        FlatTheme {
            Dialog(
                onDismissRequest = {
                    onCancel()
                    openDialog.value = false
                },
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable {
                            onCancel()
                            openDialog.value = false
                        },
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OperationItem(stringResource(R.string.setting_on)) {
                                onOpen()
                                openDialog.value = false
                            }
                            OperationItem(stringResource(R.string.setting_off)) {
                                onClose()
                                openDialog.value = false
                            }
                            OperationItem(stringResource(R.string.cancel)) {
                                onCancel()
                                openDialog.value = false
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OperationItem(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        FlatTextBodyOne(text)
    }
}

@Composable
@Preview(locale = "zh")
fun StreamCollectDialogPreview() {
    StreamCollectDialog({}, {}, {})
}