package io.agora.flat.ui.activity.setting

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatBasicTextField
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatDivider
import io.agora.flat.ui.compose.FlatPrimaryTextField
import io.agora.flat.ui.compose.FlatTextOnButton
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.ui.viewmodel.EditNameUiState
import io.agora.flat.ui.viewmodel.EditNameViewModel
import io.agora.flat.util.showToast

@AndroidEntryPoint
class EditNameActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EditNameScreen(onFinish = { finish() })
        }
    }
}

@Composable
private fun EditNameScreen(onFinish: () -> Unit, viewModel: EditNameViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    val activity = LocalContext.current as Activity

    ShowUiMessageEffect(state.message) {
        viewModel.clearMessage(it)
    }

    LaunchedEffect(state) {
        if (state.success) {
            activity.showToast(R.string.request_common_success)
            onFinish()
        }
    }

    EditNameScreen(state = state, onFinish = onFinish, onRename = viewModel::rename)
}

@Composable
private fun EditNameScreen(state: EditNameUiState, onFinish: () -> Unit, onRename: (String) -> Unit) {
    var name by remember { mutableStateOf(state.name) }

    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_user_info), onBackPressed = onFinish) {
            TextButton(onClick = { onRename(name) }) {
                FlatTextOnButton(stringResource(R.string.save))
            }
        }

        Column(Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_user_profile_head), contentDescription = "")
                Spacer(Modifier.width(8.dp))
                FlatBasicTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                    placeholderValue = stringResource(R.string.input_nickname_hint)
                )
            }
            FlatDivider(thickness = 1.dp)
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20, locale = "en")
fun EditNameScreenPreview() {
    EditNameScreen(EditNameUiState(""), {}, {})
}