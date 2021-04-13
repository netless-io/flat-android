package link.netless.flat.ui.activity

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
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import link.netless.flat.Constants
import link.netless.flat.R
import link.netless.flat.common.Navigator
import link.netless.flat.ui.activity.ui.theme.FlatColorGray
import link.netless.flat.ui.activity.ui.theme.FlatColorTextPrimary
import link.netless.flat.ui.activity.ui.theme.FlatCommonTextStyle
import link.netless.flat.ui.activity.ui.theme.Shapes
import link.netless.flat.ui.compose.FlatPage
import link.netless.flat.ui.viewmodel.UserViewModel


@AndroidEntryPoint
class LoginActivity : ComponentActivity() {
    private val userViewModel: UserViewModel by viewModels()
    private lateinit var api: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatPage {
                LoginContent { action ->
                    when (action) {
                        LoginUIAction.WeChatLogin -> callWeChatLogin()
                    }
                }
            }
        }

        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, false)
    }

    override fun onResume() {
        super.onResume()
        if (userViewModel.isLoggedIn()) {
            Navigator.launchHomeActivity(this)
        }
    }

    private fun callWeChatLogin() {
        val req = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "wechat_sdk_flat"
        }
        api.sendReq(req)
    }
}

@Composable
private fun LoginContent(actioner: (LoginUIAction) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(120.dp))
        Image(
            painter = painterResource(id = R.drawable.ic_flat_logo),
            contentDescription = null
        )
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 32.dp)) {
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
}