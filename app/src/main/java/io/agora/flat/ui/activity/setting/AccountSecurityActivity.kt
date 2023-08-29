package io.agora.flat.ui.activity.setting

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Config
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.login.LoginType
import io.agora.flat.common.login.UserBindingHandler
import io.agora.flat.data.model.LoginPlatform
import io.agora.flat.data.model.Meta
import io.agora.flat.data.model.UserBindings
import io.agora.flat.data.model.UserInfo
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.ui.viewmodel.AccountSecurityUiState
import io.agora.flat.ui.viewmodel.AccountSecurityViewModel
import io.agora.flat.util.getActivity
import javax.inject.Inject

@AndroidEntryPoint
class AccountSecurityActivity : BaseComposeActivity() {
    @Inject
    lateinit var bindingHandler: UserBindingHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                AccountSecurityScreen(
                    onBindGithub = { bindingHandler.bindWithType(LoginType.Github) },
                    onBindWeChat = { bindingHandler.bindWithType(LoginType.WeChat) },
                    onBindGoogle = {},
                    onBindPhone = { Navigator.launchPhoneBindActivity(this, Constants.From.UserSecurity) },
                    onBindEmail = { Navigator.launchEmailBindActivity(this) },
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
private fun AccountSecurityScreen(
    onBindGithub: () -> Unit,
    onBindWeChat: () -> Unit,
    onBindGoogle: () -> Unit,
    onBindPhone: () -> Unit,
    onBindEmail: () -> Unit,
    viewModel: AccountSecurityViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    var loginPlatform by remember { mutableStateOf<LoginPlatform?>(null) }
    val showUnbindNotice = loginPlatform != null && state.bindingCount > 1
    val showUnbindLimit = loginPlatform != null && state.bindingCount == 1


    ShowUiMessageEffect(state.message) {
        viewModel.clearMessage(it)
    }

    LaunchedEffect(state) {
        if (state.deleteAccount) {
            Navigator.launchHomeActivity(context)
        }
    }

    LifecycleHandler(
        onResume = {
            viewModel.refreshUser()
        }
    )

    AccountSecurityScreen(
        state = state,
        onBindGithub = onBindGithub,
        onBindWeChat = onBindWeChat,
        onBindGoogle = onBindGoogle,
        onBindPhone = onBindPhone,
        onBindEmail = onBindEmail,
        onUnbind = { loginPlatform = it },
    )

    if (showUnbindNotice) {
        UnbindNoticeDialog(
            onConfirm = {
                loginPlatform?.run { viewModel.processUnbind(this) }
                loginPlatform = null
            },
            onCancel = {
                loginPlatform = null
            }
        )
    }

    if (showUnbindLimit) {
        UnbindLimitDialog(
            onConfirm = {
                loginPlatform = null
            }
        )
    }
}

@Composable
private fun AccountSecurityScreen(
    state: AccountSecurityUiState,
    onBindGithub: () -> Unit,
    onBindWeChat: () -> Unit,
    onBindGoogle: () -> Unit,
    onBindPhone: () -> Unit,
    onBindEmail: () -> Unit,
    onUnbind: (LoginPlatform) -> Unit,
) {
    var showCancelDialog by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    var detail: String? = null
    if (state.roomCount != 0) {
        detail = stringResource(R.string.account_security_cancel_limited_tip, state.roomCount)
    }
    val hasPassword = state.userInfo?.hasPassword == true

    Column {
        BackTopAppBar(stringResource(R.string.account_security), {
            context.getActivity()?.finish()
        })

        LazyColumn(Modifier.fillMaxWidth()) {
            if (state.bindings != null) {
                item {
                    BindingItems(
                        state.bindings,
                        onBindGithub,
                        onBindWeChat,
                        onBindGoogle,
                        onBindPhone,
                        onBindEmail,
                        onUnbind,
                    )
                }
            }
            item {
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

            item {
                SettingItem(
                    id = R.drawable.ic_settings_close_account,
                    tip = stringResource(R.string.cancel_account),
                    detail = detail,
                    enabled = state.roomCount == 0,
                    onClick = { showCancelDialog = true }
                )
            }
        }
    }


    if (showCancelDialog) {
        CancelAccountDialog(onDismissRequest = { showCancelDialog = false })
    }
}

@Composable
fun BindingItems(
    bindings: UserBindings,
    onBindGithub: () -> Unit,
    onBindWeChat: () -> Unit,
    onBindGoogle: () -> Unit,
    onBindPhone: () -> Unit,
    onBindEmail: () -> Unit,
    onUnbind: (LoginPlatform) -> Unit,
) {
    val notBound = stringResource(R.string.not_bound)
    val phoneDesc = if (bindings.phone) bindings.meta.phone else notBound
    val emailDesc = if (bindings.email) bindings.meta.email else notBound
    val githubDesc = if (bindings.github) bindings.meta.github else notBound
    val googleDesc = if (bindings.google) bindings.meta.google else notBound
    val wechatDesc = if (bindings.wechat) bindings.meta.wechat else notBound

    SettingItem(
        id = R.drawable.ic_user_profile_phone,
        tip = stringResource(R.string.phone),
        desc = phoneDesc,
    ) {
        if (bindings.phone) {
            onUnbind(LoginPlatform.Phone)
        } else {
            onBindPhone()
        }
    }
    SettingItem(
        id = R.drawable.ic_user_profile_email,
        tip = stringResource(R.string.email),
        desc = emailDesc
    ) {
        if (bindings.email) {
            onUnbind(LoginPlatform.Email)
        } else {
            onBindEmail()
        }
    }
    SettingItem(
        id = R.drawable.ic_user_profile_github,
        tip = stringResource(R.string.github),
        desc = githubDesc
    ) {
        if (bindings.github) {
            onUnbind(LoginPlatform.Github)
        } else {
            onBindGithub()
        }
    }
    SettingItem(
        id = R.drawable.ic_user_profile_wechat,
        tip = stringResource(R.string.wechat),
        desc = wechatDesc
    ) {
        if (bindings.wechat) {
            onUnbind(LoginPlatform.WeChat)
        } else {
            onBindWeChat()
        }
    }
    SettingItem(
        id = R.drawable.ic_user_profile_google,
        tip = stringResource(R.string.google),
        desc = googleDesc
    ) {
        if (bindings.google) {
            onUnbind(LoginPlatform.Google)
        } else {
            onBindGoogle()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CancelAccountDialog(onDismissRequest: () -> Unit, viewModel: AccountSecurityViewModel = hiltViewModel()) {
    val scrollState = rememberScrollState()

    FlatTheme {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                Modifier.widthIn(max = 400.dp),
                shape = Shapes.large
            ) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(12.dp))
                    FlatTextTitle(stringResource(R.string.account_security_cancel_dialog_title))
                    Spacer(Modifier.height(12.dp))
                    FlatTextBodyOne(
                        stringResource(R.string.account_security_cancel_dialog_message),
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(scrollState)
                    )
                    Spacer(Modifier.height(16.dp))
                    CancelAccountCheck(viewModel::deleteAccount)
                }
            }
        }
    }
}

@Composable
fun CancelAccountCheck(onAgree: () -> Unit) {
    var agree by remember { mutableStateOf(false) }
    var remainTime by remember { mutableStateOf(0L) }
    val countDownTimer = remember {
        object : CountDownTimer(Config.cancelAccountCountTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainTime = millisUntilFinished / 1000
            }

            override fun onFinish() {
                remainTime = 0
            }
        }
    }
    val buttonText = if (remainTime != 0L) {
        "${stringResource(R.string.account_cancel_and_exit)}($remainTime)"
    } else {
        stringResource(R.string.account_cancel_and_exit)
    }
    val buttonEnable = agree && remainTime == 0L

    Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = agree,
            onCheckedChange = { agree = it }
        )
        Spacer(Modifier.width(4.dp))
        FlatTextBodyTwo(stringResource(R.string.account_cancel_agree_tip))
        Spacer(Modifier.weight(1f))
        CancelAgreeButton(
            text = buttonText,
            enabled = buttonEnable,
            onClick = onAgree,
        )
    }

    DisposableEffect(Unit) {
        countDownTimer.start()
        onDispose {
            countDownTimer.cancel()
        }
    }
}

@Composable
private fun CancelAgreeButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val darkMode = isDarkTheme()

    val border = if (enabled) BorderStroke(1.dp, Red_6) else null
    val colors = if (enabled) {
        ButtonDefaults.outlinedButtonColors(contentColor = Red_6)
    } else {
        ButtonDefaults.textButtonColors(
            backgroundColor = if (enabled) {
                if (darkMode) Blue_7 else Blue_6
            } else {
                if (darkMode) Gray_9 else Gray_2
            },
            contentColor = if (darkMode) Gray_0 else Gray_0,
            disabledContentColor = if (darkMode) Gray_7 else Gray_5
        )
    }

    Button(
        modifier = Modifier
            .defaultMinSize(minWidth = 86.dp)
            .height(40.dp),
        enabled = enabled,
        elevation = null,
        border = border,
        colors = colors,
        onClick = onClick,
    ) {
        FlatTextOnButton(text, softWrap = false, maxLines = 1)
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun AccountSecurityScreenPreview() {
    val state = AccountSecurityUiState(
        roomCount = 1, loading = true, userInfo = UserInfo(
            name = "name",
            avatar = "",
            uuid = "uuid",
            hasPhone = false,
            hasPassword = true,
        ), bindings = UserBindings(
            wechat = false,
            phone = true,
            github = false,
            google = true,
            email = true,
            meta = Meta(
                google = "google",
                phone = "130****0000",
                email = "e**@a.com",
            )
        )
    )
    FlatPage {
        AccountSecurityScreen(
            state = state,
            {},
            {},
            {},
            {},
            {},
            { _ -> },
        )
    }

}