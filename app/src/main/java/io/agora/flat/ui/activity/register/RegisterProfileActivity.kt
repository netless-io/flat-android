package io.agora.flat.ui.activity.register

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BindPhoneTextField
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.FlatAvatar
import io.agora.flat.ui.compose.FlatDivider
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPageLoading
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatTextBodyTwo
import io.agora.flat.ui.compose.launcherPickContent
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.ContentInfo

@AndroidEntryPoint
class RegisterProfileActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                RegisterProfileScreen(
                    onSuccess = {
                        Navigator.launchHomeActivity(this)
                        finish()
                    }
                )
            }
        }
    }
}

@Composable
fun RegisterProfileScreen(
    onSuccess: () -> Unit = {},
    viewModel: RegisterProfileViewModel = hiltViewModel(),
) {
    val viewState by viewModel.state.collectAsState()

    ShowUiMessageEffect(uiMessage = viewState.message, onMessageShown = {
        viewModel.clearUiMessage(it)
    })

    LaunchedEffect(viewState) {
        if (viewState.success) {
            onSuccess()
        }
    }

    RegisterProfileScreen(
        viewState = viewState,
        onClose = onSuccess,
        onConfirm = { name, avatarRes ->
            viewModel.handleConfirmInfo(name, avatarRes)
        }
    )
}

@Composable
internal fun RegisterProfileScreen(
    viewState: RegisterProfileUiState,
    onClose: () -> Unit = {},
    onConfirm: (String?, ContentInfo?) -> Unit,
) {
    var avatarRes by remember { mutableStateOf<ContentInfo?>(null) }
    var name by remember { mutableStateOf("") }

    val launcherPickAvatar = launcherPickContent {
        avatarRes = it
    }

    val avatar = avatarRes?.uri ?: R.drawable.ic_register_avatar

    Column {
        CloseTopAppBar(title = stringResource(R.string.register_profile_title), onClose = onClose)

        Spacer(Modifier.height(12.dp))

        Column(
            Modifier
                .padding(horizontal = 16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = { launcherPickAvatar("image/*") }) {
                    FlatAvatar(avatar = avatar, size = 80.dp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                FlatTextBodyTwo(text = stringResource(R.string.register_profile_avatar_tip))
            }
            Spacer(modifier = Modifier.height(24.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_user_profile_head), contentDescription = "")
                Spacer(Modifier.width(8.dp))
                BindPhoneTextField(
                    value = name,
                    onValueChange = {
                        name = it
                    },
                    modifier = Modifier
                        .height(48.dp)
                        .weight(1f),
                    placeholderValue = stringResource(R.string.register_profile_username_tip)
                )
            }
            FlatDivider(thickness = 1.dp)
        }

        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.padding(16.dp)) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.confirm),
                onClick = { onConfirm(name, avatarRes) },
            )
        }
    }

    if (viewState.loading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            FlatPageLoading()
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
internal fun RegisterProfilePreview() {
    FlatPage {
        RegisterProfileScreen(
            viewState = RegisterProfileUiState(loading = true),
            onConfirm = { _, _ -> },
        )
    }
}
