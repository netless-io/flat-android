package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.agora.flat.R
import io.agora.flat.ui.theme.FlatColorBlue
import io.agora.flat.ui.theme.FlatColorGray
import io.agora.flat.ui.theme.FlatCommonTextStyle
import io.agora.flat.ui.theme.FlatTitleTextStyle
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.util.getAppVersion
import io.agora.flat.util.showDebugToast

class AboutUsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AboutUsPage { action ->
                when (action) {
                    is AboutUiAction.Back -> finish()
                    is AboutUiAction.OpenServiceProtocol -> showDebugToast("OpenServiceProtocol")
                    is AboutUiAction.OpenPrivacyProtocol -> showDebugToast("OpenPrivacyProtocol")
                }
            }
        }
    }
}

@Composable
private fun AboutUsPage(actioner: (AboutUiAction) -> Unit) {
    var context = LocalContext.current
    var version = context.getAppVersion()

    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_about_us), { actioner(AboutUiAction.Back) });
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.ic_flat_logo),
                    contentDescription = null
                )
                Spacer(Modifier.height(16.dp))
                Text(text = "Flat", style = FlatTitleTextStyle)
                Spacer(Modifier.height(8.dp))
                Text(text = "Version $version", style = FlatCommonTextStyle)
            }
        }
        Box(
            modifier = Modifier
                .padding(vertical = 32.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { actioner(AboutUiAction.OpenServiceProtocol) }) {
                    Text(text = "服务协议", style = ProtocolTextStyle)
                }
                Spacer(
                    Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(FlatColorGray)
                )
                TextButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { actioner(AboutUiAction.OpenPrivacyProtocol) }) {
                    Text(text = "隐私协议", style = ProtocolTextStyle)
                }
            }
        }
    }
}

@Composable
@Preview
private fun AboutUsPagePreview() {
    AboutUsPage { }
}

private val ProtocolTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    color = FlatColorBlue,
)

internal sealed class AboutUiAction {
    object Back : AboutUiAction()
    object OpenServiceProtocol : AboutUiAction()
    object OpenPrivacyProtocol : AboutUiAction()
}