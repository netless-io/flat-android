package io.agora.flat.ui.activity.phone

import PhoneAndCodeArea
import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.util.isValidPhone
import io.agora.flat.util.isValidSmsCode

@AndroidEntryPoint
class PhoneBindActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PhoneBindScreen()
        }
    }
}

@Composable
fun PhoneBindScreen(
    viewModel: PhoneBindViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    val actioner: (PhoneBindUiAction) -> Unit = { action ->
        when (action) {
            PhoneBindUiAction.Close -> {
                (context as Activity).finish()
            }
            is PhoneBindUiAction.Bind -> viewModel.bindPhone(action.phone, action.code)
            is PhoneBindUiAction.SendCode -> viewModel.sendSmsCode(action.phone)
        }
    }
    LaunchedEffect(viewState) {
        if (viewState.bindSuccess) {
            Navigator.launchHomeActivity(context = context)
        }
    }
    PhoneBindScreen(viewState = viewState, actioner = actioner)
}

@Composable
internal fun PhoneBindScreen(
    viewState: PhoneBindUiViewState,
    actioner: (PhoneBindUiAction) -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    val buttonEnable = phone.isValidPhone() && code.isValidSmsCode() && !viewState.binding

    FlatColumnPage {
        CloseTopAppBar(stringResource(R.string.bind_phone), {
            actioner(PhoneBindUiAction.Close)
        })
        Spacer(Modifier.height(16.dp))
        PhoneAndCodeArea(
            phone,
            onPhoneChange = { phone = it },
            code,
            onCodeChange = { code = it },
            onSendCode = {
                actioner(PhoneBindUiAction.SendCode("+86${phone}"))
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = "确定",
                enabled = buttonEnable,
                onClick = {
                    actioner(PhoneBindUiAction.Bind("+86${phone}", code))
                },
            )
        }
    }
}

@Composable
@Preview
internal fun PhoneBindScreenPreview() {
    PhoneBindScreen(viewState = PhoneBindUiViewState.Empty) {}
}
