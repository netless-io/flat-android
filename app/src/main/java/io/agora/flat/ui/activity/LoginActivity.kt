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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
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
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.LocalIsPadMode
import io.agora.flat.ui.theme.FlatCommonTextStyle
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
    FlatPage(statusBarColor = Transparent) {
        val isPad = LocalIsPadMode.current
        if (isPad) {
            LoginMainPad(actioner)
        } else {
            LoginMain(actioner)
        }
    }
}

@Composable
internal fun LoginMain(actioner: (LoginUIAction) -> Unit) {
    Column(Modifier
        .fillMaxSize()
        .navigationBarsPadding(), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(120.dp))
        Image(painterResource(R.drawable.img_login_logo), null)
        Spacer(Modifier.height(2.dp))
        Text("Flat", style = MaterialTheme.typography.h5)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.login_page_label_1), style = FlatCommonTextStyle)
        Spacer(Modifier.weight(1f))
        Row {
            LoginImageButton(onClick = { actioner(LoginUIAction.WeChatLogin) }) {
                Image(painterResource(R.drawable.ic_wechat_login), "")
            }
            Spacer(Modifier.width(48.dp))
            LoginImageButton(onClick = { actioner(LoginUIAction.GithubLogin) }) {
                Image(painterResource(R.drawable.ic_github_login), "")
            }
        }
        Spacer(modifier = Modifier.height(100.dp))
        Box(Modifier.padding(vertical = 24.dp)) {
            Text(stringResource(R.string.login_page_label_2), style = FlatCommonTextStyle)
        }
    }
}

@Composable
internal fun LoginMainPad(actioner: (LoginUIAction) -> Unit) {
    Row {
        Image(
            painterResource(R.drawable.img_pad_login),
            contentDescription = null,
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            contentScale = ContentScale.Crop,
        )

        Column(
            Modifier
                .weight(1f)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            Image(painterResource(R.drawable.img_login_logo), null)
            Spacer(Modifier.height(2.dp))
            Text("Flat", style = MaterialTheme.typography.h5)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.login_page_label_1), style = FlatCommonTextStyle)
            Spacer(Modifier.height(48.dp))
            Row {
                LoginImageButton(onClick = { actioner(LoginUIAction.WeChatLogin) }) {
                    Image(painterResource(R.drawable.ic_wechat_login), "")
                }
                Spacer(Modifier.width(48.dp))
                LoginImageButton(onClick = { actioner(LoginUIAction.GithubLogin) }) {
                    Image(painterResource(R.drawable.ic_github_login), "")
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(Modifier
                .padding(vertical = 40.dp)
                .navigationBarsPadding()) {
                Text(stringResource(R.string.login_page_label_2), style = FlatCommonTextStyle)
            }
        }
    }
}

@Composable
private fun LoginImageButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    Box(modifier.clickable(
        interactionSource = remember { MutableInteractionSource() },
        indication = rememberRipple(bounded = false, radius = 48.dp),
        onClick = onClick,
    ), Alignment.Center) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha, content = content)
    }
}

@Composable
@Preview
private fun LoginPagePreview() {
    FlatPage {
        LoginPage { }
    }
}

@Composable
@Preview(device = PIXEL_C)
private fun LoginPagePreviewPad() {
    FlatPage {
        LoginPage { }
    }
}


sealed class LoginUIAction {
    object WeChatLogin : LoginUIAction()
    object GithubLogin : LoginUIAction()
}