package io.agora.flat.ui.activity.setting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
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
import io.agora.flat.ui.util.ShowUiMessageEffect
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

    ShowUiMessageEffect(uiMessage = state.message) {
        viewModel.clearMessage(it)
    }

    LifecycleHandler(
        onResume = {
            viewModel.refreshUser()
        }
    )

    UserInfoScreen(state, actioner)
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
    var hasPassword = state.userInfo?.hasPassword == true

    val launcherPickAvatar = launcherPickContent {
        avatar = it.uri
        actioner(UserInfoUiAction.PickedAvatar(it))
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
        item {
            BindingItem(
                icon = R.drawable.ic_user_profile_github,
                tip = stringResource(R.string.github),
                bind = bindingGithub
            ) {
                if (bindingGithub) {
                    actioner(UserInfoUiAction.UnbindGithub)
                } else {
                    actioner(UserInfoUiAction.BindGithub)
                }
            }
            BindingItem(
                icon = R.drawable.ic_user_profile_wechat,
                tip = stringResource(R.string.wechat),
                bind = bindingWeChat
            ) {
                if (bindingWeChat) {
                    actioner(UserInfoUiAction.UnbindWeChat)
                } else {
                    actioner(UserInfoUiAction.BindWeChat)
                }
            }

            if (hasPassword) {
                SettingItem(
                    id = R.drawable.ic_login_password,
                    tip = stringResource(R.string.change_password),
                    onClick = {
                        Navigator.launchPasswordChangeActivity(context)
                    }
                )
            } else {
                SettingItem(
                    id = R.drawable.ic_login_password,
                    tip = stringResource(R.string.set_password),
                    onClick = {
                        Navigator.launchPasswordSetActivity(context)
                    }
                )
            }
        }
    }
}

@Composable
internal fun AvatarItem(
    avatar: Any?,
    onIconClick: () -> Unit = {},
) {
    Row(Modifier.heightIn(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(16.dp))
        Image(painterResource(R.drawable.ic_user_profile_avater), contentDescription = null)
        Spacer(Modifier.width(4.dp))
        FlatTextBodyOne(stringResource(R.string.user_avatar), Modifier.weight(1f))
        IconButton(onClick = onIconClick) {
            Image(
                painter = rememberImagePainter(avatar),
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
    @DrawableRes icon: Int,
    tip: String,
    bind: Boolean,
    onClick: () -> Unit,
) {
    Row(Modifier.heightIn(48.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(16.dp))
        Image(painterResource(icon), contentDescription = null)
        Spacer(Modifier.width(4.dp))
        FlatTextBodyOne(tip, Modifier.weight(1f))
        TextButton(onClick = onClick) {
            FlatTextOnButton(stringResource(if (bind) R.string.unbind else R.string.bind))
        }
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
fun DefaultPreview() {
    val uiState = UserInfoUiState(
        UserInfo(
            name = "name",
            avatar = "",
            uuid = "uuid",
            hasPhone = false,
            hasPassword = true,
        ),
        UserBindings(
            wechat = false,
            phone = true,
            github = false,
            google = true
        )
    )
    FlatPage { UserInfoScreen(state = uiState) { } }
}
