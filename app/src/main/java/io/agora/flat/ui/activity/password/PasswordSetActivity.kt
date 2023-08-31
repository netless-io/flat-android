package io.agora.flat.ui.activity.password

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.error.FlatErrorHandler
import io.agora.flat.data.onFailure
import io.agora.flat.data.onSuccess
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatTextCaption
import io.agora.flat.ui.compose.PasswordInput
import io.agora.flat.util.isValidPassword
import io.agora.flat.util.showToast
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PasswordSetActivity : BaseComposeActivity() {
    @Inject
    lateinit var userRepository: UserRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                PasswordSetScreen(
                    onClose = { finish() },
                    onConfirm = { password ->
                        lifecycleScope.launch {
                            userRepository.setPassword(password)
                                .onSuccess {
                                    showToast(R.string.message_set_password_success)
                                    finish()
                                }.onFailure {
                                    showToast(FlatErrorHandler.getErrorStr(this@PasswordSetActivity, it))
                                }
                        }
                    })
            }
        }
    }
}

@Composable
fun PasswordSetScreen(
    onClose: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }
    var confirmPassword by rememberSaveable { mutableStateOf("") }

    val enableConfirm = password.isValidPassword() && confirmPassword == password

    Column {
        CloseTopAppBar(stringResource(R.string.new_password), onClose = onClose)
        Spacer(Modifier.height(12.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            PasswordInput(
                password = password,
                onPasswordChange = { password = it },
                placeholderValue = stringResource(R.string.new_password_input_hint),
                checkValid = true,
            )

            PasswordInput(
                password = confirmPassword,
                onPasswordChange = { confirmPassword = it },
                placeholderValue = stringResource(R.string.new_password_input_hint_again)
            )

            Box(Modifier.padding(top = 24.dp, bottom = 12.dp)) {
                FlatPrimaryTextButton(
                    text = stringResource(id = R.string.confirm),
                    enabled = enableConfirm,
                    onClick = { onConfirm(password) },
                )
            }

            FlatTextCaption(stringResource(R.string.password_rule_tips), Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun PasswordSetScreenPreview() {
    FlatPage {
        PasswordSetScreen(onClose = {}, onConfirm = { })
    }
}
