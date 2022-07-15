package io.agora.flat.ui.activity.setting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.rememberImagePainter
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.login.LoginType
import io.agora.flat.common.login.UserBindingHandler
import io.agora.flat.data.model.UserBindings
import io.agora.flat.data.model.UserInfo
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.viewmodel.UserInfoUiAction
import io.agora.flat.ui.viewmodel.UserInfoUiState
import io.agora.flat.ui.viewmodel.UserInfoViewModel
import javax.inject.Inject

/**
 * bindingHandler -> thirdParty(webview, wechat) -> XXXEntryActivity -> LoginManager -> bindingHandler.handleResult
 */
@AndroidEntryPoint
class UserInfoActivity : BaseComposeActivity() {

    @Inject
    lateinit var bindingHandler: UserBindingHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            UserInfoScreen(
                onBindGithub = { bindingHandler.bindWithType(LoginType.Github) },
                onBindWeChat = { bindingHandler.bindWithType(LoginType.WeChat) },
                onBackPressed = { finish() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        intent?.run {
            bindingHandler.handleResult(this)
        }
    }
}


@Composable
internal fun UserInfoScreen(
    viewModel: UserInfoViewModel = hiltViewModel(),
    onBindGithub: () -> Unit,
    onBindWeChat: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val state by viewModel.state.collectAsState()

    val actioner: (UserInfoUiAction) -> Unit = { action ->
        when (action) {
            UserInfoUiAction.Finish -> {
                onBackPressed()
            }
            UserInfoUiAction.BindGithub -> {
                onBindGithub()
            }
            UserInfoUiAction.BindWeChat -> {
                onBindWeChat()
            }
            else -> viewModel.processAction(action)
        }
    }

    UserInfoScreen(state, actioner)
    LifecycleHandler(onResume = { viewModel.refreshUser() })
}

@Composable
internal fun UserInfoScreen(state: UserInfoUiState, actioner: (UserInfoUiAction) -> Unit) {
    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_user_info), { actioner(UserInfoUiAction.Finish) })
        SettingList(state, actioner)
    }
}

@Composable
private fun SettingList(state: UserInfoUiState, actioner: (UserInfoUiAction) -> Unit) {
    val context = LocalContext.current
    val bindingWeChat = state.bindings?.wechat == true
    val bindingGithub = state.bindings?.github == true
    // avatar may be image uri string or android content uri
    var avatar by remember { mutableStateOf<Any?>(state.userInfo?.avatar) }

    val launcherPickAvatar = launcherPickContent {
        avatar = it.uri
        actioner(UserInfoUiAction.PickedAvatar(it))
    }

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            AvatarItem(stringResource(R.string.user_avatar), userAvatar = avatar) {
                launcherPickAvatar("image/*")
            }
            SettingItem(
                tip = stringResource(R.string.username),
                desc = state.userInfo?.name ?: "",
                onClick = { Navigator.launchEditNameActivity(context) }
            )
        }
        item {
            FlatTextBodyTwo(stringResource(R.string.bind_info), Modifier.padding(16.dp))
            BindingItem(stringResource(R.string.github), bindingGithub) {
                if (bindingGithub) {
                    actioner(UserInfoUiAction.UnbindGithub)
                } else {
                    actioner(UserInfoUiAction.BindGithub)
                }
            }
            BindingItem(stringResource(R.string.wechat), bindingWeChat) {
                if (bindingWeChat) {
                    actioner(UserInfoUiAction.UnbindWeChat)
                } else {
                    actioner(UserInfoUiAction.BindWeChat)
                }
            }
        }
    }
}

@Composable
internal fun AvatarItem(
    tip: String,
    userAvatar: Any?,
    onIconClick: () -> Unit = {},
) {
    Row(Modifier.heightIn(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(16.dp))
        FlatTextBodyOne(tip, Modifier.weight(1f))
        IconButton(onClick = onIconClick) {
            Image(
                painter = rememberImagePainter(userAvatar),
                contentDescription = null,
                modifier = Modifier
                    .size(32.dp, 32.dp)
                    .clip(shape = RoundedCornerShape(16.dp)),
                contentScale = ContentScale.Crop
            )
        }
        Spacer(Modifier.width(16.dp))
    }
}


@Composable
private fun BindingItem(
    tip: String,
    bind: Boolean,
    onClick: () -> Unit,
) {
    Row(Modifier.heightIn(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyOne(tip, modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            FlatTextButton(stringResource(if (bind) R.string.unbind else R.string.bind))
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true, uiMode = 0x30)
@Preview(showBackground = true, uiMode = 0x20)
@Composable
fun DefaultPreview() {
    val userInfo = UserInfo("name", "", "uuid", false)
    val userBindings = UserBindings(wechat = false, phone = true, github = false, google = true)
    UserInfoScreen(UserInfoUiState(userInfo, userBindings)) { }
}
