package io.agora.flat.ui.activity.room

import android.app.Activity
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
import io.agora.flat.ui.activity.ui.theme.FlatColorBlue
import io.agora.flat.ui.activity.ui.theme.FlatColorBorder
import io.agora.flat.ui.activity.ui.theme.FlatColorGray
import io.agora.flat.ui.activity.ui.theme.FlatCommonTextStyle
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.viewmodel.CreateRoomViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

@AndroidEntryPoint
class CreateRoomActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CreateRoomPage()
        }
    }
}

@Preview
@Composable
private fun CreateRoomPage() {
    val viewModel: CreateRoomViewModel = viewModel()
    val viewState = viewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var type by remember { mutableStateOf(RoomType.OneToOne) }
    var openVideo by remember { mutableStateOf(false) }
    val context = LocalContext.current

    if (viewState.value.roomUUID.isNotBlank()) {
        LaunchedEffect(true) {
            // TODO 正确切换协程、处理finish操作
            GlobalScope.launch {
                scope.launch {
                    viewModel.enableVideo(openVideo)
                    Navigator.launchRoomPlayActivity(context, roomUUID = viewState.value.roomUUID)
                    (context as Activity).finish()
                }
            }
        }
    }

    FlatColumnPage {
        CloseTopAppBar(
            title = stringResource(id = R.string.create_room),
            onClose = {})
        Column(
            Modifier
                .weight(1f)
                .padding(horizontal = 16.dp)) {
            FlatNormalVerticalSpacer()
            Text("主题")
            FlatSmallVerticalSpacer()
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.textFieldColors(
                    focusedIndicatorColor = FlatColorBorder,
                    unfocusedIndicatorColor = FlatColorBorder,
                    cursorColor = FlatColorBlue,
                ),
                textStyle = FlatCommonTextStyle,
                singleLine = true,
                placeholder = {
                    Text("请输入房间主题", style = FlatCommonTextStyle, color = FlatColorGray)
                }
            )
            FlatNormalVerticalSpacer()
            Text("类型")
            FlatSmallVerticalSpacer()
            TypeCheckLayout(type, onTypeChange = { type = it })
            FlatNormalVerticalSpacer()
            Text("加入选项")
            FlatSmallVerticalSpacer()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = openVideo,
                    onCheckedChange = { openVideo = it }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("开启摄像头")
            }
            Spacer(modifier = Modifier.height(32.dp))
            // TODO Loading
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                FlatPrimaryTextButton(text = "创建", enabled = !viewState.value.loading) {
                    viewModel.createRoom(title, type)
                }
                if (viewState.value.loading) {
                    FlatPageLoading()
                }
            }
        }
    }
}

@Composable
private fun ColumnScope.TypeCheckLayout(type: RoomType, onTypeChange: (RoomType) -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        TypeItem(
            checked = type == RoomType.OneToOne,
            text = "一对一",
            id = R.drawable.img_one_to_one,
            modifier = Modifier
                .weight(7f)
                .clickable { onTypeChange(RoomType.OneToOne) })
        Spacer(Modifier.weight(1f))
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
    }
}

@Composable
private fun RowScope.TypeItem(
    checked: Boolean,
    text: String,
    @DrawableRes id: Int,
    modifier: Modifier,
) {
    val border = BorderStroke(1.dp, if (checked) FlatColorBlue else FlatColorGray)
    // TODO fix icon usage
    val icon = if (checked) R.drawable.ic_item_checked else R.drawable.ic_item_unchecked

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.border(border, shape = MaterialTheme.shapes.small),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .aspectRatio(1f))
            Spacer(modifier = Modifier.height(12.dp))
            Text(text, style = FlatCommonTextStyle)
            Spacer(modifier = Modifier.height(12.dp))
        }
        Spacer(modifier = Modifier.height(8.dp))
        Icon(painter = painterResource(icon), contentDescription = null, tint = Color.Unspecified)
    }
}
