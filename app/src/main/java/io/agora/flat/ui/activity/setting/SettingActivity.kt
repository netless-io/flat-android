package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NavigateNext
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatHighlightTextButton
import io.agora.flat.ui.theme.FlatCommonTextStyle
import io.agora.flat.ui.theme.FlatCommonTipTextStyle
import io.agora.flat.ui.viewmodel.UserViewModel
import io.agora.flat.util.getAppVersion
import io.agora.flat.util.isApkInDebug

@AndroidEntryPoint
class SettingActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel = viewModel(UserViewModel::class.java)

            FlatColumnPage {
                BackTopAppBar(title = stringResource(R.string.title_setting), onBackPressed = { finish() })
                Box(Modifier.weight(1f)) {
                    SettingItemList()
                    BottomOptArea(onLogoutClick = {
                        viewModel.logout()
                        Navigator.launchHomeActivity(this@SettingActivity)
                    })
                }
            }
        }
    }
}

@Composable
private fun SettingItemList() {
    val context = LocalContext.current

    LazyColumn {
        item {
            SettingItem(
                id = R.drawable.ic_user_profile_update,
                tip = stringResource(R.string.setting_check_update),
                desc = context.getAppVersion()
            )
            Divider(SettingDividerModifier)
            SettingItem(
                id = R.drawable.ic_user_profile_aboutus,
                tip = stringResource(R.string.title_feedback),
                onClick = { Navigator.launchFeedbackActivity(context) })
            Divider(SettingDividerModifier)
            SettingItem(
                id = R.drawable.ic_user_profile_feedback,
                tip = stringResource(R.string.title_about_us),
                onClick = { Navigator.launchAboutUsActivity(context) })
            Divider(SettingDividerModifier)
            if (context.isApkInDebug()) {
                SettingItem(
                    id = R.drawable.ic_user_profile_feedback,
                    tip = "Debug Tools",
                    onClick = { Navigator.launchDevToolsActivity(context) })
            }
        }
    }
}

@Composable
private fun BoxScope.BottomOptArea(onLogoutClick: () -> Unit) {
    Box(BottomOptBoxModifier) {
        FlatHighlightTextButton("退出登录", icon = R.drawable.ic_login_out, onClick = { onLogoutClick() })
    }
}

@Composable
internal fun SettingItem(@DrawableRes id: Int, tip: String, desc: String = "", onClick: () -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))
        Image(painterResource(id), contentDescription = null)
        Spacer(Modifier.width(4.dp))
        Text(text = tip, style = FlatCommonTextStyle)
        Spacer(Modifier.weight(1f))
        Text(text = desc, style = FlatCommonTipTextStyle)
        Spacer(Modifier.width(8.dp))
        Icon(Icons.Outlined.NavigateNext, contentDescription = null)
        Spacer(Modifier.width(16.dp))
    }
}

internal val SettingDividerModifier = Modifier.padding(start = 44.dp, end = 16.dp)

private val BoxScope.BottomOptBoxModifier
    get() = Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 32.dp)

@Preview(showSystemUi = false)
@Composable
fun UserSettingActivityPreview() {
    FlatColumnPage() {
        BackTopAppBar(title = stringResource(R.string.title_setting), onBackPressed = { })
        Box(Modifier.weight(1f)) {
            SettingItemList()
            BottomOptArea {}
        }
    }
}