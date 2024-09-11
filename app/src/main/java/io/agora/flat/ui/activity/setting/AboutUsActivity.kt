package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.android.ProtocolUrlManager
import io.agora.flat.data.AppEnv
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextBodyTwo
import io.agora.flat.ui.compose.FlatTextTitle
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.util.getAppVersion
import javax.inject.Inject

@AndroidEntryPoint
class AboutUsActivity : BaseComposeActivity() {
    @Inject
    lateinit var appEnv: AppEnv;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatPage {
                AboutUsScreen(showIcp = appEnv.showIcp) { action ->
                    when (action) {
                        is AboutUiAction.Back -> finish()
                        is AboutUiAction.OpenServiceProtocol -> {
                            Navigator.launchWebViewActivity(this@AboutUsActivity, ProtocolUrlManager.Service)
                        }

                        is AboutUiAction.OpenPrivacyProtocol -> {
                            Navigator.launchWebViewActivity(this@AboutUsActivity, ProtocolUrlManager.Privacy)
                        }

                        is AboutUiAction.OpenRegistration -> {
                            Navigator.launchWebViewActivity(this@AboutUsActivity, Constants.URL.Registration)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutUsScreen(showIcp: Boolean = true, actioner: (AboutUiAction) -> Unit) {
    val context = LocalContext.current
    val version = "Version ${context.getAppVersion()}"

    Column {
        BackTopAppBar(stringResource(R.string.title_about_us), { actioner(AboutUiAction.Back) })
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f), contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(painterResource(R.drawable.ic_flat_logo), null)
                Spacer(Modifier.height(16.dp))
                FlatTextTitle("Flat")
                Spacer(Modifier.height(8.dp))
                FlatTextBodyOne(version)
            }
        }

        Box(Modifier.fillMaxWidth(), Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AboutTextButton(
                    Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                    text = stringResource(R.string.service_agreement)
                ) {
                    actioner(AboutUiAction.OpenServiceProtocol)
                }
                Spacer(
                    Modifier
                        .height(16.dp)
                        .width(1.dp)
                        .background(FlatTheme.colors.divider)
                )
                AboutTextButton(
                    Modifier.padding(vertical = 4.dp, horizontal = 16.dp),
                    text = stringResource(R.string.privacy_agreement)
                ) {
                    actioner(AboutUiAction.OpenPrivacyProtocol)
                }
            }
        }
        if (showIcp) {
            Spacer(modifier = Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth(), Alignment.Center) {
                AboutTextButton(Modifier.padding(4.dp), text = stringResource(R.string.icp_registration_record)) {
                    actioner(AboutUiAction.OpenRegistration)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun AboutTextButton(
    modifier: Modifier = Modifier,
    text: String,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clickable(
                indication = LocalIndication.current,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick,
            )
    ) {
        FlatTextBodyTwo(text, color = MaterialTheme.colors.primary)
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun AboutUsPagePreview() {
    FlatPage {
        AboutUsScreen { }
    }
}

internal sealed class AboutUiAction {
    object Back : AboutUiAction()
    object OpenServiceProtocol : AboutUiAction()
    object OpenPrivacyProtocol : AboutUiAction()
    object OpenRegistration : AboutUiAction()
}