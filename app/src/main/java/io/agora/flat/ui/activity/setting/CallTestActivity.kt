package io.agora.flat.ui.activity.setting

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.viewmodel.CallTestState
import io.agora.flat.ui.viewmodel.CallTestViewModel

@AndroidEntryPoint
class CallTestActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val viewModel: CallTestViewModel = viewModel()
            val state by viewModel.state.collectAsState()

            CallTestContent(state) { action ->
                when (action) {
                    CallTestUIAction.Back -> finish()
                    CallTestUIAction.EchoTest -> viewModel.startEchoTest()
                    CallTestUIAction.EchoTestStop -> viewModel.stopEchoTest()
                    CallTestUIAction.LastmileTest -> viewModel.startLastmileTest()
                }
            }
        }
    }
}

@Composable
private fun CallTestContent(state: CallTestState, actioner: (CallTestUIAction) -> Unit) {
    FlatColumnPage {
        BackTopAppBar(stringResource(R.string.title_call_test), { actioner(CallTestUIAction.Back) })
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(16.dp)
        ) {
            Column {
                Text(text = "quality: ${state.quality}")
                Text(text = "last mile: ${state.getLastMileResult()}")
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column {
                FlatPrimaryTextButton(text = "网络质量探测") {
                    actioner(CallTestUIAction.LastmileTest)
                }
                Spacer(Modifier.height(8.dp))
                if (state.echoStarted) {
                    FlatPrimaryTextButton(text = "停止回声检测") {
                        actioner(CallTestUIAction.EchoTestStop)
                    }
                } else {
                    FlatPrimaryTextButton(text = "开始回声检测") {
                        actioner(CallTestUIAction.EchoTest)
                    }
                }

            }
        }
    }
}

@Composable
@Preview
private fun CallTestPreview() {
    val state = CallTestState(
        quality = 100
    )
    CallTestContent(state) { }
}

internal sealed class CallTestUIAction {
    object Back : CallTestUIAction()
    object EchoTest : CallTestUIAction()
    object EchoTestStop : CallTestUIAction()
    object LastmileTest : CallTestUIAction()
}