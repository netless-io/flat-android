package io.agora.flat.ui.activity.setting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
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
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.UserInfo
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatAvatar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.LifecycleHandler
import io.agora.flat.ui.compose.launcherPickContent
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.ui.viewmodel.UserInfoUiState
import io.agora.flat.ui.viewmodel.UserInfoViewModel
import io.agora.flat.util.ContentInfo

@AndroidEntryPoint
class UserInfoActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UserInfoScreen(
                onBackPressed = { finish() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}


@Composable
internal fun UserInfoScreen(
    onBackPressed: () -> Unit,
    viewModel: UserInfoViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()

    ShowUiMessageEffect(uiMessage = state.message) {
        viewModel.clearMessage(it)
    }

    LifecycleHandler(
        onResume = {
            viewModel.refreshUser()
        }
    )

    UserInfoScreen(
        state,
        onBackPressed,
        onPickedAvatar = { info ->
            viewModel.handlePickedAvatar(info)
        },
    )
}

@Composable
internal fun UserInfoScreen(
    state: UserInfoUiState,
    onBackPressed: () -> Unit,
    onPickedAvatar: (ContentInfo) -> Unit
) {
    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_user_info), { onBackPressed() })
        SettingList(state, onPickedAvatar)
    }
}

@Composable
private fun SettingList(
    state: UserInfoUiState,
    onPickedAvatar: (ContentInfo) -> Unit
) {
    val context = LocalContext.current
    // avatar may be image uri string or android content uri
    var avatar by remember { mutableStateOf<Any?>(state.userInfo?.avatar) }

    val launcherPickAvatar = launcherPickContent {
        avatar = it.uri
        onPickedAvatar(it)
    }

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            AvatarItem(avatar = avatar) {
                launcherPickAvatar("image/*")
            }

            SettingItem(
                id = R.drawable.ic_user_profile_head,
                tip = stringResource(R.string.username),
                desc = state.userInfo?.name ?: "",
                onClick = { Navigator.launchEditNameActivity(context) }
            )
        }
    }
}

@Composable
internal fun AvatarItem(
    avatar: Any?,
    onAvatarClick: () -> Unit,
) {
    Row(
        Modifier
            .heightIn(48.dp)
            .padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painterResource(R.drawable.ic_user_profile_avater), contentDescription = null)
        Spacer(Modifier.width(4.dp))
        FlatTextBodyOne(stringResource(R.string.user_avatar), Modifier.weight(1f))
        IconButton(onClick = onAvatarClick) {
            FlatAvatar(avatar = avatar, size = 32.dp)
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20, locale = "en")
private fun DefaultPreview() {
    val uiState = UserInfoUiState(
        UserInfo(
            name = "name",
            avatar = "",
            uuid = "uuid",
            hasPhone = false,
            hasPassword = true,
        ),
    )
    FlatPage { UserInfoScreen(state = uiState, {}) { } }
}