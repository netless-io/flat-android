package io.agora.flat.ui.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.ui.theme.FlatTitleTextStyle
import io.agora.flat.ui.theme.Shapes

@Composable
fun GlobalAgreementDialog(onAgree: () -> Unit, onRefuse: () -> Unit) {
    var recheck by remember { mutableStateOf(false) }
    if (recheck) {
        Dialog(onDismissRequest = {}) {
            Surface(shape = Shapes.large) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(stringResource(R.string.login_agreement_dialog_title), style = FlatTitleTextStyle)
                    FlatNormalVerticalSpacer()
                    GlobalRecheckAgreementMessage()
                    FlatNormalVerticalSpacer()
                    Column {
                        FlatPrimaryTextButton(stringResource(R.string.agreement_global_agree)) { onAgree() }
                        FlatSmallVerticalSpacer()
                        FlatSecondaryTextButton(stringResource(R.string.agreement_global_disagree_and_exit)) { onRefuse() }
                    }
                }
            }
        }
    } else {
        Dialog(onDismissRequest = {}) {
            Surface(shape = Shapes.large) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    Text(stringResource(R.string.login_agreement_dialog_title), style = FlatTitleTextStyle)
                    FlatNormalVerticalSpacer()
                    GlobalAgreementMessage()
                    FlatNormalVerticalSpacer()
                    Column {
                        FlatPrimaryTextButton(stringResource(R.string.agreement_global_agree)) { onAgree() }
                        FlatSmallVerticalSpacer()
                        FlatSecondaryTextButton(stringResource(R.string.agreement_global_disagree)) {
                            recheck = true
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun AgreementDialog(onAgree: () -> Unit, onRefuse: () -> Unit) {
    Dialog(onDismissRequest = onRefuse) {
        Surface(shape = Shapes.large) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                FlatTextTitle(stringResource(R.string.login_agreement_dialog_title))
                FlatNormalVerticalSpacer()
                LoginAgreementDialogMessage()
                FlatNormalVerticalSpacer()
                Row {
                    FlatSmallSecondaryTextButton(stringResource(R.string.refuse), Modifier.weight(1f)) { onRefuse() }
                    FlatLargeHorizontalSpacer()
                    FlatSmallPrimaryTextButton(stringResource(R.string.agree), Modifier.weight(1f)) { onAgree() }
                }
            }
        }
    }
}


@Composable
private fun LoginAgreementDialogMessage() {
    val text = stringResource(R.string.login_agreement_dialog_message)
    val items = listOf(
        ClickableItem(stringResource(R.string.agreement_global_privacy), "privacy", Constants.URL.Privacy),
        ClickableItem(stringResource(R.string.agreement_global_service), "service", Constants.URL.Service)
    )

    FlatClickableText(text = text, items = items)
}

@Composable
private fun GlobalAgreementMessage() {
    val text = stringResource(R.string.agreement_global_dialog_message)
    val items = listOf(
        ClickableItem(stringResource(R.string.agreement_global_privacy), "privacy", Constants.URL.Privacy),
        ClickableItem(stringResource(R.string.agreement_global_service), "service", Constants.URL.Service)
    )

    FlatClickableText(text = text, items = items)
}

@Composable
private fun GlobalRecheckAgreementMessage() {
    val text = stringResource(R.string.agreement_global_recheck_dialog_message)
    val items = listOf(
        ClickableItem(stringResource(R.string.agreement_global_privacy), "privacy", Constants.URL.Privacy),
        ClickableItem(stringResource(R.string.agreement_global_service), "service", Constants.URL.Service)
    )

    FlatClickableText(text = text, items = items)
}