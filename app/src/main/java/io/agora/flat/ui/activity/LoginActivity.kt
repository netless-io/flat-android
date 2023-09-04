package io.agora.flat.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.TextButton
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.android.LanguageManager
import io.agora.flat.common.login.LoginActivityHandler
import io.agora.flat.common.login.LoginState
import io.agora.flat.common.login.LoginType
import io.agora.flat.data.LoginConfig
import io.agora.flat.data.model.PhoneOrEmailInfo
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.activity.login.LoginUiAction
import io.agora.flat.ui.activity.login.LoginUiState
import io.agora.flat.ui.activity.login.LoginViewModel
import io.agora.flat.ui.activity.password.PasswordResetDialog
import io.agora.flat.ui.activity.phone.PhoneBindDialog
import io.agora.flat.ui.activity.register.RegisterDialog
import io.agora.flat.ui.compose.AgreementDialog
import io.agora.flat.ui.compose.ClickableItem
import io.agora.flat.ui.compose.FlatClickableText
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatSecondaryTextButton
import io.agora.flat.ui.compose.FlatTextBodyOneSecondary
import io.agora.flat.ui.compose.FlatTextBodyTwo
import io.agora.flat.ui.compose.PasswordInput
import io.agora.flat.ui.compose.PhoneInput
import io.agora.flat.ui.compose.PhoneOrEmailInput
import io.agora.flat.ui.compose.SendCodeInput
import io.agora.flat.ui.theme.Gray_1
import io.agora.flat.ui.theme.isDarkTheme
import io.agora.flat.ui.theme.isTabletMode
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.util.isApkInDebug
import io.agora.flat.util.isPhoneMode
import io.agora.flat.util.isValidPhone
import io.agora.flat.util.isValidSmsCode
import io.agora.flat.util.showToast
import kotlinx.coroutines.delay
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
            val uiState by viewModel.state.collectAsState()

            val isPhoneMode = isPhoneMode()

            var showPhoneBind by remember { mutableStateOf(false) }
            var showRegister by remember { mutableStateOf(false) }
            var showForgotPwd by remember { mutableStateOf(false) }

            val actioner: (LoginUiAction) -> Unit = { action ->
                when (action) {
                    LoginUiAction.WeChatLogin -> {
                        loginHandler.loginWithType(LoginType.WeChat)
                    }

                    LoginUiAction.GithubLogin -> {
                        loginHandler.loginWithType(LoginType.Github)
                    }

                    LoginUiAction.GoogleLogin -> {
                        loginHandler.loginWithType(LoginType.Google)
                    }

                    LoginUiAction.OpenServiceProtocol -> {
                        Navigator.launchWebViewActivity(this, Constants.URL.Service)
                    }

                    LoginUiAction.OpenPrivacyProtocol -> {
                        Navigator.launchWebViewActivity(this, Constants.URL.Privacy)
                    }

                    is LoginUiAction.PhoneLogin -> {
                        loginHandler.loginWithPhone(action.phone, action.code)
                    }

                    is LoginUiAction.PhoneSendCode -> {
                        viewModel.sendPhoneCode(action.phone)
                    }

                    is LoginUiAction.SignUpClick -> {
                        if (isPhoneMode) {
                            Navigator.launchRegisterActivity(this)
                        } else {
                            showRegister = true
                        }
                    }

                    LoginUiAction.ForgotPwdClick -> {
                        if (isPhoneMode) {
                            Navigator.launchForgotPwdActivity(this)
                        } else {
                            showForgotPwd = true
                        }
                    }

                    is LoginUiAction.PasswordLoginClick -> {
                        viewModel.login(action.info)
                    }

                    is LoginUiAction.LoginInputChange -> {
                        viewModel.updateLoginInput(action.info)
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

            LaunchedEffect(uiState) {
                if (uiState.success) {
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

            ShowUiMessageEffect(uiMessage = uiState.message, onMessageShown = {
                viewModel.clearUiMessage(it)
            })

            LaunchedEffect(uiState) {
                if (uiState.sendCodeSuccess) {
                    showToast(R.string.message_code_send_success)
                    viewModel.clearSendCodeSuccess()
                }
            }

            // Disable press back to exit login page
            BackHandler {}

            LoginPage(uiState, actioner = actioner)

            if (showPhoneBind) {
                PhoneBindDialog(
                    onBindSuccess = {
                        Navigator.launchHomeActivity(this)
                    },
                    onDismissRequest = {
                        showPhoneBind = false
                    }
                )
            }

            if (showRegister) {
                RegisterDialog(
                    onRegisterSuccess = {
                        Navigator.launchHomeActivity(this)
                    },
                    onClose = {
                        showRegister = false
                    }
                )
            }

            if (showForgotPwd) {
                PasswordResetDialog(
                    onResetSuccess = {
                        showForgotPwd = false
                    },
                    onClose = {
                        showForgotPwd = false
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        intent?.let { loginHandler.handleResult(it) }
    }
}

@Composable
internal fun LoginPage(uiState: LoginUiState, actioner: (LoginUiAction) -> Unit) {
    FlatPage(statusBarColor = Transparent) {
        if (isTabletMode()) {
            LoginMainPad(uiState, actioner)
        } else {
            LoginSlogan()
            LoginMain(uiState, actioner)
        }
    }
}

@Composable
internal fun LoginMain(uiState: LoginUiState, actioner: (LoginUiAction) -> Unit) {
    LoginArea(uiState, Modifier.fillMaxSize(), actioner = actioner)
}

@Composable
internal fun LoginMainPad(uiState: LoginUiState, actioner: (LoginUiAction) -> Unit) {
    Row {
        LoginPadSlogan(
            Modifier
                .fillMaxHeight()
                .weight(1f)
        )
        Box(
            Modifier
                .fillMaxHeight()
                .weight(1f),
            contentAlignment = Alignment.TopCenter,
        ) {
            LoginArea(
                state = uiState, modifier = Modifier
                    .width(360.dp)
                    .fillMaxHeight(), actioner = actioner
            )
        }
    }
}

@Composable
private fun LoginArea(state: LoginUiState, modifier: Modifier, actioner: (LoginUiAction) -> Unit) {
    var agreementChecked by rememberSaveable { mutableStateOf(false) }
    var showAgreement by rememberSaveable { mutableStateOf(false) }
    var passwordMode by rememberSaveable { mutableStateOf(true) }
    val inputState = state.inputState

    Box(modifier) {
        Column(
            Modifier
                .align(Alignment.TopCenter)
                .padding(top = 200.dp)
        ) {
            if (passwordMode) {
                PasswordLoginArea(
                    inputState = inputState,
                    onLoginInputChange = { actioner(LoginUiAction.LoginInputChange(it)) },
                    onSmsClick = {
                        // clear email code when switch to phone mode
                        if (!inputState.value.isValidPhone()) {
                            actioner(LoginUiAction.LoginInputChange(inputState.copy(value = "")))
                        }
                        passwordMode = false
                    },
                    actioner = {
                        if (agreementChecked.not() && it is LoginUiAction.PasswordLoginClick) {
                            showAgreement = true
                            return@PasswordLoginArea
                        }
                        actioner(it)
                    }
                )
            } else {
                PhoneLoginArea(
                    inputState = inputState,
                    onLoginInputChange = { actioner(LoginUiAction.LoginInputChange(it)) },
                    actioner = {
                        if (it is LoginUiAction.PasswordLoginClick) {
                            passwordMode = true
                            return@PhoneLoginArea
                        }
                        if (agreementChecked.not() && it is LoginUiAction.PhoneLogin) {
                            showAgreement = true
                            return@PhoneLoginArea
                        }
                        actioner(it)
                    })
            }

            LoginAgreement(
                modifier = Modifier.padding(horizontal = 24.dp),
                checked = agreementChecked,
                onCheckedChange = { agreementChecked = it },
            )
        }

        LoginButtonsArea(
            loginConfig = state.loginConfig,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            actioner = {
                if (agreementChecked) {
                    actioner(it)
                } else {
                    showAgreement = true
                }
            })
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
private fun PasswordLoginArea(
    inputState: PhoneOrEmailInfo,
    onLoginInputChange: (PhoneOrEmailInfo) -> Unit,
    onSmsClick: () -> Unit,
    actioner: (LoginUiAction) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        PhoneOrEmailInput(
            value = inputState.value,
            onValueChange = { onLoginInputChange(inputState.copy(value = it)) },
            callingCode = inputState.cc,
            onCallingCodeChange = { onLoginInputChange(inputState.copy(cc = it)) },
            phoneFirst = inputState.phoneFirst,
        )
        PasswordInput(
            password = inputState.password,
            onPasswordChange = { onLoginInputChange(inputState.copy(password = it)) },
        )
    }

    Row(
        Modifier
            .fillMaxWidth(1f)
            .padding(horizontal = 8.dp)
    ) {
        TextButton(onClick = onSmsClick) {
            FlatTextBodyTwo(stringResource(R.string.login_use_sms_login), color = MaterialTheme.colors.primary)
        }
        Spacer(Modifier.weight(1f))
        TextButton(
            onClick = { actioner(LoginUiAction.ForgotPwdClick) },
        ) {
            FlatTextBodyTwo(stringResource(R.string.login_forgot_pwd), color = MaterialTheme.colors.primary)
        }
    }

    Spacer(modifier = Modifier.height(16.dp))
    Box(Modifier.padding(horizontal = 16.dp)) {
        FlatPrimaryTextButton(
            text = stringResource(id = R.string.login_sign_in),
            enabled = inputState.value.isNotEmpty() && inputState.password.isNotEmpty(),
        ) {
            actioner(LoginUiAction.PasswordLoginClick(inputState))
        }
    }
    Spacer(modifier = Modifier.height(8.dp))
    Box(Modifier.padding(horizontal = 16.dp)) {
        FlatSecondaryTextButton(text = stringResource(id = R.string.login_sign_up)) {
            actioner(LoginUiAction.SignUpClick)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
}

@Composable
private fun PhoneLoginArea(
    inputState: PhoneOrEmailInfo,
    onLoginInputChange: (PhoneOrEmailInfo) -> Unit,
    actioner: (LoginUiAction) -> Unit
) {
    Column(Modifier.padding(horizontal = 16.dp)) {
        PhoneInput(
            phone = inputState.value,
            onPhoneChange = { onLoginInputChange(inputState.copy(value = it)) },
            callingCode = inputState.cc,
            onCallingCodeChange = { onLoginInputChange(inputState.copy(cc = it)) },
        )

        SendCodeInput(
            code = inputState.code,
            onCodeChange = { onLoginInputChange(inputState.copy(code = it)) },
            onSendCode = { actioner(LoginUiAction.PhoneSendCode("${inputState.cc}${inputState.value}")) },
            ready = inputState.value.isValidPhone(),
            remainTime = inputState.remainTime,
        )
    }

    Row(
        Modifier
            .fillMaxWidth(1f)
            .padding(horizontal = 8.dp)
    ) {
        TextButton(
            onClick = { actioner(LoginUiAction.PasswordLoginClick(inputState)) }) {
            FlatTextBodyTwo(stringResource(R.string.login_use_password_login), color = MaterialTheme.colors.primary)
        }
        Spacer(Modifier.weight(1f))
    }

    Box(Modifier.padding(16.dp)) {
        FlatPrimaryTextButton(
            stringResource(id = R.string.login_sign_in_or_up),
            enabled = inputState.value.isValidPhone() && inputState.code.isValidSmsCode(),
        ) {
            actioner(LoginUiAction.PhoneLogin(inputState.cc + inputState.value, inputState.code))
        }
    }
}

@Composable
private fun LoginButtonsArea(
    loginConfig: LoginConfig,
    modifier: Modifier = Modifier,
    actioner: (LoginUiAction) -> Unit,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
        Spacer(Modifier.height(24.dp))
        Row {
            if (loginConfig.wechat) {
                Spacer(Modifier.width(24.dp))
                LoginImageButton(onClick = { actioner(LoginUiAction.WeChatLogin) }) {
                    Image(painterResource(R.drawable.ic_wechat_login), "")
                }
                Spacer(Modifier.width(24.dp))
            }
            if (loginConfig.github) {
                Spacer(Modifier.width(24.dp))
                LoginImageButton(onClick = { actioner(LoginUiAction.GithubLogin) }) {
                    Image(painterResource(R.drawable.ic_github_login), "")
                }
                Spacer(Modifier.width(24.dp))
            }
            if (loginConfig.google) {
                Spacer(Modifier.width(24.dp))
                LoginImageButton(onClick = { actioner(LoginUiAction.GoogleLogin) }) {
                    Image(painterResource(R.drawable.ic_google_login), "")
                }
                Spacer(Modifier.width(24.dp))
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun LoginSlogan(
    isDark: Boolean = isDarkTheme(),
    isZh: Boolean = LanguageManager.currentLocale().language == "zh"
) {
    val context = LocalContext.current
    val imgId = if (isZh) {
        if (isDark) R.drawable.img_login_slogan_dark_zh else R.drawable.img_login_slogan_light_zh
    } else {
        if (isDark) R.drawable.img_login_slogan_dark_en else R.drawable.img_login_slogan_light_en
    }

    Box(Modifier.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                if (context.isApkInDebug()) Navigator.launchDevSettingsActivity(context)
            },
        )
    }) {
        Image(
            painter = painterResource(imgId),
            modifier = Modifier.fillMaxWidth(),
            contentScale = ContentScale.FillWidth,
            contentDescription = null
        )
    }
}

@Composable
private fun LoginPadSlogan(
    modifier: Modifier = Modifier,
    isDark: Boolean = isDarkTheme(),
    izZh: Boolean = LanguageManager.currentLocale().language == "zh"
) {
    val context = LocalContext.current
    val imgId = if (izZh) {
        if (isDark) R.drawable.img_login_slogan_pad_dark_zh else R.drawable.img_login_slogan_pad_light_zh
    } else {
        if (isDark) R.drawable.img_login_slogan_pad_dark_en else R.drawable.img_login_slogan_pad_light_en
    }

    Box(modifier = modifier.pointerInput(Unit) {
        detectTapGestures(
            onLongPress = {
                if (context.isApkInDebug()) Navigator.launchDevSettingsActivity(context)
            },
        )
    }) {
        Image(
            painterResource(imgId),
            contentDescription = null,
            Modifier.fillMaxSize(),
            contentScale = ContentScale.FillBounds,
        )
    }
}

@Composable
fun LoginAgreement(
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
@Preview(widthDp = 480, heightDp = 700)
@Preview(widthDp = 400, heightDp = 800)
private fun LoginPagePreview() {
    LoginPage(
        LoginUiState(
            inputState = PhoneOrEmailInfo(
                value = "12345678901",
                password = "123456",
                code = "123456",
                cc = "+86",
            ),
        )
    ) { }
}