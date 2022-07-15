package io.agora.flat.ui.activity.setting

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.coil.rememberCoilPainter
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

@AndroidEntryPoint
class UserInfoActivity : BaseComposeActivity() {

    @Inject
    lateinit var bindingHandler: UserBindingHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatColumnPage {
                val viewModel = viewModel(UserInfoViewModel::class.java)
                val state by viewModel.state.collectAsState()

                val actioner: (UserInfoUiAction) -> Unit = { action ->
                    when (action) {
                        UserInfoUiAction.BindGithub -> {
                            bindingHandler.bindWithType(LoginType.Github)
                        }
                        UserInfoUiAction.BindWeChat -> {
                            bindingHandler.bindWithType(LoginType.WeChat)
                        }
                        else -> viewModel.processAction(action)
                    }
                }

                BackTopAppBar(stringResource(R.string.title_user_info), { finish() })

                SettingList(state, actioner)

                LifecycleHandler(
                    onResume = {
                        viewModel.refreshUser()
                        intent?.run {
                            bindingHandler.handleResult(this)
                        }
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun SettingList(state: UserInfoUiState, actioner: (UserInfoUiAction) -> Unit) {
    val context = LocalContext.current
    val bindingWeChat = state.bindings?.wechat == true
    val bindingGithub = state.bindings?.github == true
    var avatar by remember { mutableStateOf<Any?>(state.userInfo?.avatar) }

    val launcherPickAvatar = launcherPickContent {
        avatar = it.uri
        actioner(UserInfoUiAction.PickedAvatar(it))
    }

    LazyColumn(Modifier.fillMaxWidth()) {
        item {
            AvatarItem(tip = stringResource(R.string.user_avatar), userAvatar = avatar) {
                launcherPickAvatar("image/*")
            }
            SettingItem(
                tip = stringResource(R.string.username),
                desc = state.userInfo?.name ?: "",
                onClick = { Navigator.launchEditNameActivity(context) }
            )
        }
        item {
            FlatTextBodyOneSecondary(stringResource(R.string.bind_info), Modifier.padding(16.dp))
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
    onClick: () -> Unit = {},
) {
    val color = MaterialTheme.colors.onBackground
    CompositionLocalProvider(LocalContentColor provides color.copy(alpha = 1f)) {
        Row(
            Modifier
                .fillMaxWidth()
                .heightIn(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))
            FlatTextBodyOne(text = tip,
                Modifier
                    .weight(1f)
                    .padding(vertical = 8.dp))
            Spacer(Modifier.width(8.dp))
            IconButton(onClick = { onClick() }) {
                Image(
                    painter = rememberCoilPainter(userAvatar),
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
}


@Composable
private fun BindingItem(
    tip: String,
    bind: Boolean,
    onClick: () -> Unit,
) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyOne(tip)
        Spacer(modifier = Modifier.weight(1f))
        TextButton(onClick = onClick) {
            FlatTextButton(if (bind) "解绑" else "绑定")
        }
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    FlatColumnPage {
        SettingList(UserInfoUiState(
            UserInfo("name", "", "uuid", false),
            UserBindings(
                wechat = false,
                phone = true,
                github = false,
                google = true
            )
        )) { }
    }
}
