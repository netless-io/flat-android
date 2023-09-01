package io.agora.flat.ui.activity.phone

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.FlatNetException
import io.agora.flat.common.Navigator
import io.agora.flat.common.android.CallingCodeManager
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatLargeHorizontalSpacer
import io.agora.flat.ui.compose.FlatNormalVerticalSpacer
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatSmallPrimaryTextButton
import io.agora.flat.ui.compose.FlatSmallSecondaryTextButton
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextTitle
import io.agora.flat.ui.compose.PhoneInput
import io.agora.flat.ui.compose.SendCodeInput
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.isValidPhone
import io.agora.flat.util.isValidSmsCode
import io.agora.flat.util.showToast

@AndroidEntryPoint
class PhoneBindActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatPage {
                PhoneBindScreen(
                    onBindSuccess = {
                        when (intent?.getStringExtra(Constants.IntentKey.FROM)) {
                            Constants.From.Login -> {
                                Navigator.launchHomeActivity(this@PhoneBindActivity)
                                finish()
                            }

                            Constants.From.UserSecurity -> {
                                this@PhoneBindActivity.showToast(R.string.bind_success)
                                finish()
                            }
                        }
                    },
                    onBindClose = {
                        when (intent?.getStringExtra(Constants.IntentKey.FROM)) {
                            Constants.From.Login -> {
                                Navigator.launchLoginActivity(this@PhoneBindActivity)
                                finish()
                            }

                            Constants.From.UserSecurity -> {
                                finish()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun PhoneBindDialog(
    onBindSuccess: () -> Unit,
    onDismissRequest: () -> Unit,
) {
    FlatTheme {
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(
                Modifier
                    .widthIn(max = 400.dp)
                    .height(500.dp),
                shape = Shapes.large,
            ) {
                PhoneBindScreen(
                    onBindSuccess = onBindSuccess,
                    onBindClose = onDismissRequest,
                )
            }
        }
    }
}

@Composable
fun PhoneBindScreen(
    onBindSuccess: () -> Unit,
    onBindClose: () -> Unit,
    viewModel: PhoneBindViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val showMergeAccount = (state.message?.exception as? FlatNetException)?.code == FlatErrorCode.Web.SMSAlreadyBinding

    var lastPhone by rememberSaveable { mutableStateOf("") }
    var lastCCode by remember { mutableStateOf("") }

    BackHandler {
        if (state.merging) {
            viewModel.setMerging(false)
        } else {
            onBindClose()
        }
    }

    LaunchedEffect(state) {
        if (state.success) {
            onBindSuccess()
        }
    }

    LaunchedEffect(state) {
        if (state.codeSuccess) {
            context.showToast(R.string.message_code_send_success)
            viewModel.clearCodeSuccess()
        }
    }

    if (showMergeAccount) {
        MergeAccountDialog(
            onConfirm = {
                state.message?.run { viewModel.clearUiMessage(id) }
                viewModel.setMerging(true)
            },
            onCancel = {
                state.message?.run { viewModel.clearUiMessage(id) }
            }
        )
    } else {
        ShowUiMessageEffect(uiMessage = state.message, onMessageShown = { id ->
            viewModel.clearUiMessage(id)
        })
    }

    PhoneBindScreen(
        state = state,
        onBindClose = onBindClose,
        onSendCode = { ccode, phone ->
            lastCCode = ccode
            lastPhone = phone

            viewModel.sendCode("$ccode$phone")
        },
        onConfirm = viewModel::confirmBind,
    )
}

@Composable
fun MergeAccountDialog(onConfirm: () -> Unit, onCancel: () -> Unit) {
    FlatTheme {
        Dialog(onDismissRequest = onCancel) {
            Surface(
                Modifier.widthIn(max = 400.dp),
                shape = Shapes.large,
            ) {
                Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                    FlatTextTitle(stringResource(R.string.merge_account))
                    FlatNormalVerticalSpacer()
                    FlatTextBodyOne(stringResource(R.string.merge_account_message))
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
internal fun PhoneBindScreen(
    state: PhoneBindUiViewState,
    onBindClose: () -> Unit = {},
    onSendCode: (String, String) -> Unit,
    onConfirm: (String, String) -> Unit = { _, _ -> },
) {
    var phone by remember { mutableStateOf("") }
    var ccode by remember { mutableStateOf(CallingCodeManager.getDefaultCC()) }
    var code by remember { mutableStateOf("") }
    val buttonEnable = phone.isValidPhone() && code.isValidSmsCode() && !state.loading

    val title = stringResource(if (state.merging) R.string.merge_account else R.string.bind_phone)

    Column {
        CloseTopAppBar(title, onClose = onBindClose)
        Spacer(Modifier.height(16.dp))

        Column(Modifier.padding(horizontal = 16.dp)) {
            PhoneInput(
                phone,
                onPhoneChange = { phone = it },
                callingCode = ccode,
                onCallingCodeChange = { ccode = it },
            )

            SendCodeInput(
                code,
                onCodeChange = { code = it },
                onSendCode = {
                    onSendCode(ccode, phone)
                },
                ready = phone.isValidPhone(),
                remainTime = state.remainTime,
            )
        }

        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.confirm),
                enabled = buttonEnable,
                onClick = { onConfirm("$ccode$phone", code) },
            )
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun PhoneBindScreenPreview() {
    FlatPage {
        PhoneBindScreen(state = PhoneBindUiViewState.Empty, {}, { _, _ -> }, { _, _ -> })
    }
}