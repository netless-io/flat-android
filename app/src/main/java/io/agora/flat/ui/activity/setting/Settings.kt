package io.agora.flat.ui.activity.setting

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
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

@Composable
fun Settings(navController: NavController, viewModel: UserViewModel = hiltViewModel()) {
    val context = LocalContext.current

    Column {
        BackTopAppBar(
            title = stringResource(R.string.title_setting),
            onBackPressed = { navController.popBackStack() }
        )
        Box(Modifier.weight(1f)) {
            SettingItemList()
            BottomOptArea(onLogoutClick = {
                viewModel.logout()
                Navigator.launchHomeActivity(context)
            })
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
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_user_profile_aboutus,
                tip = stringResource(R.string.title_feedback),
                onClick = { Navigator.launchFeedbackActivity(context) })
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_user_profile_feedback,
                tip = stringResource(R.string.title_about_us),
                onClick = { Navigator.launchAboutUsActivity(context) })
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_user_profile_feedback,
                tip = stringResource(R.string.title_language),
                onClick = { Navigator.launchLanguageActivity(context) })
            SettingItemDivider()
            // Device Test
            // SettingItem(
            //     id = R.drawable.ic_user_profile_feedback,
            //     tip = stringResource(R.string.title_call_test),
            //     onClick = { Navigator.launchCallTestActivity(context) })
            // SettingItemDivider()
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
    Box(Modifier
        .align(Alignment.BottomCenter)
        .padding(horizontal = 16.dp, vertical = 32.dp)) {
        FlatHighlightTextButton(
            stringResource(R.string.login_exit),
            icon = R.drawable.ic_login_out,
            onClick = { onLogoutClick() }
        )
    }
}

@Composable
internal fun SettingItem(@DrawableRes id: Int, tip: String, desc: String = "", onClick: () -> Unit = {}) {
    Row(
        Modifier
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

@Composable
internal fun SettingItemDivider() {
    Divider(Modifier.padding(start = 44.dp, end = 16.dp))
}

@Preview(showSystemUi = false)
@Composable
fun UserSettingActivityPreview() {
    FlatColumnPage {
        BackTopAppBar(title = stringResource(R.string.title_setting), onBackPressed = { })
        Box(Modifier.weight(1f)) {
            SettingItemList()
            BottomOptArea {}
        }
    }
}