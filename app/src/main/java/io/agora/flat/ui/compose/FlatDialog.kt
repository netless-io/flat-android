package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.agora.flat.R
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Shapes

@Composable
fun UnbindNoticeDialog(onConfirm: () -> Unit, onCancel: () -> Unit, onDismiss: () -> Unit = onCancel) {
    FlatTheme {
        Dialog(onDismissRequest = onDismiss) {
            Surface(
                Modifier.widthIn(max = 400.dp),
                shape = Shapes.large,
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    FlatTextTitle(stringResource(R.string.check_unbind_title))
                    FlatNormalVerticalSpacer()
                    FlatTextBodyOne(stringResource(R.string.check_unbind_message))
                    FlatNormalVerticalSpacer()
                    Row {
                        FlatSmallSecondaryTextButton(
                            stringResource(R.string.cancel),
                            Modifier.weight(1f)
                        ) { onCancel() }
                        FlatLargeHorizontalSpacer()
                        FlatSmallPrimaryTextButton(
                            stringResource(R.string.confirm),
                            Modifier.weight(1f)
                        ) { onConfirm() }
                    }
                }
            }
        }
    }
}

@Composable
fun UnbindLimitDialog(onConfirm: () -> Unit) {
    FlatTheme {
        Dialog(onDismissRequest = onConfirm) {
            Surface(
                Modifier.widthIn(max = 400.dp),
                shape = Shapes.large,
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    FlatTextTitle(stringResource(R.string.check_unbind_title))
                    FlatNormalVerticalSpacer()
                    FlatTextBodyOne(stringResource(R.string.unbind_limit_message))
                    FlatNormalVerticalSpacer()
                    Row {
                        FlatSmallPrimaryTextButton(
                            stringResource(R.string.confirm),
                            Modifier.weight(1f)
                        ) { onConfirm() }
                    }
                }
            }
        }
    }
}

@Composable
fun FlatBaseDialog(title: String, message: String, onConfirm: () -> Unit, onCancel: (() -> Unit)? = null) {
    FlatTheme {
        Dialog(onDismissRequest = onConfirm) {
            Surface(
                Modifier.widthIn(max = 400.dp),
                shape = Shapes.large,
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    FlatTextTitle(title)
                    FlatNormalVerticalSpacer()
                    FlatTextBodyOne(message)
                    FlatNormalVerticalSpacer()
                    Row {
                        if (onCancel != null) {
                            FlatSmallSecondaryTextButton(
                                stringResource(R.string.cancel),
                                Modifier.weight(1f)
                            ) { onCancel() }
                            FlatLargeHorizontalSpacer()
                        }
                        FlatSmallPrimaryTextButton(
                            stringResource(R.string.confirm),
                            Modifier.weight(1f)
                        ) { onConfirm() }
                    }
                }
            }
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun UnbindNoticeDialogPreview() {
    Row {
        UnbindNoticeDialog(onConfirm = { }, onCancel = { })
        UnbindLimitDialog(onConfirm = { })
    }

}