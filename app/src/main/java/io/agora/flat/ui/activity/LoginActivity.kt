package io.agora.flat.ui.activity

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices.PIXEL_C
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.navigationBarsPadding
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.login.LoginActivityHandler
import io.agora.flat.common.login.LoginState
import io.agora.flat.common.login.LoginType
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.activity.login.LoginUiAction
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.MaxHeightSpread
import io.agora.flat.ui.theme.isTabletMode
import io.agora.flat.ui.viewmodel.LoginViewModel
import io.agora.flat.util.showToast
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
            // val viewState by viewModel.state.collectAsState()
            val loginState by loginHandler.observeLoginState().collectAsState()

            val actioner: (LoginUiAction) -> Unit = { action ->
                when (action) {
                    LoginUiAction.WeChatLogin -> {
                        loginHandler.loginWithType(LoginType.WeChat);
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
                }
            }

            LaunchedEffect(loginState) {
                when (loginState) {
                    LoginState.Init -> {}
                    is LoginState.Process -> showToast((loginState as LoginState.Process).message)
                    LoginState.Success -> {
                        showToast(R.string.login_success_and_jump)
                        delay(2000)
                        Navigator.launchHomeActivity(this@LoginActivity)
                    }
                }
            }

            LoginPage(actioner = actioner)
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
            if (intent.data?.scheme == "x-agora-flat-client" &&
                intent.data?.authority == "joinRoom"
            ) {
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
    var agreementChecked by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(120.dp))
        LoginLogoDisplay()
        Spacer(Modifier.weight(1f))
        LoginButtonsArea(agreementChecked, onAgree = { agreementChecked = true }, actioner)
        Spacer(modifier = Modifier.height(100.dp))
        LoginAgreement(
            Modifier.padding(horizontal = 24.dp),
            checked = agreementChecked,
            onCheckedChange = { agreementChecked = it },
            actioner = actioner
        )
        Box(Modifier.padding(vertical = 24.dp)) {
            FlatTextBodyOne(stringResource(R.string.login_page_label_2))
        }
    }
}

@Composable
internal fun LoginMainPad(actioner: (LoginUiAction) -> Unit) {
    var agreementEnable by remember { mutableStateOf(false) }

    Row {
        Image(
            painterResource(R.drawable.img_pad_login),
            contentDescription = null,
            MaxHeightSpread,
            contentScale = ContentScale.Crop,
        )

        Column(MaxHeightSpread, horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(modifier = Modifier.weight(1f))
            LoginLogoDisplay()
            Spacer(Modifier.height(48.dp))
            LoginButtonsArea(agreementEnable, onAgree = { agreementEnable = true }, actioner)
            Spacer(modifier = Modifier.weight(1f))
            LoginAgreement(
                Modifier.padding(horizontal = 24.dp),
                checked = agreementEnable,
                onCheckedChange = { agreementEnable = it },
                actioner = actioner
            )
            Box(Modifier
                .padding(vertical = 24.dp)
                .navigationBarsPadding()) {
                FlatTextBodyOne(stringResource(R.string.login_page_label_2))
            }
        }
    }
}

@Composable
private fun LoginButtonsArea(
    agreementEnable: Boolean,
    onAgree: () -> Unit,
    actioner: (LoginUiAction) -> Unit,
) {
    var showAgreement by rememberSaveable { mutableStateOf(false) }

    Row {
        LoginImageButton(onClick = {
            if (agreementEnable) {
                actioner(LoginUiAction.WeChatLogin)
            } else {
                showAgreement = true
            }
        }) {
            Image(painterResource(R.drawable.ic_wechat_login), "")
        }
        Spacer(Modifier.width(48.dp))
        LoginImageButton(onClick = {
            if (agreementEnable) {
                actioner(LoginUiAction.GithubLogin)
            } else {
                showAgreement = true
            }
        }) {
            Image(painterResource(R.drawable.ic_github_login), "")
        }
    }
    if (showAgreement) {
        AgreementDialog(
            onAgree = {
                onAgree()
                showAgreement = false
            },
            onRefuse = { showAgreement = false },
        )
    }
}

@Composable
private fun LoginLogoDisplay() {
    Image(painterResource(R.drawable.ic_flat_logo), null)
    Spacer(Modifier.height(2.dp))
    FlatTextTitle("Flat")
    Spacer(Modifier.height(4.dp))
    FlatTextBodyOne(stringResource(R.string.login_page_label_1))
}

@Composable
private fun LoginAgreement(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    actioner: (LoginUiAction) -> Unit,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Spacer(Modifier.width(4.dp))
        val text = stringResource(R.string.login_agreement_message)
        val items = listOf(
            ClickableItem(stringResource(R.string.privacy_policy), "privacy", Constants.URL.Privacy),
            ClickableItem(stringResource(R.string.term_of_service), "service", Constants.URL.Service)
        )
        FlatClickableText(text = text, items = items)
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
        modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = rememberRipple(bounded = false, radius = 48.dp),
            onClick = onClick,
        ),
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
@Preview(device = PIXEL_C)
@Preview
private fun LoginAgreementPreview() {
    FlatPage {
        LoginAgreement(Modifier, true, {}) { }
    }
}

@Composable
@Preview(device = PIXEL_C)
@Preview
private fun LoginPagePreviewPad() {
    FlatPage {
        LoginPage { }
    }
}

@Composable
@Preview
private fun AgreementPreview() {
    FlatPage {
        AgreementDialog(onAgree = {}) {

        }
    }
}