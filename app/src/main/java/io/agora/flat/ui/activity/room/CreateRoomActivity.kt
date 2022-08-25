package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices.PIXEL_C
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.RoomType
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.FlatColorBlue
import io.agora.flat.ui.theme.FlatColorGray
import io.agora.flat.ui.util.ShowUiMessageEffect
import io.agora.flat.ui.viewmodel.CreateRoomUiState
import io.agora.flat.ui.viewmodel.CreateRoomViewModel
import io.agora.flat.util.delayLaunch
import io.agora.flat.util.isTabletMode

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
                viewModel.enableVideo(action.openVideo)
                Navigator.launchRoomPlayActivity(context, viewState.roomUUID, quickStart = true)
                scope.delayLaunch {
                    navController.popBackStack()
                }
            }
            is CreateRoomAction.CreateRoom -> {
                viewModel.createRoom(action.title, action.roomType)
            }
            is CreateRoomAction.OnMessageShown -> {
                viewModel.clearMessage(action.id)
            }
        }
    }

    CreateRoomContent(viewState, actioner)
}

@Composable
private fun CreateRoomContent(viewState: CreateRoomUiState, actioner: (CreateRoomAction) -> Unit) {
    val defaultTheme = LocalContext.current.getString(
        R.string.join_room_default_time_format,
        viewState.username
    )
    var theme by remember { mutableStateOf(defaultTheme) }
    var type by remember { mutableStateOf(RoomType.BigClass) }
    var openVideo by remember { mutableStateOf(false) }

    if (viewState.roomUUID.isNotBlank()) {
        LaunchedEffect(true) {
            actioner(CreateRoomAction.JoinRoom(viewState.roomUUID, openVideo))
        }
    }

    ShowUiMessageEffect(
        uiMessage = viewState.message,
        onMessageShown = { actioner(CreateRoomAction.OnMessageShown(it)) })

    Column {
        CloseTopAppBar(stringResource(R.string.create_room), onClose = { actioner(CreateRoomAction.Close) })
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            FlatNormalVerticalSpacer()
            FlatTextBodyTwo(stringResource(R.string.room_theme))
            FlatSmallVerticalSpacer()
            FlatPrimaryTextField(
                value = theme,
                onValueChange = { theme = it },
                placeholderValue = stringResource(R.string.create_room_input_theme)
            )
            FlatNormalVerticalSpacer()
            FlatTextBodyTwo(stringResource(R.string.create_room_type))
            FlatSmallVerticalSpacer()
            TypeCheckLayout(type) { type = it }
            FlatNormalVerticalSpacer()
            FlatTextBodyTwo(stringResource(R.string.join_option))
            FlatSmallVerticalSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = openVideo, onCheckedChange = { openVideo = it })
                Spacer(Modifier.width(8.dp))
                FlatTextCaption(stringResource(id = R.string.turn_on_camera))
            }
            Spacer(Modifier.height(32.dp))
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FlatPrimaryTextButton(stringResource(R.string.start), enabled = !viewState.loading) {
                    actioner(CreateRoomAction.CreateRoom(theme, type))
                }
                if (viewState.loading) {
                    CircularProgressIndicator(Modifier.size(24.dp))
                }
            }
        }
    }
}

@Composable
private fun TypeCheckLayout(type: RoomType, onTypeChange: (RoomType) -> Unit) {
    Row {
        TypeItem(
            checked = type == RoomType.BigClass,
            text = stringResource(id = R.string.room_type_big_class),
            desc = stringResource(id = R.string.room_type_desc_big_class),
            id = R.drawable.img_big_class,
            modifier = Modifier
                .weight(1f)
                .clickable { onTypeChange(RoomType.BigClass) })
        Spacer(Modifier.width(16.dp))
        TypeItem(
            checked = type == RoomType.SmallClass,
            text = stringResource(id = R.string.room_type_small_class),
            desc = stringResource(id = R.string.room_type_desc_small_class),
            id = R.drawable.img_small_class,
            modifier = Modifier
                .weight(1f)
                .clickable { onTypeChange(RoomType.SmallClass) })
        Spacer(Modifier.width(16.dp))
        TypeItem(
            checked = type == RoomType.OneToOne,
            text = stringResource(id = R.string.room_type_one_to_one),
            desc = stringResource(id = R.string.room_type_desc_one_to_one),
            id = R.drawable.img_one_to_one,
            modifier = Modifier
                .weight(1f)
                .clickable { onTypeChange(RoomType.OneToOne) })
    }
}

@Composable
private fun TypeItem(checked: Boolean, text: String, desc: String, @DrawableRes id: Int, modifier: Modifier) {
    val isTablet = LocalContext.current.isTabletMode()

    if (isTablet) {
        TypeItemTablet(checked, text, desc, id, modifier)
    } else {
        TypeItemPhone(checked, text, id, modifier)
    }
}

@Composable
private fun TypeItemTablet(checked: Boolean, text: String, desc: String, @DrawableRes id: Int, modifier: Modifier) {
    val border = BorderStroke(1.dp, if (checked) FlatColorBlue else FlatColorGray)
    val icon = if (checked) R.drawable.ic_item_checked else R.drawable.ic_item_unchecked

    Row(modifier = modifier.border(border, shape = MaterialTheme.shapes.small)) {
        Image(
            painter = painterResource(id),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .padding(12.dp)
                .size(72.dp)
        )
        Column(Modifier.weight(1f)) {
            Spacer(modifier = Modifier.height(8.dp))
            FlatTextBodyOne(text)
            Spacer(modifier = Modifier.height(4.dp))
            FlatTextCaption(desc, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Icon(
                painterResource(icon),
                null,
                Modifier
                    .align(Alignment.End)
                    .padding(12.dp),
                Color.Unspecified
            )
        }
    }
}

@Composable
private fun TypeItemPhone(checked: Boolean, text: String, @DrawableRes id: Int, modifier: Modifier) {
    val border = BorderStroke(1.dp, if (checked) FlatColorBlue else FlatColorGray)
    val icon = if (checked) R.drawable.ic_item_checked else R.drawable.ic_item_unchecked

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            Modifier.border(border, shape = MaterialTheme.shapes.small),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .aspectRatio(1f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            FlatTextBodyOne(text)
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Icon(painter = painterResource(icon), contentDescription = null, tint = Color.Unspecified)
    }
}

@Composable
@Preview(device = PIXEL_C)
@Preview(device = PIXEL_C, uiMode = 0x20)
@Preview
private fun CreateRoomPageTabletPreview() {
    FlatPage {
        CreateRoomContent(CreateRoomUiState()) {}
    }
}

internal sealed class CreateRoomAction {
    object Close : CreateRoomAction()
    data class JoinRoom(val roomUUID: String, val openVideo: Boolean) : CreateRoomAction()
    data class CreateRoom(val title: String, val roomType: RoomType) : CreateRoomAction()
    data class OnMessageShown(val id: Long) : CreateRoomAction()
}