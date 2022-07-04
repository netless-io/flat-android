package io.agora.flat.ui.activity.dev

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Switch
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextBodyTwo
import io.agora.flat.ui.theme.MaxWidthSpread
import io.agora.flat.ui.viewmodel.UserViewModel
import io.agora.flat.util.showDebugToast
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.system.exitProcess

@AndroidEntryPoint
class DevToolsActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FlatColumnPage {
                BackTopAppBar(title = "DevTools", onBackPressed = { finish() })

                LazyColumn(MaxWidthSpread) {
                    item {
                        ClearLastCancelUpdate()
                        ProjectorEnableFlag()
                        UserLoginFlag()
                        MockEnableFlag()
                        EnvSwitch()
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
private fun EnvSwitch() {
    val context = LocalContext.current
    val appEnv = AppEnv(context)
    val curEnv = appEnv.getEnv()
    val flatServiceUrl = appEnv.flatServiceUrl
    val userViewModel: UserViewModel = viewModel()

    val scope = rememberCoroutineScope()
    var expanded by remember { mutableStateOf(false) }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(16.dp))
            FlatTextBodyOne(text = "current $curEnv $flatServiceUrl")
            Spacer(modifier = Modifier.width(16.dp))
        }
        AnimatedVisibility(visible = expanded) {
            Column {
                appEnv.envMap.forEach { (k, v) ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .clickable {
                                expanded = !expanded
                                if (k != curEnv) {
                                    appEnv.setEnv(k)

                                    scope.launch {
                                        (context as DevToolsActivity).showDebugToast("退出应用中...")
                                        userViewModel.logout()
                                        delay(2000)
                                        // exit application
                                        context.finishAffinity()
                                        android.os.Process.killProcess(android.os.Process.myPid())
                                        exitProcess(1)
                                    }
                                }
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Spacer(modifier = Modifier.width(16.dp))
                        FlatTextBodyOne(text = "$k ${v.serviceUrl}", modifier = Modifier.weight(1f))
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
fun UserLoginFlag() {
    val userViewModel: UserViewModel = viewModel()
    val loggedInData = userViewModel.loggedInData.observeAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyOne(text = "设置User")
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = loggedInData.value ?: false,
            enabled = loggedInData.value ?: true,
            onCheckedChange = { userViewModel.logout() })
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun MockEnableFlag() {
    val userViewModel: UserViewModel = viewModel()
    var mockEnable by remember { mutableStateOf(AppKVCenter.MockData.mockEnable) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyTwo(text = "登录Mock")
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = mockEnable,
            onCheckedChange = {
                mockEnable = it
                AppKVCenter.MockData.mockEnable = it
                userViewModel.logout()
            })
        Spacer(modifier = Modifier.width(16.dp))
    }
}


@Composable
fun ProjectorEnableFlag() {
    val context = LocalContext.current
    val appKVCenter = AppKVCenter(context)
    var useProjector by remember { mutableStateOf(appKVCenter.useProjectorConvertor()) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyTwo(text = "使用 Projector")
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            checked = useProjector,
            onCheckedChange = {
                useProjector = it
                appKVCenter.setUseProjectorConvertor(useProjector)
            })
        Spacer(modifier = Modifier.width(16.dp))
    }
}

@Composable
fun ClearLastCancelUpdate() {
    val context = LocalContext.current
    val appKVCenter = AppKVCenter(context)
    var lastCancelUpdate by remember { mutableStateOf(appKVCenter.getLastCancelUpdate()) }

    Row(
        Modifier
            .fillMaxWidth()
            .height(56.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.width(16.dp))
        FlatTextBodyTwo(text = "清除 LastCancelUpdate")
        Spacer(modifier = Modifier.weight(1f))
        Switch(
            enabled = lastCancelUpdate != 0L,
            checked = lastCancelUpdate != 0L,
            onCheckedChange = {
                lastCancelUpdate = 0
                appKVCenter.setLastCancelUpdate(0)
            })
        Spacer(modifier = Modifier.width(16.dp))
    }
}


@Preview
@Composable
fun PreviewDevTools() {
    FlatColumnPage {
        BackTopAppBar(title = "DevTools", {})

        LazyColumn(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                UserLoginFlag()
                MockEnableFlag()
                EnvSwitch()
            }
        }
    }
}