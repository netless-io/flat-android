package io.agora.flat.ui.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.ui.theme.FlatColorGray
import io.agora.flat.ui.activity.ui.theme.FlatColorTextPrimary
import io.agora.flat.ui.activity.ui.theme.FlatCommonTextStyle
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.ui.viewmodel.UserViewModel
import io.agora.flat.util.showToast
import kotlinx.coroutines.launch

@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    companion object {
        const val LOGIN_WECHAT = 1
        const val LOGIN_GITHUB = 2
    }

    private val userViewModel: UserViewModel by viewModels()
    private lateinit var api: IWXAPI

    // TODO
    private var currentLogin = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatPage {
                LoginContent { action ->
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
            }
        }

        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, false)
    }

    override fun onResume() {
        super.onResume()
        // TODO 暂时不抽取
        when (currentLogin) {
            LOGIN_WECHAT -> {
                if (userViewModel.isLoggedIn()) {
                    loginSuccess()
                }
            }
            LOGIN_GITHUB -> {
                lifecycleScope.launch {
                    if (userViewModel.loginProcess()) {
                        loginSuccess()
                    }
                }
            }
        }
    }

    private fun loginSuccess() {
        Navigator.launchHomeActivity(this)
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
            data = Uri.parse(userViewModel.githubLoginUrl())
        }
        if (intent.resolveActivity(packageManager) != null) {
            startActivity(Intent.createChooser(intent, "Choose Browser"))
        } else {
            showToast("链接错误或无浏览器")
        }
    }

    private fun loginAfterSetAuthUUID(call: () -> Unit) {
        lifecycleScope.launch {
            if (userViewModel.loginSetAuthUUID()) {
                call()
            } else {
                showToast("set auth uuid error")
            }
        }
    }
}

@Composable
private fun LoginContent(actioner: (LoginUIAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))
        Image(painterResource(R.drawable.img_login_logo), null)
        Box(modifier = Modifier.padding(vertical = 24.dp)) {
            Text(text = "在线互动，让想法同步", style = FlatCommonTextStyle)
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
                shape = Shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FlatColorGray),
                onClick = { actioner(LoginUIAction.WeChatLogin) }) {
                Text(
                    "微信登录",
                    style = FlatCommonTextStyle,
                    color = FlatColorTextPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            OutlinedButton(modifier = Modifier
                .fillMaxWidth()
                .height(40.dp),
                shape = Shapes.small,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FlatColorGray),
                onClick = { actioner(LoginUIAction.GithubLogin) }) {
                Text(
                    "Github 登录",
                    style = FlatCommonTextStyle,
                    color = FlatColorTextPrimary
                )
            }
        }
        Box(modifier = Modifier.padding(vertical = 24.dp)) {
            Text(text = "powered by Agora", style = FlatCommonTextStyle)
        }
    }
}

@Composable
@Preview
private fun LoginPagePreview() {
    FlatPage {
        LoginContent {

        }
    }
}

sealed class LoginUIAction {
    object WeChatLogin : LoginUIAction()
    object GithubLogin : LoginUIAction()
}