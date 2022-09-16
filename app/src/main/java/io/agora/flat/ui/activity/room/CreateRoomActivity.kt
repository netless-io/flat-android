package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices.PIXEL_C
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.RoomType
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.ui.viewmodel.CreateRoomUiState
import io.agora.flat.ui.viewmodel.CreateRoomViewModel
import io.agora.flat.util.delayLaunch
import io.agora.flat.util.showToast

@AndroidEntryPoint
class CreateRoomActivity : BaseComposeActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            FlatPage {
                // CreateRoomPage()
            }
        }
    }
}

@Composable
fun CreateRoomScreen(
    navController: NavController,
    viewModel: CreateRoomViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    val actioner: (CreateRoomAction) -> Unit = { action ->
        when (action) {
            CreateRoomAction.Close -> {
                navController.popBackStack()
            }
            is CreateRoomAction.JoinRoom -> {
                viewModel.enableVideo(action.cameraOn)
                Navigator.launchRoomPlayActivity(context, viewState.roomUUID, quickStart = true)
                scope.delayLaunch { navController.popBackStack() }
            }
            is CreateRoomAction.CreateRoom -> {
                viewModel.createRoom(action.title, action.roomType)
            }
            is CreateRoomAction.OnMessageShown -> {
                viewModel.clearMessage(action.id)
            }
        }
    }

    CreateRoomScreen(viewState, actioner)
}

@Composable
private fun CreateRoomScreen(viewState: CreateRoomUiState, actioner: (CreateRoomAction) -> Unit) {
    ShowUiMessageEffect(
        uiMessage = viewState.message,
        onMessageShown = { actioner(CreateRoomAction.OnMessageShown(it)) }
    )

    if (isTabletMode()) {
        CreateRoomContentTablet(viewState, actioner)
    } else {
        CreateRoomContent(viewState, actioner)
    }
}

@Composable
private fun CreateRoomContentTablet(
    viewState: CreateRoomUiState,
    actioner: (CreateRoomAction) -> Unit
) {
    val defaultTheme = stringResource(R.string.join_room_default_time_format, viewState.username)

    var theme by remember { mutableStateOf(defaultTheme) }
    var type by remember { mutableStateOf(RoomType.BigClass) }
    var cameraOn by remember { mutableStateOf(false) }
    var micOn by remember { mutableStateOf(false) }
    var openDialog by remember { mutableStateOf(true) }

    if (viewState.roomUUID.isNotBlank()) {
        LaunchedEffect(true) {
            actioner(CreateRoomAction.JoinRoom(viewState.roomUUID, cameraOn, micOn))
        }
    }

    if (openDialog) {
        Dialog(
            onDismissRequest = {
                actioner(CreateRoomAction.Close)
                openDialog = false
            },
            properties = DialogProperties(
                dismissOnClickOutside = false
            )
        ) {
            val focusRequester = remember { FocusRequester() }
            val focusManager = LocalFocusManager.current
            val context = LocalContext.current

            Surface(Modifier.sizeIn(maxWidth = 480.dp), shape = Shapes.large) {
                Column(Modifier.noRippleClickable { focusManager.clearFocus() }) {
                    Box(
                        Modifier
                            .fillMaxWidth(1f)
                            .height(56.dp)
                    ) {
                        FlatTextTitle(stringResource(R.string.create_room), Modifier.align(Alignment.Center))
                        IconButton(
                            onClick = {
                                actioner(CreateRoomAction.Close)
                                openDialog = false
                            },
                            modifier = Modifier.align(Alignment.CenterEnd)
                        ) {
                            Icon(painterResource(R.drawable.ic_title_close), contentDescription = null)
                        }
                    }
                    FlatDivider()
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height(12.dp))
                        ThemeTextField(
                            value = theme,
                            onValueChange = { theme = it },
                            modifier = Modifier
                                .fillMaxWidth(1f)
                                .height(64.dp)
                                .padding(horizontal = 80.dp)
                                .focusRequester(focusRequester),
                            placeholderValue = stringResource(R.string.create_room_input_theme)
                        )
                        Spacer(Modifier.height(24.dp))
                        TypeChooseLayout(
                            type = type,
                            onTypeChange = {
                                type = it
                                focusManager.clearFocus()
                            },
                            modifier = Modifier.padding(horizontal = 80.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        CameraPreviewCard(
                            modifier = Modifier
                                .padding(horizontal = 48.dp)
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
                                FlatSmallPrimaryTextButton(
                                    stringResource(R.string.start),
                                    enabled = !viewState.loading
                                ) {
                                    if (theme.isNotBlank()) {
                                        actioner(CreateRoomAction.CreateRoom(theme, type))
                                    } else {
                                        context.showToast(R.string.room_theme_empty_toast)
                                    }
                                    focusManager.clearFocus()
                                }
                                if (viewState.loading) CircularProgressIndicator(Modifier.size(24.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateRoomContent(viewState: CreateRoomUiState, actioner: (CreateRoomAction) -> Unit) {
    val defaultTheme = stringResource(R.string.join_room_default_time_format, viewState.username)
    var theme by remember { mutableStateOf(defaultTheme) }
    var type by remember { mutableStateOf(RoomType.BigClass) }
    var cameraOn by remember { mutableStateOf(false) }
    var micOn by remember { mutableStateOf(false) }

    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current

    if (viewState.roomUUID.isNotBlank()) {
        LaunchedEffect(true) {
            actioner(CreateRoomAction.JoinRoom(viewState.roomUUID, cameraOn, micOn))
        }
    }

    Column {
        CloseTopAppBar(stringResource(R.string.create_room), onClose = { actioner(CreateRoomAction.Close) })
        Column(
            Modifier
                .weight(1f)
                .noRippleClickable { focusManager.clearFocus() },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            ThemeTextField(
                value = theme,
                onValueChange = { theme = it },
                modifier = Modifier
                    .fillMaxWidth(1f)
                    .height(64.dp)
                    .padding(horizontal = 16.dp)
                    .focusRequester(focusRequester),
                placeholderValue = stringResource(R.string.create_room_input_theme)
            )
            Spacer(Modifier.height(32.dp))
            TypeChooseLayout(
                type = type,
                onTypeChange = {
                    type = it
                    focusManager.clearFocus()
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
                cameraOn = cameraOn,
                micOn = micOn,
                onCameraChanged = { cameraOn = it },
                onMicChanged = { micOn = it }
            )
            Spacer(Modifier.weight(1f))
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(16.dp, 32.dp), contentAlignment = Alignment.Center
            ) {
                FlatPrimaryTextButton(stringResource(R.string.start), enabled = !viewState.loading) {
                    if (theme.isNotBlank()) {
                        actioner(CreateRoomAction.CreateRoom(theme, type))
                    } else {
                        context.showToast(R.string.room_theme_empty_toast)
                    }
                    focusManager.clearFocus()
                }
                if (viewState.loading) CircularProgressIndicator(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
internal fun ThemeTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    placeholderValue: String,
) {
    var isCaptured by remember { mutableStateOf(false) }
    val dividerColor = if (isCaptured) Blue_6 else Gray_1

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier.onFocusChanged {
            isCaptured = it.isCaptured
        },
        textStyle = MaterialTheme.typography.h6.copy(
            color = FlatTheme.colors.textPrimary,
            textAlign = TextAlign.Center,
        ),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = true,
    ) { innerTextField ->
        Box(Modifier, contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 48.dp), contentAlignment = Alignment.Center
            ) {
                innerTextField()
            }
            Spacer(
                Modifier
                    .fillMaxWidth(1f)
                    .height(1.dp)
                    .background(dividerColor)
                    .align(Alignment.BottomCenter)
            )
            if (value.isEmpty()) {
                FlatTextBodyOneSecondary(placeholderValue)
            }
            if (value.isNotBlank()) {
                IconButton(onClick = { onValueChange("") }, modifier = Modifier.align(Alignment.CenterEnd)) {
                    Icon(painterResource(id = R.drawable.ic_text_filed_clear), "", tint = FlatTheme.colors.textPrimary)
                }
            }
        }
    }
}

@Composable
private fun TypeChooseLayout(type: RoomType, onTypeChange: (RoomType) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier) {
        TypeCheckItem(
            iconPainter = painterResource(R.drawable.ic_room_type_big_class),
            text = stringResource(id = R.string.room_type_big_class),
            checked = type == RoomType.BigClass,
            modifier = Modifier
                .weight(1f)
                .clickable { onTypeChange(RoomType.BigClass) })
        TypeCheckItem(
            iconPainter = painterResource(R.drawable.ic_room_type_small_class),
            text = stringResource(id = R.string.room_type_small_class),
            checked = type == RoomType.SmallClass,
            modifier = Modifier
                .weight(1f)
                .clickable { onTypeChange(RoomType.SmallClass) }
        )
        TypeCheckItem(
            iconPainter = painterResource(R.drawable.ic_room_type_ono_to_one),
            text = stringResource(id = R.string.room_type_one_to_one),
            checked = type == RoomType.OneToOne,
            modifier = Modifier
                .weight(1f)
                .clickable { onTypeChange(RoomType.OneToOne) }
        )
    }
}

@Composable
private fun TypeCheckItem(iconPainter: Painter, text: String, checked: Boolean, modifier: Modifier) {
    val icColor = if (checked) {
        MaterialTheme.colors.primary
    } else {
        if (isDarkTheme()) Gray_6 else Gray_3
    }
    val textColor = if (isDarkTheme()) Gray_6 else Gray_3
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(iconPainter, null, modifier = Modifier.size(48.dp), colorFilter = ColorFilter.tint(icColor))
            FlatTextCaption(text, color = textColor)
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
@Preview(device = PIXEL_C, widthDp = 400, uiMode = 0x20)
private fun CreateRoomScreenPreview() {
    FlatPage {
        CreateRoomScreen(CreateRoomUiState()) {}
    }
}

internal sealed class CreateRoomAction {
    object Close : CreateRoomAction()
    data class JoinRoom(val roomUUID: String, val cameraOn: Boolean, val micOn: Boolean) : CreateRoomAction()
    data class CreateRoom(val title: String, val roomType: RoomType) : CreateRoomAction()
    data class OnMessageShown(val id: Long) : CreateRoomAction()
}