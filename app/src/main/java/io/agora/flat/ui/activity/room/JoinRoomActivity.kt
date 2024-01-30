package io.agora.flat.ui.activity.room

import android.Manifest
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Surface
import androidx.compose.material.TextButton
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import io.agora.flat.R
import io.agora.flat.common.FlatErrorCode
import io.agora.flat.common.FlatNetException
import io.agora.flat.common.Navigator
import io.agora.flat.common.board.DeviceState
import io.agora.flat.data.model.JoinRoomRecord
import io.agora.flat.ui.compose.CameraPreviewCard
import io.agora.flat.ui.compose.CloseTopAppBar
import io.agora.flat.ui.compose.DeviceOptions
import io.agora.flat.ui.compose.FlatBaseDialog
import io.agora.flat.ui.compose.FlatDivider
import io.agora.flat.ui.compose.FlatPage
import io.agora.flat.ui.compose.FlatPrimaryTextButton
import io.agora.flat.ui.compose.FlatSmallPrimaryTextButton
import io.agora.flat.ui.compose.FlatTextBodyOne
import io.agora.flat.ui.compose.FlatTextBodyTwo
import io.agora.flat.ui.compose.FlatTextTitle
import io.agora.flat.ui.compose.JoinRoomTextField
import io.agora.flat.ui.compose.WheelPicker
import io.agora.flat.ui.compose.noRippleClickable
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.ui.theme.isTabletMode
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.ui.util.UiMessage
import io.agora.flat.ui.viewmodel.JoinRoomUiState
import io.agora.flat.ui.viewmodel.JoinRoomViewModel
import io.agora.flat.util.hasPermission
import io.agora.flat.util.showToast
import io.agora.flat.util.toInviteCodeDisplay
import kotlinx.coroutines.launch

@Composable
fun JoinRoomScreen(
    navController: NavController,
    viewModel: JoinRoomViewModel = hiltViewModel(),
) {
    val viewState by viewModel.state.collectAsState()
    val context = LocalContext.current

    MessageHandler(viewState.message, viewState.joinEarly, viewModel::clearMessage)

    viewState.roomPlayInfo?.let { roomPlayInfo ->
        LaunchedEffect(roomPlayInfo) {
            Navigator.launchRoomPlayActivity(context, roomPlayInfo)
            navController.popBackStack()
        }
    }

    JoinRoomScreen(
        viewState = viewState,
        onClose = { navController.popBackStack() },
        onJoinRoom = { roomID, openVideo, openAudio -> viewModel.joinRoom(roomID, openVideo, openAudio) },
        onClearRecord = viewModel::clearJoinRoomRecord
    )
}

@Composable
fun MessageHandler(message: UiMessage?, joinEarly: Int, onClearMessage: (Long) -> Unit = {}) {
    val context = LocalContext.current

    if (message?.exception is FlatNetException && message.exception.code == FlatErrorCode.Web.RoomNotBegin) {
        FlatBaseDialog(
            title = context.getString(R.string.room_not_stared),
            message = context.getString(R.string.pay_room_not_started_join, joinEarly),
            onConfirm = { onClearMessage(message.id) }
        )
    } else {
        ShowUiMessageEffect(
            uiMessage = message,
            onMessageShown = onClearMessage
        )
    }
}

@Composable
fun JoinRoomScreen(
    viewState: JoinRoomUiState,
    onClose: () -> Unit,
    onJoinRoom: (String, Boolean, Boolean) -> Unit,
    onClearRecord: () -> Unit,
) {
    if (isTabletMode()) {
        JoinRoomContentTablet(viewState, onClose = onClose, onJoinRoom = onJoinRoom, onClearRecord)
    } else {
        JoinRoomContent(viewState, onClose = onClose, onJoinRoom = onJoinRoom, onClearRecord)
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun JoinRoomContentTablet(
    viewState: JoinRoomUiState,
    onClose: () -> Unit,
    onJoinRoom: (String, Boolean, Boolean) -> Unit,
    onClearRecord: () -> Unit,
) {
    Dialog(
        onDismissRequest = {
            onClose.invoke()
        },
        properties = DialogProperties(dismissOnClickOutside = false),
    ) {
        var uuid by remember { mutableStateOf("") }

        val cameraGranted = LocalContext.current.hasPermission(Manifest.permission.CAMERA)
        val recordGranted = LocalContext.current.hasPermission(Manifest.permission.RECORD_AUDIO)
        var cameraOn by remember { mutableStateOf(viewState.deviceState.camera && cameraGranted) }
        var micOn by remember { mutableStateOf(viewState.deviceState.mic && recordGranted) }

        val focusRequester = remember { FocusRequester() }
        val focusManager = LocalFocusManager.current
        val context = LocalContext.current

        val sheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Hidden,
            skipHalfExpanded = true,
        )
        val scope = rememberCoroutineScope()

        Surface(Modifier.sizeIn(maxWidth = 480.dp, maxHeight = 500.dp), shape = Shapes.large) {
            Column {
                Box(
                    Modifier
                        .fillMaxWidth(1f)
                        .height(56.dp)
                ) {
                    FlatTextTitle(stringResource(R.string.title_join_room), Modifier.align(Alignment.Center))
                    IconButton(
                        onClick = {
                            onClose.invoke()
                        },
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(painterResource(R.drawable.ic_title_close), contentDescription = null)
                    }
                }
                FlatDivider()
                Spacer(Modifier.height(12.dp))
                JoinRoomTextField(
                    value = uuid,
                    onValueChange = { uuid = it },
                    modifier = Modifier
                        .fillMaxWidth(1f)
                        .height(64.dp)
                        .padding(horizontal = 80.dp)
                        .focusRequester(focusRequester),
                    placeholderValue = stringResource(R.string.input_room_id_hint),
                    onExtendButtonClick = {
                        if (viewState.records.isEmpty()) return@JoinRoomTextField
                        focusManager.clearFocus()
                        scope.launch {
                            sheetState.show()
                        }
                    }
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
                Spacer(Modifier.height(48.dp))
                Row(
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DeviceOptions(
                        preferCameraOn = viewState.deviceState.camera,
                        preferMicOn = viewState.deviceState.mic,
                        cameraOn = cameraOn,
                        micOn = micOn,
                        onCameraChanged = { cameraOn = it },
                        onMicChanged = { micOn = it }
                    )
                    Box(Modifier.padding(horizontal = 8.dp), contentAlignment = Alignment.Center) {
                        FlatSmallPrimaryTextButton(stringResource(R.string.join)) {
                            if (uuid.isNotBlank()) {
                                onJoinRoom(uuid, cameraOn, micOn)
                            } else {
                                context.showToast(R.string.join_room_toast_empty)
                            }
                            focusManager.clearFocus()
                        }
                    }
                }
            }

            JoinRoomHistoryBottomSheet(
                sheetState = sheetState,
                histories = viewState.records,
                onClearRecord = {
                    scope.launch {
                        sheetState.hide()
                    }
                    onClearRecord()
                },
                onItemPicked = {
                    scope.launch {
                        sheetState.hide()
                    }
                    uuid = it.uuid
                },
                onCancel = {
                    scope.launch {
                        sheetState.hide()
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun JoinRoomContent(
    viewState: JoinRoomUiState,
    onClose: () -> Unit,
    onJoinRoom: (String, Boolean, Boolean) -> Unit,
    onClearRecord: () -> Unit,
) {
    var uuid by remember { mutableStateOf("") }

    val cameraGranted = LocalContext.current.hasPermission(Manifest.permission.CAMERA)
    val recordGranted = LocalContext.current.hasPermission(Manifest.permission.RECORD_AUDIO)
    var cameraOn by remember { mutableStateOf(viewState.deviceState.camera && cameraGranted) }
    var micOn by remember { mutableStateOf(viewState.deviceState.mic && recordGranted) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
    )
    val scope = rememberCoroutineScope()

    Column {
        CloseTopAppBar(title = stringResource(R.string.title_join_room), onClose = onClose)
        Column(
            Modifier
                .weight(1f)
                .noRippleClickable { focusManager.clearFocus() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(48.dp))
            JoinRoomTextField(
                value = uuid,
                onValueChange = { uuid = it },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                placeholderValue = stringResource(R.string.input_room_id_hint),
                onExtendButtonClick = if (viewState.records.isNotEmpty()) {
                    {
                        focusManager.clearFocus()
                        scope.launch {
                            sheetState.show()
                        }
                    }
                } else {
                    null
                }
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
                preferCameraOn = viewState.deviceState.camera,
                preferMicOn = viewState.deviceState.mic,
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
                        onJoinRoom(uuid, cameraOn, micOn)
                    } else {
                        context.showToast(R.string.join_room_toast_empty)
                    }
                    focusManager.clearFocus()
                }
            }
        }
    }

    JoinRoomHistoryBottomSheet(
        sheetState = sheetState,
        histories = viewState.records,
        onClearRecord = {
            scope.launch {
                sheetState.hide()
            }
            onClearRecord()
        },
        onItemPicked = {
            scope.launch {
                sheetState.hide()
            }
            uuid = it.uuid
        },
        onCancel = {
            scope.launch {
                sheetState.hide()
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun JoinRoomHistoryBottomSheet(
    sheetState: ModalBottomSheetState,
    histories: List<JoinRoomRecord>,
    onClearRecord: () -> Unit,
    onItemPicked: (JoinRoomRecord) -> Unit,
    onCancel: () -> Unit,
) {
    var index by remember { mutableStateOf(0) }

    ModalBottomSheetLayout(
        sheetState = sheetState,
        sheetShape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        sheetContent = {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.height(40.dp)) {
                    TextButton(onClick = onClearRecord) {
                        FlatTextBodyOne(
                            text = stringResource(R.string.clear_record),
                            color = FlatTheme.colors.textPrimary
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    TextButton(onClick = { onItemPicked(histories[index]) }) {
                        FlatTextBodyOne(text = stringResource(R.string.confirm), color = MaterialTheme.colors.primary)
                    }
                }

                WheelPicker(
                    count = histories.count(),
                    rowCount = 5,
                    size = DpSize(500.dp, 200.dp),
                    onScrollFinished = {
                        Log.e("Aderan", "onScrollFinished $it")
                        index = it
                        null
                    }) {
                    JoinRoomRecordLayout(histories[it])
                }

                TextButton(
                    onClick = onCancel,
                    Modifier
                        .height(40.dp)
                        .fillMaxWidth()
                ) {
                    FlatTextBodyTwo(text = stringResource(R.string.cancel), color = FlatTheme.colors.textPrimary)
                }
            }
        }
    ) {}
}

@Composable
private fun JoinRoomRecordLayout(item: JoinRoomRecord) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlatTextBodyTwo(item.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        Spacer(Modifier.width(8.dp))
        FlatTextBodyTwo(item.uuid.toInviteCodeDisplay())
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
@Preview(widthDp = 800, uiMode = 0x20)
private fun PagePreview() {
    FlatPage {
        JoinRoomScreen(
            viewState = JoinRoomUiState.by(
                DeviceState(camera = true, mic = true),
                avatar = "",
                records = listOf(
                    JoinRoomRecord("AAA PMI Room", "11112223333"),
                    JoinRoomRecord("BBB PMI Room", "11112224444"),
                    JoinRoomRecord("CCC PMI Room", "11112225555"),
                    JoinRoomRecord("DDD PMI Room", "11112226666"),
                )
            ),
            {},
            { _, _, _ -> },
            {}
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
@Preview(widthDp = 400)
private fun JoinRoomRecordLayoutPreview() {
    FlatPage {
        JoinRoomHistoryBottomSheet(
            sheetState = rememberModalBottomSheetState(
                initialValue = ModalBottomSheetValue.Expanded,
                skipHalfExpanded = true,
            ),
            histories = listOf(
                JoinRoomRecord("AAA PMI Room, AAA PMI Room, AAA PMI Room, AAA PMI Room, AAA PMI Room", "11123333"),
                JoinRoomRecord("AAA PMI Room", "1223333"),
                JoinRoomRecord("AAA PMI Room dd", "11112223333"),
            ),
            onClearRecord = { },
            onItemPicked = {}
        ) {

        }
    }
}