package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.ui.theme.isTabletMode
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
    if (isTabletMode()) {
        JoinRoomContentTablet(viewState, actioner = actioner)
    } else {
        JoinRoomContent(viewState, actioner = actioner)
    }
}

@Composable
private fun JoinRoomContentTablet(viewState: JoinRoomUiState, actioner: (JoinRoomAction) -> Unit) {
    var uuid by remember { mutableStateOf("") }
    var micOn by remember { mutableStateOf(false) }
    var cameraOn by remember { mutableStateOf(false) }
    var openDialog by remember { mutableStateOf(true) }

    if (openDialog) {
        Dialog(
            onDismissRequest = {
                actioner(JoinRoomAction.Close)
                openDialog = false
            },
            properties = DialogProperties(dismissOnClickOutside = false),
        ) {
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            val context = LocalContext.current

            Surface(Modifier.sizeIn(maxWidth = 480.dp), shape = Shapes.large) {
                Column {
                    Box(
                        Modifier
                            .fillMaxWidth(1f)
                            .height(56.dp)
                    ) {
                        FlatTextTitle(stringResource(R.string.title_join_room), Modifier.align(Alignment.Center))
                        IconButton(
                            onClick = {
                                actioner(JoinRoomAction.Close)
                                openDialog = false
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(painterResource(R.drawable.ic_title_close), contentDescription = null)
                        }
                    }
                    FlatDivider()
                    Spacer(Modifier.height(12.dp))
                    ThemeTextField(
                        value = uuid,
                        onValueChange = { uuid = it },
                        modifier = Modifier
                            .fillMaxWidth(1f)
                            .height(64.dp)
                            .padding(horizontal = 80.dp)
                            .focusRequester(focusRequester),
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number),
                        placeholderValue = stringResource(R.string.input_room_id_hint)
                    )
                    Spacer(Modifier.height(24.dp))
                    CameraPreviewCard(
                        modifier = Modifier
                            .padding(horizontal = 80.dp)
                            .aspectRatio(2f)
                            .clip(MaterialTheme.shapes.large),
                        cameraOn = cameraOn,
                        avatar = viewState.avatar
                    )
                    Row(
                        Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        DeviceOptions(
                            cameraOn = cameraOn,
                            micOn = micOn,
                            onCameraChanged = { cameraOn = it },
                            onMicChanged = { micOn = it }
                        )
                        Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                            FlatSmallPrimaryTextButton(stringResource(R.string.join)) {
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
        }
    }
}

@Composable
private fun JoinRoomContent(viewState: JoinRoomUiState, actioner: (JoinRoomAction) -> Unit) {
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
            CameraPreviewCard(
                modifier = Modifier
                    .padding(horizontal = 48.dp)
                    .aspectRatio(if (isTabletMode()) 2f else 1f)
                    .clip(MaterialTheme.shapes.large),
                cameraOn = cameraOn,
                avatar = viewState.avatar
            )
            DeviceOptions(
                cameraOn = cameraOn,
                micOn = micOn,
                onCameraChanged = { cameraOn = it },
                onMicChanged = { micOn = it }
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
@Preview(widthDp = 640, uiMode = 0x20)
private fun PagePreview() {
    FlatPage {
        JoinRoomContent(JoinRoomUiState.Empty) {}
    }
}