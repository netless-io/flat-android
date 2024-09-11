package io.agora.flat.ui.activity.setting

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Switch
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.agora.flat.Constants
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatDivider
import io.agora.flat.ui.compose.FlatHighlightTextButton
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextCaption
import io.agora.flat.ui.compose.StreamCollectDialog
import io.agora.flat.ui.compose.UpdateDialog
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.viewmodel.SettingsUiState
import io.agora.flat.ui.viewmodel.SettingsViewModel
import io.agora.flat.util.getAppVersion
import io.agora.flat.util.isApkInDebug
import io.agora.flat.util.launchMarket

@Composable
fun SettingsScreen(navController: NavController, viewModel: SettingsViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    SettingsScreen(
        state = state,
        onDownload = viewModel::downloadApp,
        onAgreeStream = viewModel::setAgreeStream,
        onBack = { navController.popBackStack() },
        onLogout = {
            viewModel.logout()
            Navigator.launchHomeActivity(context)
        }
    )
}

@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onDownload: suspend () -> Uri,
    onAgreeStream: (Boolean) -> Unit,
    onBack: () -> Unit,
    onLogout: () -> Unit
) {
    Column {
        BackTopAppBar(
            title = stringResource(R.string.title_setting),
            onBackPressed = onBack
        )
        Box(Modifier.weight(1f)) {
            SettingsList(state, onDownload, onAgreeStream)
        }
        BottomOperateArea(onLogout = onLogout)
    }
}

@Composable
private fun SettingsList(state: SettingsUiState, onDownload: suspend () -> Uri, onAgreeStream: (Boolean) -> Unit) {
    val context = LocalContext.current
    var showUpdate by remember { mutableStateOf(false) }
    var showCollect by remember { mutableStateOf(false) }

    LazyColumn {
        item {
            SettingLabel(stringResource(R.string.account))
            SettingItem(
                id = R.drawable.ic_user_profile_head,
                tip = stringResource(R.string.title_my_profile),
                onClick = { Navigator.launchUserInfoActivity(context) }
            )
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_settings_security,
                tip = stringResource(R.string.account_security),
                onClick = { Navigator.launchAccountSecurityActivity(context) }
            )

            Spacer(Modifier.height(12.dp))
            SettingLabel(stringResource(R.string.preferences))
            SettingItem(
                id = R.drawable.ic_settings_language,
                tip = stringResource(R.string.title_language),
                onClick = { Navigator.launchLanguageActivity(context) })
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_settings_dark_light,
                tip = stringResource(R.string.title_dark_mode),
                onClick = { Navigator.launchDarkModeActivity(context) })
            Spacer(Modifier.height(12.dp))
            SettingLabel(stringResource(R.string.privacy))
            SettingItem(
                id = R.drawable.ic_settings_privacy_policy,
                tip = stringResource(R.string.privacy_policy),
                onClick = { Navigator.launchWebViewActivity(context, Constants.URL.Privacy) })
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_settings_term_of_service,
                tip = stringResource(R.string.term_of_service),
                onClick = { Navigator.launchWebViewActivity(context, Constants.URL.Service) })
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_settings_info_gathering,
                tip = stringResource(R.string.info_gathering),
                onClick = { Navigator.launchWebViewActivity(context, state.infoUrl) })
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_settings_info_third_party,
                tip = stringResource(R.string.info_third_party),
                onClick = { Navigator.launchWebViewActivity(context, Constants.URL.Libraries) })
            if (state.isAgreeStream != null) {
                SettingItem(
                    id = R.drawable.ic_settings_stream_analysis,
                    tip = stringResource(R.string.stream_data_analysis),
                    desc = if (state.isAgreeStream) stringResource(R.string.setting_on) else stringResource(R.string.setting_off),
                    onClick = { showCollect = true }
                )
                SettingItemDivider()
            }

            Spacer(Modifier.height(12.dp))
            SettingLabel(stringResource(R.string.more))
            SettingItem(
                id = R.drawable.ic_settings_app_version,
                tip = stringResource(R.string.setting_check_update),
                desc = context.getAppVersion(),
                onClick = {
                    if (state.versionCheckResult.showUpdate) {
                        showUpdate = true
                    }
                }
            )
            SettingItemDivider()
            SettingItem(
                id = R.drawable.ic_settings_about_us,
                tip = stringResource(R.string.title_about_us),
                onClick = { Navigator.launchAboutUsActivity(context) })
            SettingItemDivider()
            if (context.isApkInDebug()) {
                // Device Test
                // SettingItem(
                //     id = R.drawable.ic_user_profile_feedback,
                //     tip = stringResource(R.string.title_call_test),
                //     onClick = { Navigator.launchCallTestActivity(context) })
                // SettingItemDivider()
                SettingItem(
                    id = R.drawable.ic_settings_debug,
                    tip = "Debug Tools",
                    onClick = { Navigator.launchDevToolsActivity(context) })
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showUpdate) {
        UpdateDialog(
            versionCheckResult = state.versionCheckResult,
            downloadApp = onDownload,
            onCancel = {
                showUpdate = false
            },
            onGotoMarket = {
                showUpdate = false
                context.launchMarket()
            }
        )
    }

    if (showCollect) {
        StreamCollectDialog(
            onOpen = {
                onAgreeStream(true)
                showCollect = false
            },
            onClose = {
                onAgreeStream(false)
                showCollect = false
            },
            onCancel = {
                showCollect = false
            }
        )
    }
}

@Composable
private fun BottomOperateArea(onLogout: () -> Unit) {
    Box(
        Modifier
            .padding(horizontal = 16.dp, vertical = 32.dp)
    ) {
        FlatHighlightTextButton(
            stringResource(R.string.login_exit),
            icon = R.drawable.ic_login_out,
            onClick = { onLogout() }
        )
    }
}

@Composable
internal fun SettingItem(
    @DrawableRes id: Int = 0,
    tip: String,
    desc: String = "",
    detail: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(48.dp)
            .clickable(
                enabled = enabled,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(Modifier.width(16.dp))
        if (id != 0) {
            Image(painterResource(id), contentDescription = null)
            Spacer(Modifier.width(4.dp))
        }
        Column(
            Modifier
                .weight(1f)
                .padding(vertical = 8.dp)
        ) {
            FlatTextBodyOne(tip)
            if (detail != null) FlatTextCaption(text = detail)
        }
        Spacer(Modifier.width(8.dp))
        FlatTextBodyOne(desc, color = FlatTheme.colors.textSecondary)
        Spacer(Modifier.width(8.dp))
        Icon(painterResource(id = R.drawable.ic_arrow_right), contentDescription = null)
        Spacer(Modifier.width(16.dp))
    }
}

@Composable
internal fun SettingLabel(text: String) {
    Row(Modifier.padding(horizontal = 16.dp)) {
        FlatTextCaption(text)
    }
}

@Composable
internal fun SettingItemSwitch(
    @DrawableRes id: Int,
    tip: String,
    desc: String = "",
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colors.onBackground.copy(alpha = 1f),
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(Modifier.width(16.dp))
            Image(painterResource(id), contentDescription = null)
            Spacer(Modifier.width(4.dp))
            FlatTextBodyOne(tip)
            Spacer(Modifier.weight(1f))
            FlatTextBodyOne(desc, color = FlatTheme.colors.textSecondary)
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
            Spacer(Modifier.width(16.dp))
        }
    }
}

@Composable
internal fun SettingItemDivider() {
    FlatDivider(startIndent = 44.dp, endIndent = 16.dp)
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
@Preview(widthDp = 800, heightDp = 600, uiMode = 0x20)
fun UserSettingActivityPreview() {
    val state = SettingsUiState()
    FlatColumnPage {
        SettingsScreen(state, { Uri.EMPTY }, {}, {}, {})
    }
}