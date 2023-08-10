package io.agora.flat.ui.activity.register

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.PasswordInput
import io.agora.flat.ui.compose.PhoneOrEmailInput
import io.agora.flat.ui.compose.SendCodeInput
import io.agora.flat.util.isValidEmail
import io.agora.flat.util.isValidPhone
import io.agora.flat.util.showToast

@AndroidEntryPoint
class RegisterActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                RegisterScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun RegisterScreen(
    onClose: () -> Unit,
    viewModel: RegisterViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    LaunchedEffect(viewState) {
        viewState.message?.let {
            context.showToast(it.text)
        }
    }

    RegisterScreen(
        viewState = viewState,
        onInfoUpdate = {
            viewModel.updateRegisterInfo(it)
        },
        onClose = onClose,
        onConfirm = {
            viewModel.register()
        },
        onSendCode = {
            viewModel.sendCode()
        },
    )
}

@Composable
internal fun RegisterScreen(
    viewState: RegisterUiState,
    onInfoUpdate: (RegisterInfo) -> Unit,
    onClose: () -> Unit,
    onConfirm: () -> Unit,
    onSendCode: () -> Unit,
) {
    val info = viewState.registerInfo
    val buttonEnable = (info.value.isValidPhone() || info.value.isValidEmail())
            && info.password.isNotEmpty() && info.code.isNotEmpty()

    val codeReady = info.value.isValidPhone() || info.value.isValidEmail()

    Column {
        CloseTopAppBar(stringResource(R.string.register), {
            onClose()
        })
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            PhoneOrEmailInput(
                value = info.value,
                onValueChange = { onInfoUpdate(info.copy(value = it)) },
                callingCode = info.cc,
                onCallingCodeChange = { onInfoUpdate(info.copy(cc = it)) },
            )

            SendCodeInput(
                code = info.code,
                onCodeChange = { onInfoUpdate(info.copy(code = it)) },
                onSendCode = onSendCode,
                ready = codeReady,
            )

            PasswordInput(
                password = info.password,
                onPasswordChange = { onInfoUpdate(info.copy(password = it)) },
            )
        }

        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.confirm),
                enabled = buttonEnable,
                onClick = {
                    onConfirm()
                },
            )
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun PhoneBindScreenPreview() {
    FlatPage {
        RegisterScreen(
            viewState = RegisterUiState.Empty.copy(
                registerInfo = RegisterInfo(
                    value = "12345678901",
                    cc = "86",
                    code = "123456",
                    password = "123456",
                )
            ),
            onInfoUpdate = {},
            onClose = {},
            onConfirm = {},
            onSendCode = {})
    }
}
