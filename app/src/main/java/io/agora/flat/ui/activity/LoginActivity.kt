package io.agora.flat.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.tooling.preview.Devices.PIXEL_C
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.accompanist.insets.navigationBarsPadding
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.Constants.Login.AUTH_ERROR
import io.agora.flat.Constants.Login.AUTH_SUCCESS
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.ui.viewmodel.LoginViewModel
import io.agora.flat.util.showToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : BaseComposeActivity() {
    companion object {
        const val LOGIN_WECHAT = 1
        const val LOGIN_GITHUB = 2
    }

    private val viewModel: LoginViewModel by viewModels()
    private lateinit var api: IWXAPI

    private var currentLogin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val actioner: (LoginUIAction) -> Unit = { action ->
                when (action) {
                    LoginUIAction.WeChatLogin -> {
                        currentLogin = LOGIN_WECHAT
                        loginAfterSetAuthUUID(::callWeChatLogin)
                    }
                    LoginUIAction.GithubLogin -> {
                        currentLogin = LOGIN_GITHUB
                        loginAfterSetAuthUUID(::callGithubLogin)
                    }
                    LoginUIAction.OpenServiceProtocol -> {
                        Navigator.launchWebViewActivity(this, Constants.URL.Service)
                    }
                    LoginUIAction.OpenPrivacyProtocol -> {
                        Navigator.launchWebViewActivity(this, Constants.URL.Privacy)
                    }
                    LoginUIAction.AgreementHint -> {
                        this.showToast(R.string.login_agreement_unchecked_hint)
                    }
                }
            }
            LoginPage(actioner = actioner)
        }

        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, false)
    }

    override fun onResume() {
        super.onResume()
        handleLoginResult()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun handleLoginResult() {
        when (currentLogin) {
            LOGIN_WECHAT -> {
                if (!intent.hasExtra(Constants.Login.KEY_LOGIN_STATE)) {
                    return
                }
                val state = intent.getIntExtra(Constants.Login.KEY_LOGIN_STATE, AUTH_ERROR)
                if (state != AUTH_SUCCESS) {
                    showToast(R.string.login_fail)
                    return
                }
                lifecycleScope.launch {
                    val code = intent.getStringExtra(Constants.Login.KEY_LOGIN_RESP) ?: ""
                    if (viewModel.loginWeChatCallback(code)) {
                        loginSuccess()
                    } else {
                        showToast(R.string.login_fail)
                    }
                }
            }
            LOGIN_GITHUB -> {
                if (intent.data?.scheme != "x-agora-flat-client") {
                    return
                }
                lifecycleScope.launch {
                    if (viewModel.loginProcess()) {
                        loginSuccess()
                    } else {
                        showToast(R.string.login_fail)
                    }
                }
            }
            else -> {
                // login for url
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
    }

    private fun loginSuccess() {
        lifecycleScope.launch {
            showToast(R.string.login_success_and_jump)
            delay(2000)
            Navigator.launchHomeActivity(this@LoginActivity)
        }
    }

    private fun callWeChatLogin() {
        val req = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "wechat_sdk_flat"
        }
        api.sendReq(req)
    }

    private fun callGithubLogin() {
        val intent = Intent().apply {
            action = Intent.ACTION_VIEW
            data = Uri.parse(viewModel.githubLoginUrl())
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, getString(R.string.login_github_browser_choose_title)))
        } else {
            showToast(R.string.login_github_no_browser)
        }
    }

    private fun loginAfterSetAuthUUID(call: () -> Unit) {
        lifecycleScope.launch {
            if (viewModel.loginSetAuthUUID()) {
                call()
            } else {
                showToast(R.string.login_set_auth_uuid_error)
            }
        }
    }
}

@Composable
internal fun LoginPage(actioner: (LoginUIAction) -> Unit) {
    // var globalAgree by remember { mutableStateOf(false) }

    FlatPage(statusBarColor = Transparent) {
        if (isTabletMode()) {
            LoginMainPad(actioner)
        } else {
            LoginMain(actioner)
        }
    }
}

@Composable
internal fun LoginMain(actioner: (LoginUIAction) -> Unit) {
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
            Text(stringResource(R.string.login_page_label_2), style = FlatCommonTextStyle)
        }
    }
}

@Composable
internal fun LoginMainPad(actioner: (LoginUIAction) -> Unit) {
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
                Text(stringResource(R.string.login_page_label_2), style = FlatCommonTextStyle)
            }
        }
    }
}

@Composable
private fun LoginButtonsArea(
    agreementEnable: Boolean,
    onAgree: () -> Unit,
    actioner: (LoginUIAction) -> Unit,
) {
    var showAgreement by remember { mutableStateOf(false) }

    Row {
        LoginImageButton(onClick = {
            if (agreementEnable) {
                actioner(LoginUIAction.WeChatLogin)
            } else {
                showAgreement = true
            }
        }) {
            Image(painterResource(R.drawable.ic_wechat_login), "")
        }
        Spacer(Modifier.width(48.dp))
        LoginImageButton(onClick = {
            if (agreementEnable) {
                actioner(LoginUIAction.GithubLogin)
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
    Image(painterResource(R.drawable.img_login_logo), null)
    Spacer(Modifier.height(2.dp))
    Text("Flat", style = MaterialTheme.typography.h5)
    Spacer(Modifier.height(4.dp))
    Text(stringResource(R.string.login_page_label_1), style = FlatCommonTextStyle)
}

@Composable
private fun LoginAgreement(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    actioner: (LoginUIAction) -> Unit,
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

sealed class LoginUIAction {
    object WeChatLogin : LoginUIAction()
    object GithubLogin : LoginUIAction()
    object AgreementHint : LoginUIAction()

    object OpenServiceProtocol : LoginUIAction()
    object OpenPrivacyProtocol : LoginUIAction()
}