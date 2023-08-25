package io.agora.flat.ui.activity.account

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
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.PhoneInput
import io.agora.flat.ui.compose.SendCodeInput
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.isValidPhone
import io.agora.flat.util.isValidSmsCode

@AndroidEntryPoint
class MergeAccountActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatPage {
                MergeAccountScreen(
                    onSuccess = {
                        Navigator.launchHomeActivity(this@MergeAccountActivity)
                        finish()
                    },
                    onClose = {
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun MergeAccountScreen(
    onSuccess: () -> Unit,
    onClose: () -> Unit,
    viewModel: MergeAccountViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state.bindSuccess) {
            onSuccess()
        }
    }

    ShowUiMessageEffect(uiMessage = state.message, onMessageShown = { id ->
        viewModel.clearUiMessage(id)
    })

    MergeAccountScreen(
        state = state,
        onBindClose = onClose,
        onSendPhoneCode = { phone ->
            viewModel.sendPhoneCode(phone)
        },
        onMergeByPhone = { phone, code ->
            viewModel.mergeAccountByPhone(phone, code)
        },
    )
}

@Composable
internal fun MergeAccountScreen(
    state: MergeAccountUiState,
    onBindClose: () -> Unit = {},
    onSendPhoneCode: (String) -> Unit = {},
    onMergeByPhone: (String, String) -> Unit = { _, _ -> },
) {
    var phone by remember { mutableStateOf(state.phone) }
    var ccode by remember { mutableStateOf(state.ccode) }
    var code by remember { mutableStateOf("") }
    val buttonEnable = phone.isValidPhone() && code.isValidSmsCode() && !state.binding

    Column {
        CloseTopAppBar(stringResource(R.string.bind_phone), onClose = onBindClose)
        Spacer(Modifier.height(16.dp))
        Column(Modifier.padding(horizontal = 16.dp)) {
            PhoneInput(
                phone = phone,
                onPhoneChange = { phone = it },
                callingCode = ccode,
                onCallingCodeChange = { ccode = it },
                enabled = false,
            )

            SendCodeInput(
                code = code,
                onCodeChange = { code = it },
                onSendCode = {
                    onSendPhoneCode("$ccode$phone")
                },
                ready = phone.isValidPhone()
            )
        }
        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.confirm),
                enabled = buttonEnable,
                onClick = { onMergeByPhone("$ccode$phone", code) },
            )
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun PhoneBindScreenPreview() {
    FlatPage {
        MergeAccountScreen(state = MergeAccountUiState.Empty, {}, {}, { _, _ -> })
    }
}