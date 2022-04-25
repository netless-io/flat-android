package io.agora.flat.ui.activity.setting

import android.app.Activity
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatPrimaryTextField
import io.agora.flat.ui.compose.FlatTextButton
import io.agora.flat.ui.viewmodel.UserViewModel
import io.agora.flat.util.showToast
import kotlinx.coroutines.launch

@AndroidEntryPoint
class EditNameActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            EditNameScreen()
        }
    }
}

@Composable
private fun EditNameScreen() {
    val viewModel: UserViewModel = hiltViewModel()
    val state by viewModel.userInfo.collectAsState()

    var name by remember { mutableStateOf(state?.name ?: "") }
    val scope = rememberCoroutineScope()
    val activity = LocalContext.current as Activity

    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_user_info), {
            activity.finish()
        }) {
            TextButton(onClick = {
                scope.launch {
                    if (viewModel.rename(name = name)) {
                        activity.showToast(R.string.request_common_success)
                        activity.finish()
                    } else {
                        activity.showToast(R.string.error_request_common_fail)
                    }
                }
            }) {
                FlatTextButton(stringResource(id = R.string.save))
            }
        }
        Box(Modifier.padding(16.dp)) {
            FlatPrimaryTextField(
                value = name,
                onValueChange = {
                    name = it
                },
            )
        }
    }
}