package io.agora.flat.ui.activity.{PACKAGE_NAME}

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.BackTopAppBar
import io.agora.flat.ui.compose.FlatColumnPage

@AndroidEntryPoint
class {ACTIVITY_NAME}Activity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val actioner: ({ACTIVITY_NAME}UiAction) -> Unit = { action ->
                when (action) {
                    else -> {}
                }
            }
            {ACTIVITY_NAME}Screen(actioner = actioner)
        }
    }
}

@Composable
fun {ACTIVITY_NAME}Screen(
    viewModel: {ACTIVITY_NAME}ViewModel = hiltViewModel(),
    actioner: ({ACTIVITY_NAME}UiAction) -> Unit,
) {
    val viewState by viewModel.state.collectAsState()

    {ACTIVITY_NAME}Screen(viewState = viewState, actioner = actioner)
}

@Composable
internal fun {ACTIVITY_NAME}Screen(
    viewState: {ACTIVITY_NAME}UiViewState,
    actioner: ({ACTIVITY_NAME}UiAction) -> Unit,
) {
    FlatColumnPage {
        BackTopAppBar(title = "{ACTIVITY_NAME}", {})
    }
}

@Composable
@Preview
internal fun {ACTIVITY_NAME}ScreenPreview() {
    {ACTIVITY_NAME}Screen(viewState = {ACTIVITY_NAME}UiViewState.Empty) {}
}
