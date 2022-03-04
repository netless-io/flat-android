package io.agora.flat.ui.activity.setting

import android.os.Bundle
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
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextTitle
import io.agora.flat.ui.theme.FlatColorBlue
import io.agora.flat.ui.theme.FlatColorGray
import io.agora.flat.ui.theme.MaxWidth
import io.agora.flat.ui.theme.MaxWidthSpread
import io.agora.flat.util.getAppVersion

class AboutUsActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AboutUsPage { action ->
                when (action) {
                    is AboutUiAction.Back -> finish()
                    is AboutUiAction.OpenServiceProtocol -> {
                        Navigator.launchWebViewActivity(this@AboutUsActivity, Constants.URL.Service)
                    }
                    is AboutUiAction.OpenPrivacyProtocol -> {
                        Navigator.launchWebViewActivity(this@AboutUsActivity, Constants.URL.Privacy)
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutUsPage(actioner: (AboutUiAction) -> Unit) {
    val context = LocalContext.current
    val version = context.getAppVersion()

    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_about_us), { actioner(AboutUiAction.Back) })
        Box(MaxWidthSpread, contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.ic_flat_logo), null)
                Spacer(Modifier.height(16.dp))
                FlatTextTitle("Flat")
                Spacer(Modifier.height(8.dp))
                FlatTextBodyOne("Version $version")
            }
        }
        Box(MaxWidth.padding(vertical = 32.dp), Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = { actioner(AboutUiAction.OpenServiceProtocol) }) {
                    Text(stringResource(id = R.string.service_agreement), style = ProtocolTextStyle)
                }
                Spacer(
                    Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(FlatColorGray)
                )
                TextButton(
                    onClick = { actioner(AboutUiAction.OpenPrivacyProtocol) },
                    modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(stringResource(R.string.privacy_agreement), style = ProtocolTextStyle)
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