package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.RoomType
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.FlatColorBlue
import io.agora.flat.ui.theme.FlatColorGray
import io.agora.flat.ui.theme.FlatCommonTextStyle
import io.agora.flat.ui.viewmodel.CreateRoomViewModel
import io.agora.flat.ui.viewmodel.ViewState

@AndroidEntryPoint
class CreateRoomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: CreateRoomViewModel = viewModel()
            val viewState by viewModel.state.collectAsState()

            CreateRoomContent(viewState) { action ->
                when (action) {
                    CreateRoomAction.Close -> finish()
                    is CreateRoomAction.JoinRoom -> {
                        viewModel.enableVideo(action.openVideo)
                        Navigator.launchRoomPlayActivity(this, viewState.roomUUID)
                        finish()
                    }
                    is CreateRoomAction.CreateRoom -> viewModel.createRoom(
                        action.title,
                        action.roomType,
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateRoomContent(viewState: ViewState, actioner: (CreateRoomAction) -> Unit) {
    val context = LocalContext.current
    var title by remember {
        mutableStateOf(
            context.getString(
                R.string.join_room_default_time_format,
                viewState.username
            )
        )
    }
    var type by remember { mutableStateOf(RoomType.BigClass) }
    var openVideo by remember { mutableStateOf(false) }

    if (viewState.roomUUID.isNotBlank()) {
        LaunchedEffect(true) {
            actioner(CreateRoomAction.JoinRoom(viewState.roomUUID, openVideo))
        }
    }

    FlatColumnPage {
        CloseTopAppBar(
            title = stringResource(R.string.create_room),
            onClose = { actioner(CreateRoomAction.Close) })
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.create_room_topic))
            FlatSmallVerticalSpacer()
            FlatPrimaryTextField(
                value = title,
                onValueChange = { title = it },
                placeholderValue = stringResource(R.string.create_room_input_topic_hint)
            )
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.create_room_type))
            FlatSmallVerticalSpacer()
            TypeCheckLayout(type) { type = it }
            FlatNormalVerticalSpacer()
            Text(stringResource(R.string.join_option))
            FlatSmallVerticalSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = openVideo,
                    onCheckedChange = { openVideo = it }
                )
                Spacer(Modifier.width(8.dp))
                Text(stringResource(id = R.string.open_video))
            }
            Spacer(Modifier.height(32.dp))
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                FlatPrimaryTextButton(text = "创建", enabled = !viewState.loading) {
                    actioner(CreateRoomAction.CreateRoom(title, type))
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
    Row(Modifier.fillMaxWidth()) {
        TypeItem(
            checked = type == RoomType.BigClass,
            text = "大班课",
            id = R.drawable.img_big_class,
            modifier = Modifier
                .weight(7f)
                .clickable { onTypeChange(RoomType.BigClass) })
        Spacer(Modifier.weight(1f))
        TypeItem(
            checked = type == RoomType.SmallClass,
            text = "小班课",
            id = R.drawable.img_small_class,
            modifier = Modifier
                .weight(7f)
                .clickable { onTypeChange(RoomType.SmallClass) })
        Spacer(Modifier.weight(1f))
        TypeItem(
            checked = type == RoomType.OneToOne,
            text = "一对一",
            id = R.drawable.img_one_to_one,
            modifier = Modifier
                .weight(7f)
                .clickable { onTypeChange(RoomType.OneToOne) })
    }
}

@Composable
private fun TypeItem(
    checked: Boolean,
    text: String,
    @DrawableRes id: Int,
    modifier: Modifier,
) {
    val border = BorderStroke(1.dp, if (checked) FlatColorBlue else FlatColorGray)
    // TODO fix icon usage
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
            Text(text, style = FlatCommonTextStyle)
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Icon(painter = painterResource(icon), contentDescription = null, tint = Color.Unspecified)
    }
}

@Composable
@Preview
private fun CreateRoomPagePreview() {
    CreateRoomContent(ViewState()) {}
}

internal sealed class CreateRoomAction {
    object Close : CreateRoomAction()
    data class JoinRoom(val roomUUID: String, val openVideo: Boolean) : CreateRoomAction()
    data class CreateRoom(val title: String, val roomType: RoomType) : CreateRoomAction()
}