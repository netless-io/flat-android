package io.agora.flat.ui.activity.setting

import android.app.Activity
import android.os.Bundle
import android.os.CountDownTimer
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Checkbox
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Config
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.ui.viewmodel.AccountSecurityUiState
import io.agora.flat.ui.viewmodel.AccountSecurityViewModel
import io.agora.flat.util.showToast

@AndroidEntryPoint
class AccountSecurityActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AccountSecurityScreen()
        }
    }
}

@Composable
private fun AccountSecurityScreen(viewModel: AccountSecurityViewModel = hiltViewModel()) {
    val viewState by viewModel.state.collectAsState()

    AccountSecurityScreen(viewState = viewState)
}

@Composable
private fun AccountSecurityScreen(viewState: AccountSecurityUiState) {
    var showCancelDialog by rememberSaveable { mutableStateOf(false) }
    val activity = LocalContext.current as Activity
    var detail: String? = null
    if (viewState.roomCount != 0) {
        detail = stringResource(R.string.account_security_cancel_limited_tip, viewState.roomCount)
    }

    LaunchedEffect(viewState) {
        if (viewState.deleteAccount) {
            Navigator.launchHomeActivity(activity)
        }
        viewState.uiMessage?.let {
            activity.showToast(it.text)
        }
    }

    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.account_security), {
            activity.finish()
        })
        SettingItem(
            id = R.drawable.ic_settings_close_account,
            tip = stringResource(R.string.cancel_account),
            detail = detail,
            enabled = viewState.roomCount == 0,
            onClick = {
                showCancelDialog = true
            }
        )
    }
    if (showCancelDialog) {
        CancelAccountDialog(onDismissRequest = {
            showCancelDialog = false
        })
    }
}


@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun CancelAccountDialog(onDismissRequest: () -> Unit, viewModel: AccountSecurityViewModel = hiltViewModel()) {
    val scrollState = rememberScrollState()
    FlatAndroidTheme {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(Modifier.widthIn(max = 400.dp), shape = Shapes.large) {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Spacer(Modifier.height(12.dp))
                    FlatTextTitle(stringResource(R.string.account_security_cancel_dialog_title))

                    Spacer(Modifier.height(12.dp))
                    FlatTextBodyOne(
                        stringResource(R.string.account_security_cancel_dialog_message),
                        modifier = Modifier
                            .heightIn(max = 400.dp)
                            .verticalScroll(scrollState)
                    )
                    Spacer(NormalVerticalModifier)
                    CancelAccountCheck(onAgree = {
                        viewModel.deleteAccount()
                    })
                }
            }
        }
    }
}

@Composable
fun CancelAccountCheck(onAgree: () -> Unit) {
    var agree by remember { mutableStateOf(false) }

    var remainTime by remember { mutableStateOf(0L) }
    val countDownTimer = remember {
        object : CountDownTimer(Config.cancelAccountCountTime, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                remainTime = millisUntilFinished / 1000
            }

            override fun onFinish() {
                remainTime = 0
            }
        }
    }
    val buttonText = if (remainTime != 0L) {
        "${stringResource(R.string.account_cancel_and_exit)}($remainTime)"
    } else {
        stringResource(R.string.account_cancel_and_exit)
    }
    val buttonEnable = agree && remainTime == 0L

    Row(Modifier.padding(vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
            checked = agree,
            onCheckedChange = { agree = it }
        )
        Spacer(Modifier.width(4.dp))
        FlatTextBodyTwo(stringResource(R.string.account_cancel_agree_tip))
        Spacer(Modifier.weight(1f))
        CancelAgreeButton(
            text = buttonText,
            enabled = buttonEnable,
            onClick = {
                onAgree()
            },
        )
    }

    DisposableEffect(Unit) {
        countDownTimer.start()
        onDispose {
            countDownTimer.cancel()
        }
    }
}

@Composable
private fun CancelAgreeButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val darkMode = isDarkTheme()

    val border = if (enabled) BorderStroke(1.dp, Red_6) else null
    val colors = if (enabled) {
        ButtonDefaults.outlinedButtonColors(contentColor = Red_6)
    } else {
        ButtonDefaults.textButtonColors(
            backgroundColor = if (enabled) {
                if (darkMode) Blue_7 else Blue_6
            } else {
                if (darkMode) Gray_9 else Gray_2
            },
            contentColor = if (darkMode) Gray_0 else Gray_0,
            disabledContentColor = if (darkMode) Gray_7 else Gray_5
        )
    }

    Button(
        modifier = Modifier
            .defaultMinSize(minWidth = 86.dp)
            .height(40.dp),
        enabled = enabled,
        elevation = null,
        border = border,
        colors = colors,
        onClick = onClick,
    ) {
        FlatTextButton(text, softWrap = false, maxLines = 1)
    }
}

@Composable
@Preview
private fun AccountSecurityScreenPreview() {
    AccountSecurityScreen(AccountSecurityUiState(
        roomCount = 1,
        loading = true
    ))
}