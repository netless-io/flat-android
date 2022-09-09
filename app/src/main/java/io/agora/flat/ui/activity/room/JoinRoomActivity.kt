package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.ui.viewmodel.JoinRoomAction
import io.agora.flat.ui.viewmodel.JoinRoomUiState
import io.agora.flat.ui.viewmodel.JoinRoomViewModel
import io.agora.flat.util.showToast

@AndroidEntryPoint
class JoinRoomActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // FlatPage { JoinRoomPage() }
        }
    }
}

@Composable
fun JoinRoomScreen(
    navController: NavController,
    viewModel: JoinRoomViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    ShowUiMessageEffect(
        uiMessage = viewState.message,
        onMessageShown = viewModel::clearMessage
    )

    viewState.roomPlayInfo?.let { roomPlayInfo ->
        LaunchedEffect(roomPlayInfo) {
            Navigator.launchRoomPlayActivity(context, roomPlayInfo)
            navController.popBackStack()
        }
    }

    val actioner: (JoinRoomAction) -> Unit = { action ->
        when (action) {
            JoinRoomAction.Close -> {
                navController.popBackStack()
            }
            is JoinRoomAction.JoinRoom -> {
                viewModel.joinRoom(action.roomID, action.openVideo, action.openAudio)
            }
        }
    }
    JoinRoomScreen(viewState, actioner = actioner)
}

@Composable
private fun JoinRoomScreen(viewState: JoinRoomUiState, actioner: (JoinRoomAction) -> Unit) {
    var uuid by remember { mutableStateOf("") }
    var micOn by remember { mutableStateOf(false) }
    var cameraOn by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current

    val context = LocalContext.current
    Column {
        CloseTopAppBar(title = stringResource(R.string.title_join_room), onClose = { actioner(JoinRoomAction.Close) })
        Column(
            Modifier
                .weight(1f)
                .noRippleClickable { focusManager.clearFocus() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            ThemeTextField(
                value = uuid,
                onValueChange = { uuid = it },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                placeholderValue = stringResource(R.string.input_room_id_hint)
            )
            Spacer(Modifier.height(32.dp))
            DevicePreviewLayout(
                cameraOn = cameraOn,
                onCameraChanged = { cameraOn = it },
                micOn = micOn,
                onMicChanged = { micOn = it },
                avatar = viewState.avatar
            )
            Spacer(modifier = Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 32.dp), contentAlignment = Alignment.Center
            ) {
                FlatPrimaryTextButton(stringResource(R.string.join)) {
                    if (uuid.isNotBlank()) {
                        actioner(JoinRoomAction.JoinRoom(uuid, cameraOn, micOn))
                    } else {
                        context.showToast(R.string.join_room_toast_empty)
                    }
                    focusManager.clearFocus()
                }
            }
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun PagePreview() {
    FlatPage {
        JoinRoomScreen(JoinRoomUiState.Empty) {}
    }
}