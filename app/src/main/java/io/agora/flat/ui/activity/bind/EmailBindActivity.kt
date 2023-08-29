package io.agora.flat.ui.activity.bind

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.EmailInput
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.SendCodeInput
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.isValidEmail
import io.agora.flat.util.isValidVerifyCode
import io.agora.flat.util.showToast

@AndroidEntryPoint
class EmailBindActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatPage {
                EmailBindScreen(
                    onBindSuccess = {
                        this@EmailBindActivity.showToast(R.string.bind_success)
                        finish()
                    },
                    onBindClose = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun EmailBindDialog(onBindSuccess: () -> Unit, onDismissRequest: () -> Unit) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            Modifier
                .widthIn(max = 400.dp)
                .height(500.dp),
            shape = Shapes.large,
        ) {
            EmailBindScreen(onBindSuccess)
        }
    }
}

@Composable
fun EmailBindScreen(
    onBindSuccess: () -> Unit,
    onBindClose: () -> Unit = {},
    viewModel: EmailBindViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state.bindSuccess) {
            onBindSuccess()
        }
    }

    LaunchedEffect(state) {
        if (state.bindSuccess) {
            context.showToast(R.string.message_code_send_success)
            viewModel.clearCodeSuccess()
        }
    }

    ShowUiMessageEffect(uiMessage = state.message) {
        viewModel.clearMessage(it)
    }


    EmailBindScreen(
        viewState = state,
        onSendCode = { viewModel.sendEmailCode(email = it) },
        onBind = { email, code ->
            viewModel.bindEmail(email, code)
        },
        onBindClose = onBindClose,
    )
}

@Composable
internal fun EmailBindScreen(
    viewState: EmailBindUiState,
    onSendCode: (String) -> Unit,
    onBind: (String, String) -> Unit,
    onBindClose: () -> Unit,
) {
    var email by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    val buttonEnable = email.isValidEmail() && code.isValidVerifyCode() && !viewState.binding

    Column {
        CloseTopAppBar(stringResource(R.string.bind_email), onClose = onBindClose)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            EmailInput(
                value = email,
                onValueChange = { email = it },
            )

            SendCodeInput(
                code = code,
                onCodeChange = { code = it },
                onSendCode = { onSendCode(email) },
                ready = true
            )

        }
        Spacer(Modifier.height(12.dp))
        Box(Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(R.string.confirm),
                enabled = buttonEnable,
                onClick = {
                    onBind(email, code)
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
        EmailBindScreen(viewState = EmailBindUiState.Empty, {}, { _, _ -> }, {})
    }
}