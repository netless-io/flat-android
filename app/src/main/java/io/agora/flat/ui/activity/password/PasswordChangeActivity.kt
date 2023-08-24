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
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatTextCaption
import io.agora.flat.ui.compose.PasswordInput
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.isValidPassword
import io.agora.flat.util.showToast

@AndroidEntryPoint
class PasswordChangeActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                PasswordChangeScreen(onClose = { finish() })
            }
        }
    }
}

@Composable
fun PasswordChangeScreen(onClose: () -> Unit, viewModel: PasswordChangeViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    ShowUiMessageEffect(uiMessage = viewState.message) { id ->
        viewModel.clearUiMessage(id)
    }

    LaunchedEffect(viewState) {
        if (viewState.success) {
            context.showToast(R.string.message_reset_success)
            onClose()
        }
    }

    PasswordChangeScreen(
        onClose = onClose,
        onConfirm = { oldPassword, newPassword ->
            viewModel.changePassword(oldPassword, newPassword)
        }
    )
}

@Composable
fun PasswordChangeScreen(
    onClose: () -> Unit,
    onConfirm: (String, String) -> Unit,
) {
    var oldPassword by rememberSaveable { mutableStateOf("") }
    var newPassword by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val enableConfirm = oldPassword.isValidPassword() && newPassword.isValidPassword() && confirmPassword == newPassword

    Column {
        CloseTopAppBar(stringResource(R.string.new_password), onClose = onClose)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            PasswordInput(
                password = oldPassword,
                onPasswordChange = { oldPassword = it },
                placeholderValue = stringResource(R.string.old_password_input_hint)
            )

            PasswordInput(
                password = newPassword,
                onPasswordChange = { newPassword = it },
                placeholderValue = stringResource(R.string.new_password_input_hint),
                checkValid = true,
            )

            PasswordInput(
                password = confirmPassword,
                onPasswordChange = { confirmPassword = it },
                placeholderValue = stringResource(R.string.new_password_input_hint_again),
            )
        }

        Spacer(Modifier.height(12.dp))

        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.confirm),
                enabled = enableConfirm,
                onClick = { onConfirm(oldPassword, newPassword) },
            )
        }

        FlatTextCaption(stringResource(R.string.password_rule_tips), Modifier.align(Alignment.CenterHorizontally))
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun PasswordChangeScreenPreview() {
    FlatPage {
        PasswordChangeScreen(onClose = {}, onConfirm = { _, _ -> {} })
    }
}
