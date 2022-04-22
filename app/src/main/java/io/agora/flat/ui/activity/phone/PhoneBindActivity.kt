package io.agora.flat.ui.activity.phone

import PhoneAndCodeArea
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton

class PhoneBindActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val actioner: (PhoneBindUiAction) -> Unit = { action ->
                when (action) {
                    else -> {}
                }
            }
            PhoneBindScreen(actioner = actioner)
        }
    }
}

@Composable
fun PhoneBindScreen(
    viewModel: PhoneBindViewModel = hiltViewModel(),
    actioner: (PhoneBindUiAction) -> Unit,
) {
    val viewState by viewModel.state.collectAsState()

    PhoneBindScreen(viewState = viewState, actioner = actioner)
}

@Composable
internal fun PhoneBindScreen(
    viewState: PhoneBindUiViewState,
    actioner: (PhoneBindUiAction) -> Unit,
) {
    var phone by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }

    FlatColumnPage {
        CloseTopAppBar(title = "PhoneBind", {})
        PhoneAndCodeArea(
            phone,
            onPhoneChange = { phone = it },
            code,
            onCodeChange = { code = it },
            onSendCode = {
                actioner(PhoneBindUiAction.SendCode(phone))
            }
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(text = "确定", onClick = {
                actioner(PhoneBindUiAction.Bind(phone, code))
            })
        }
    }
}

@Composable
@Preview
internal fun PhoneBindScreenPreview() {
    PhoneBindScreen(viewState = PhoneBindUiViewState.Empty) {}
}
