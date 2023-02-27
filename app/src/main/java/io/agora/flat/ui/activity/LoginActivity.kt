package io.agora.flat.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices.PIXEL_C
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.login.LoginActivityHandler
import io.agora.flat.common.login.LoginState
import io.agora.flat.common.login.LoginType
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.activity.login.LoginUiAction
import io.agora.flat.ui.activity.phone.PhoneBindDialog
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.Gray_1
import io.agora.flat.ui.theme.isDarkTheme
import io.agora.flat.ui.theme.isTabletMode
import io.agora.flat.ui.viewmodel.LoginViewModel
import io.agora.flat.util.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class LoginActivity : BaseComposeActivity() {
    private val viewModel: LoginViewModel by viewModels()

    @Inject
    lateinit var loginHandler: LoginActivityHandler

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val loginState by loginHandler.observeLoginState().collectAsState()
            var showPhoneBind by remember { mutableStateOf(false) }

            val actioner: (LoginUiAction) -> Unit = { action ->
                when (action) {
                    LoginUiAction.WeChatLogin -> {
                        loginHandler.loginWithType(LoginType.WeChat)
                    }
                    LoginUiAction.GithubLogin -> {
                        loginHandler.loginWithType(LoginType.Github)
                    }
                    LoginUiAction.OpenServiceProtocol -> {
                        Navigator.launchWebViewActivity(this, Constants.URL.Service)
                    }
                    LoginUiAction.OpenPrivacyProtocol -> {
                        Navigator.launchWebViewActivity(this, Constants.URL.Privacy)
                    }
                    LoginUiAction.AgreementHint -> {
                        showToast(R.string.login_agreement_unchecked_hint)
                    }
                    is LoginUiAction.PhoneLogin -> {
                        loginHandler.loginWithPhone(action.phone, action.code)
                    }
                    is LoginUiAction.PhoneSendCode -> {
                        loginHandler.sendPhoneCode(action.phone)
                    }
                }
            }

            LaunchedEffect(loginState) {
                when (loginState) {
                    is LoginState.Process -> {
                        showToast((loginState as LoginState.Process).message.text)
                    }
                    LoginState.Init -> {}
                    LoginState.Success -> {
                        if (viewModel.needBindPhone()) {
                            if (isPhoneMode()) {
                                Navigator.launchPhoneBindActivity(this@LoginActivity)
                            } else {
                                showPhoneBind = true
                            }
                        } else {
                            showToast(R.string.login_success_and_jump)
                            delay(2000)
                            Navigator.launchHomeActivity(this@LoginActivity)
                        }
                    }
                }
            }

            LoginPage(actioner = actioner)

            if (showPhoneBind) {
                PhoneBindDialog(onBindSuccess = {
                    Navigator.launchHomeActivity(this)
                }, onDismissRequest = {
                    // should not cancel dialog
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        if (intent != null) {
            loginHandler.handleResult(intent)
            handleRoomJump(intent)
        }
    }

    private fun handleRoomJump(intent: Intent) {
        lifecycleScope.launch {
            if (intent.data?.scheme == "x-agora-flat-client" && intent.data?.authority == "joinRoom") {
                val roomUUID = intent.data?.getQueryParameter("roomUUID")
                if (viewModel.isLoggedIn() && roomUUID != null) {
                    Navigator.launchHomeActivity(this@LoginActivity, roomUUID)
                    finish()
                }
            }
        }
    }
}

@Composable
internal fun LoginPage(actioner: (LoginUiAction) -> Unit) {
    FlatPage(statusBarColor = Transparent) {
        if (isTabletMode()) {
            LoginMainPad(actioner)
        } else {
            LoginMain(actioner)
        }
    }
}

@Composable
internal fun LoginMain(actioner: (LoginUiAction) -> Unit) {
    LoginArea(Modifier.fillMaxSize(), actioner = actioner)
}

@Composable
internal fun LoginMainPad(actioner: (LoginUiAction) -> Unit) {
    val img = if (isDarkTheme()) R.drawable.img_pad_login_dark else R.drawable.img_pad_login_light

    Row {
        Image(
            painterResource(img),
            contentDescription = null,
            Modifier
                .fillMaxHeight()
                .weight(1f),
            contentScale = ContentScale.Crop,
        )
        Box(
            Modifier
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            LoginArea(modifier = Modifier.width(360.dp), actioner = actioner)
        }
    }
}

@Composable
private fun LoginArea(modifier: Modifier, actioner: (LoginUiAction) -> Unit) {
    var agreementChecked by remember { mutableStateOf(false) }
    var showAgreement by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.weight(1f))
        LoginSlogan()
        Spacer(Modifier.weight(1f))
        PhoneLoginArea(actioner = {
            if (agreementChecked.not() && it is LoginUiAction.PhoneLogin) {
                showAgreement = true
                return@PhoneLoginArea
            }
            actioner(it)
        })
        LoginAgreement(
            Modifier.padding(horizontal = 24.dp),
            checked = agreementChecked,
            onCheckedChange = { agreementChecked = it },
        )
        Spacer(Modifier.weight(1f))
        LoginButtonsArea(actioner = {
            if (agreementChecked) {
                actioner(it)
            } else {
                showAgreement = true
            }
        })
        Spacer(Modifier.weight(1f))
    }

    if (showAgreement) {
        AgreementDialog(
            onAgree = {
                agreementChecked = true
                showAgreement = false
            },
            onRefuse = { showAgreement = false },
        )
    }
}

@Composable
private fun PhoneLoginArea(actioner: (LoginUiAction) -> Unit) {
    var phone by remember { mutableStateOf("") }
    var ccode by remember { mutableStateOf(Constants.DEFAULT_CALLING_CODE) }
    var code by remember { mutableStateOf("") }

    PhoneAndCodeArea(
        phone = phone,
        onPhoneChange = { phone = it },
        code = code,
        onCodeChange = { code = it },
        callingCode = ccode,
        onCallingCodeChange = { ccode = it },
        onSendCode = {
            actioner(LoginUiAction.PhoneSendCode("$ccode$phone"))
        },
    )
    Box(Modifier.padding(16.dp)) {
        FlatPrimaryTextButton(
            stringResource(id = R.string.login_sign_in_or_up),
            enabled = phone.isValidPhone() && code.isValidSmsCode(),
        ) {
            actioner(LoginUiAction.PhoneLogin("$ccode$phone", code))
        }
    }
}

@Composable
private fun LoginButtonsArea(
    actioner: (LoginUiAction) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
                .height(0.5.dp)
                .background(Gray_1)
        )
        FlatTextBodyOneSecondary(stringResource(id = R.string.login_others_tip))
        Spacer(
            Modifier
                .padding(horizontal = 16.dp)
                .weight(1f)
                .height(0.5.dp)
                .background(Gray_1)
        )
    }
    Spacer(Modifier.height(32.dp))
    Row {
        LoginImageButton(onClick = { actioner(LoginUiAction.WeChatLogin) }) {
            Image(painterResource(R.drawable.ic_wechat_login), "")
        }
        Spacer(Modifier.width(48.dp))
        LoginImageButton(onClick = { actioner(LoginUiAction.GithubLogin) }) {
            Image(painterResource(R.drawable.ic_github_login), "")
        }
    }
}

@Composable
private fun LoginSlogan() {
    val context = LocalContext.current

    Column(
        Modifier.pointerInput(Unit) {
            detectTapGestures(
                onLongPress = {
                    if (context.isApkInDebug()) Navigator.launchDevSettingsActivity(context)
                },
            )
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FlatTextTitle(stringResource(R.string.login_welcome))
        Spacer(Modifier.height(4.dp))
        FlatTextBodyOne(stringResource(R.string.login_page_label_1))
    }
}

@Composable
private fun LoginAgreement(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(4.dp))
        val items = listOf(
            ClickableItem(stringResource(R.string.privacy_policy), "privacy", Constants.URL.Privacy),
            ClickableItem(stringResource(R.string.term_of_service), "service", Constants.URL.Service)
        )
        FlatClickableText(stringResource(R.string.login_agreement_message), items = items)
    }
}

@Composable
private fun LoginImageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(
        modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 36.dp),
                onClick = onClick,
            )
            .size(48.dp),
        Alignment.Center,
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(
            LocalContentAlpha provides contentAlpha,
            content = content,
        )
    }
}

@Composable
@Preview(device = PIXEL_C, widthDp = 800, heightDp = 600)
@Preview(widthDp = 400, heightDp = 600)
private fun LoginPagePreview() {
    LoginPage { }
}