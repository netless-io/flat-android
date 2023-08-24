package io.agora.flat.ui.activity.password

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.PhoneOrEmailInfo
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatTextCaption
import io.agora.flat.ui.compose.PasswordInput
import io.agora.flat.ui.compose.PhoneOrEmailInput
import io.agora.flat.ui.compose.SendCodeInput
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.isValidPassword
import io.agora.flat.util.isValidVerifyCode
import io.agora.flat.util.showToast

@AndroidEntryPoint
class PasswordResetActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                PasswordResetScreen(
                    onClose = { finish() },
                    onResetSuccess = {
                        Navigator.launchLoginActivity(this)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun PasswordResetScreen(
    onClose: () -> Unit,
    onResetSuccess: () -> Unit = {},
    viewModel: PasswordResetViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    ShowUiMessageEffect(uiMessage = viewState.message) { id ->
        viewModel.clearUiMessage(id)
    }

    LaunchedEffect(viewState) {
        if (viewState.success) {
            context.showToast(R.string.message_reset_success)
            onResetSuccess()
        }
    }

    PasswordResetScreen(
        uiState = viewState,
        onPhoneOrEmailChange = { viewModel.changePhoneOrEmailState(it) },
        onSendCode = { viewModel.sendCode() },
        onNext = { viewModel.nextStep() },
        onPrev = { viewModel.prevStep() },
        onConfirm = { viewModel.resetPassword() },
        onClose = onClose,
    )
}

@Composable
internal fun PasswordResetScreen(
    uiState: PasswordResetUiState,
    onPhoneOrEmailChange: (PhoneOrEmailInfo) -> Unit,
    onSendCode: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit,
    onConfirm: () -> Unit,
    onClose: () -> Unit,
) {
    val state = uiState.info

    when (uiState.step) {
        Step.FetchCode -> FetchCodeScreen(
            state = state,
            onPhoneOrEmailChange = onPhoneOrEmailChange,
            onSendCode = onSendCode,
            onNext = onNext,
            onClose = onClose,
        )

        Step.Confirm -> ConfirmScreen(
            state = state,
            onPhoneOrEmailChange = onPhoneOrEmailChange,
            onBack = onPrev,
            onConfirm = onConfirm,
        )
    }
}

@Composable
private fun FetchCodeScreen(
    state: PhoneOrEmailInfo,
    onPhoneOrEmailChange: (PhoneOrEmailInfo) -> Unit,
    onSendCode: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
) {
    val isValidPhoneOrEmail = state.isValidPhoneOrEmail
    val isValidCode = state.code.isValidVerifyCode()

    val enableNext = isValidPhoneOrEmail && isValidCode

    Column {
        CloseTopAppBar(stringResource(R.string.reset_password), onClose = { onClose() })

        Spacer(Modifier.height(12.dp))

        Column(Modifier.padding(horizontal = 16.dp)) {
            PhoneOrEmailInput(
                value = state.value,
                onValueChange = { onPhoneOrEmailChange(state.copy(value = it)) },
                callingCode = state.cc,
                onCallingCodeChange = { onPhoneOrEmailChange(state.copy(cc = it)) },
            )

            SendCodeInput(
                code = state.code,
                onCodeChange = { onPhoneOrEmailChange(state.copy(code = it)) },
                onSendCode = onSendCode,
                ready = isValidPhoneOrEmail,
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.next),
                enabled = enableNext,
                onClick = { onNext() },
            )
        }
    }
}

@Composable
private fun ConfirmScreen(
    state: PhoneOrEmailInfo,
    onPhoneOrEmailChange: (PhoneOrEmailInfo) -> Unit,
    onConfirm: () -> Unit,
    onBack: () -> Unit
) {
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val isValidPhoneOrEmail = state.isValidPhoneOrEmail
    val isValidCode = state.code.isValidVerifyCode()
    val isValidPassword = state.password.isValidPassword()

    val enableConfirm = isValidPhoneOrEmail && isValidCode && isValidPassword && confirmPassword == state.password

    Column {
        BackTopAppBar(stringResource(R.string.new_password), onBackPressed = onBack)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            PasswordInput(
                password = state.password,
                onPasswordChange = { onPhoneOrEmailChange(state.copy(password = it)) },
            )

            PasswordInput(
                password = confirmPassword,
                onPasswordChange = { confirmPassword = it },
                checkValid = true,
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.confirm),
                enabled = enableConfirm,
                onClick = { onConfirm() },
            )
        }

        FlatTextCaption(stringResource(R.string.password_rule_tips), Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun PhoneBindScreenPreview() {
    FlatPage {
        PasswordResetScreen(
            uiState = PasswordResetUiState(
                info = PhoneOrEmailInfo(
                    value = "12345678901",
                    cc = "86",
                    code = "123456",
                    password = "123456",
                ),
                step = Step.Confirm,
            ),
            onPhoneOrEmailChange = {},
            onSendCode = {},
            onNext = {},
            onPrev = {},
            onConfirm = {},
            onClose = {},
        )
    }
}
