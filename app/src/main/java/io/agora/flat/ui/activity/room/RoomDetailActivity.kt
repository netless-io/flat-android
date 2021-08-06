package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.MaterialTheme.typography
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.rememberAndroidClipboardController
import io.agora.flat.data.model.*
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.ui.viewmodel.RoomDetailViewModel
import io.agora.flat.ui.viewmodel.UIRoomInfo
import io.agora.flat.ui.viewmodel.UserViewModel
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.showToast
import java.util.*

@AndroidEntryPoint
class RoomDetailActivity : ComponentActivity() {

    @ExperimentalAnimationApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val viewModel: RoomDetailViewModel = viewModel()
            val cancelSuccess = viewModel.cancelSuccess.collectAsState()
            var visible by remember { mutableStateOf(false) }

            var actioner: (DetailUiAction) -> Unit = { action ->
                when (action) {
                    DetailUiAction.Back -> finish()
                    is DetailUiAction.EnterRoom -> {
                        Navigator.launchRoomPlayActivity(
                            this@RoomDetailActivity,
                            action.roomUUID,
                            action.periodicUUID
                        )
                    }
                    DetailUiAction.Invite -> {
                        // Show Dialog
                    }
                    is DetailUiAction.Playback -> {
                        Navigator.launchPlaybackActivity(
                            this@RoomDetailActivity,
                            action.roomUUID
                        )
                    }
                    DetailUiAction.ShowAllRooms -> {
                        visible = true
                    }
                    DetailUiAction.AllRoomBack -> {
                        visible = false
                    }
                    DetailUiAction.CancelRoom, DetailUiAction.DeleteRoom -> {
                        viewModel.cancelRoom()
                    }
                    DetailUiAction.ModifyRoom -> {

                    }
                }
            }

            if (cancelSuccess.value) {
                Navigator.launchHomeActivity(LocalContext.current)
                finish()
                return@setContent
            }

            Box(Modifier.fillMaxSize()) {
                RoomDetailPage(actioner = actioner)
                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(initialAlpha = 0.3F, animationSpec = tween()),
                    exit = fadeOut(animationSpec = tween())
                ) {
                    AllRoomDetail(actioner = actioner)
                }
            }
        }
    }
}

@Composable
private fun RoomDetailPage(actioner: (DetailUiAction) -> Unit) {
    FlatColumnPage {
        val viewModel: RoomDetailViewModel = viewModel()
        val viewState by viewModel.state.collectAsState()

        BackTopAppBar(
            stringResource(R.string.title_room_detail),
            onBackPressed = { actioner(DetailUiAction.Back) }) {
            viewState.roomInfo?.run {
                AppBarMoreButton(viewState.isOwner, roomStatus, actioner)
            }
        }

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f), contentAlignment = Alignment.Center
        ) {
            if (viewState.roomInfo != null) {
                val roomInfo = viewState.roomInfo!!
                Column(Modifier.fillMaxSize()) {
                    TimeDisplay(
                        begin = roomInfo.beginTime,
                        end = roomInfo.endTime,
                        state = roomInfo.roomStatus
                    )
                    if (viewModel.isPeriodicRoom() && viewState.periodicRoomInfo != null) {
                        val periodicRoomInfo = viewState.periodicRoomInfo!!
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "查看全部${periodicRoomInfo.rooms.size}个房间",
                                Modifier
                                    .padding(4.dp)
                                    .clickable { actioner(DetailUiAction.ShowAllRooms) },
                                style = allRoomTextStyle
                            )
                            FlatNormalVerticalSpacer()
                        }
                    }
                    MoreRomeInfoDisplay(
                        if (viewModel.isPeriodicRoom()) roomInfo.periodicUUID!! else roomInfo.roomUUID,
                        roomInfo.roomType,
                        viewModel.isPeriodicRoom()
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Operations(
                        roomInfo,
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 16.dp),
                        actioner = actioner
                    )
                }
            }
            if (viewState.loading) {
                FlatPageLoading()
            }
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun AllRoomDetail(actioner: (DetailUiAction) -> Unit) {
    val viewModel: RoomDetailViewModel = viewModel()
    val viewState by viewModel.state.collectAsState()

    FlatColumnPage {
        BackHandler(onBack = { actioner(DetailUiAction.AllRoomBack) })
        BackTopAppBar(stringResource(R.string.title_room_all),
            onBackPressed = { actioner(DetailUiAction.AllRoomBack) }) {
            viewState.roomInfo?.run {
                AppBarMoreButton(viewState.isOwner, roomStatus, actioner)
            }
        }
        viewState.periodicRoomInfo?.run {
            LazyColumn {
                item {
                    PeriodicInfoDisplay(periodic, rooms.size)
                }

                item {
                    PeriodicSubRoomsDisplay(rooms)
                }
            }
        }
    }
}

@Composable
private fun AppBarMoreButton(isOwner: Boolean, roomStatus: RoomStatus, actioner: (DetailUiAction) -> Unit) {
    Box {
        var expanded by remember { mutableStateOf(false) }

        IconButton(onClick = { expanded = true }, enabled = !(isOwner && roomStatus == RoomStatus.Started)) {
            Icon(Icons.Outlined.MoreHoriz, contentDescription = null)
        }

        DetailDropdownMenu(isOwner, roomStatus, expanded, { expanded = false }, actioner)
    }
}

@Composable
private fun DetailDropdownMenu(
    isOwner: Boolean,
    roomStatus: RoomStatus,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    actioner: (DetailUiAction) -> Unit,
) {
    DropdownMenu(
        modifier = Modifier.wrapContentSize(),
        expanded = expanded,
        onDismissRequest = onDismissRequest,
    ) {
        if (isOwner && roomStatus == RoomStatus.Idle) {
            DropdownMenuItem(onClick = { actioner(DetailUiAction.ModifyRoom) }) {
                Text(stringResource(R.string.modify_room))
            }
            DropdownMenuItem(onClick = { actioner(DetailUiAction.CancelRoom) }) {
                Text(stringResource(R.string.cancel_room), color = FlatColorRed)
            }
        }
        if (!isOwner && roomStatus != RoomStatus.Stopped) {
            DropdownMenuItem(onClick = { actioner(DetailUiAction.CancelRoom) }) {
                Text(stringResource(R.string.remove_room), color = FlatColorRed)
            }
        }
        if (roomStatus == RoomStatus.Stopped) {
            DropdownMenuItem(onClick = { actioner(DetailUiAction.DeleteRoom) }) {
                Text(stringResource(R.string.delete_history), color = FlatColorRed)
            }
        }
    }
}

@Composable
private fun PeriodicSubRoomsDisplay(rooms: ArrayList<RoomInfo>) {
    var lastMonth = 0
    val cal = Calendar.getInstance()
    rooms.forEach {
        cal.timeInMillis = it.beginTime
        val month = (cal.get(Calendar.MONTH) + 1)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val timeBegin = "${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE)}"

        cal.timeInMillis = it.endTime
        val timeEnd = "${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE)}"

        val dayText = "${day}日"
        val dayOfWeekText = when (dayOfWeek) {
            Calendar.SUNDAY -> "周日"
            Calendar.MONDAY -> "周一"
            Calendar.TUESDAY -> "周二"
            Calendar.WEDNESDAY -> "周三"
            Calendar.THURSDAY -> "周四"
            Calendar.FRIDAY -> "周五"
            Calendar.SATURDAY -> "周六"
            else -> ""
        }
        val timeDuring = "${timeBegin}~${timeEnd}"

        if (month != lastMonth) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                FlatNormalVerticalSpacer()
                Text("${month}月", Modifier.padding(vertical = 4.dp), style = typography.body1)
                FlatNormalVerticalSpacer()
                Divider(thickness = 1.dp, color = FlatColorDivider)
                FlatSmallVerticalSpacer()
            }
            lastMonth = month
        }
        PeriodicSubRoomItem(dayText, dayOfWeekText, timeDuring, it.roomStatus)
    }
}

@Composable
private fun PeriodicSubRoomItem(
    dayText: String,
    dayOfWeekText: String,
    timeDuring: String,
    roomStatus: RoomStatus,
) {
    var expanded by remember { mutableStateOf(false) }

    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(dayText, Modifier.padding(horizontal = 16.dp), style = typography.body2)
        Text(dayOfWeekText, Modifier.padding(horizontal = 16.dp), style = typography.body2)
        Text(timeDuring, Modifier.padding(horizontal = 16.dp), style = typography.body2)
        FlatRoomStatusText(roomStatus, Modifier.padding(horizontal = 16.dp))
        // TODO 添加按钮处理
        Box(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Outlined.MoreHoriz,
                contentDescription = null,
                modifier = Modifier
                    .size(24.dp)
                    .clickable { expanded = true }
            )
            DropdownMenu(
                modifier = Modifier.wrapContentSize(),
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("房间详情")
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("修改房间")
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("取消房间")
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    Text("复制邀请")
                }
            }
        }
    }
}

@Composable
private fun PeriodicInfoDisplay(roomPeriodic: RoomPeriodic, number: Int) {
    val typography = MaterialTheme.typography
    val colors = MaterialTheme.colors

    Box(
        Modifier
            .padding(16.dp)
            .clip(Shapes.medium)
            .background(Color(0xFFF3F6F9))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            val weekInfo = "每" + roomPeriodic.weeks.joinToString(separator = "、") {
                when (it) {
                    Week.Sunday -> "周日"
                    Week.Monday -> "周一"
                    Week.Tuesday -> "周二"
                    Week.Wednesday -> "周三"
                    Week.Thursday -> "周四"
                    Week.Friday -> "周五"
                    Week.Saturday -> "周六"
                }
            }
            val type = "房间类型：${
                when (roomPeriodic.roomType) {
                    RoomType.OneToOne -> "一对一"
                    RoomType.SmallClass -> "小班课"
                    RoomType.BigClass -> "大班课"
                }
            }（周期）"
            val desc =
                "结束于 ${FlatFormatter.longDateWithWeek(roomPeriodic.endTime)}，共 $number 场会议"
            Text(
                weekInfo,
                Modifier.padding(2.dp),
                style = typography.body2,
                color = colors.secondary
            )
            FlatSmallVerticalSpacer()
            Text(type, Modifier.padding(2.dp), style = typography.body2)
            FlatSmallVerticalSpacer()
            Text(
                desc,
                maxLines = 1,
                style = typography.body2
            )
        }
    }
}

@Composable
private fun Operations(
    roomInfo: UIRoomInfo,
    modifier: Modifier,
    actioner: (DetailUiAction) -> Unit,
) = when (roomInfo.roomStatus) {
    RoomStatus.Idle, RoomStatus.Paused, RoomStatus.Started ->
        Column(modifier) {
            var openDialog = remember { mutableStateOf(false) }
            val clipboard = rememberAndroidClipboardController()
            val context = LocalContext.current

            if (openDialog.value) {
                InviteDialog(openDialog, roomInfo) {
                    clipboard.putText(it)
                    openDialog.value = false
                    context.showToast("复制成功")
                }
            }

            FlatSecondaryTextButton(text = "邀请", onClick = { openDialog.value = true })
            FlatNormalVerticalSpacer()
            FlatPrimaryTextButton(text = "进入房间", onClick = {
                actioner(
                    DetailUiAction.EnterRoom(
                        roomInfo.roomUUID,
                        roomInfo.periodicUUID
                    )
                )
            })
        }
    RoomStatus.Stopped ->
        Column(modifier) {
            FlatPrimaryTextButton(
                text = stringResource(id = R.string.replay),
                enabled = roomInfo.hasRecord,
                onClick = { actioner(DetailUiAction.Playback(roomInfo.roomUUID)) })
        }
}

@Composable
private fun InviteDialog(openDialog: MutableState<Boolean>, roomInfo: UIRoomInfo, onCopy: (String) -> Unit) {
    val viewModel: UserViewModel = viewModel()
    val username = viewModel.userInfo.value!!.name

    Dialog(onDismissRequest = { openDialog.value = false }) {
        Surface(
            shape = Shapes.large,
            color = MaterialTheme.colors.surface,
            contentColor = contentColorFor(MaterialTheme.colors.surface)
        ) {
            val timeDuring = "${FlatFormatter.date(roomInfo.beginTime)} ${
                FlatFormatter.timeDuring(
                    roomInfo.beginTime,
                    roomInfo.endTime
                )
            }"
            val copyText = """
                            |$username 邀请你加入 Flat 房间
                            |房间主题：${roomInfo.title}
                            |开始时间：${timeDuring}
                            |房间号：${roomInfo.roomUUID}
                            |打开（没有安装的话请先下载并安装）并登录 Flat，点击加入房间，输入房间号即可加入和预约
                        """.trimMargin()
            Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text("$username 邀请您参加 Flat 房间")
                FlatLargeVerticalSpacer()
                Box(Modifier.fillMaxWidth()) {
                    Text("房间主题", Modifier.align(Alignment.TopStart))
                    Text(roomInfo.title, Modifier.align(Alignment.TopEnd))
                }
                FlatNormalVerticalSpacer()
                Box(Modifier.fillMaxWidth()) {
                    Text("房间号", Modifier.align(Alignment.TopStart))
                    Text(
                        roomInfo.roomUUID.substringAfterLast("-"),
                        Modifier.align(Alignment.TopEnd)
                    )
                }
                FlatNormalVerticalSpacer()
                Box(Modifier.fillMaxWidth()) {
                    Text("开始时间", Modifier.align(Alignment.TopStart))
                    Text(timeDuring, Modifier.align(Alignment.TopEnd))
                }
                FlatLargeVerticalSpacer()
                FlatPrimaryTextButton(
                    text = "复制邀请链接",
                    onClick = { onCopy(copyText) },
                )
            }
        }
    }
}

private val allRoomTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    color = FlatColorBlue,
)

private val moreInfoTextStyle = TextStyle(
    fontFamily = FontFamily.Default,
    fontSize = 14.sp,
    color = FlatColorTextSecondary,
)

@Composable
private fun MoreRomeInfoDisplay(uuid: String, roomType: RoomType, isPeriodic: Boolean) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        Divider(color = FlatColorDivider, thickness = 1.dp)
        FlatNormalVerticalSpacer()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.ic_room), contentDescription = "")
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.room_number), style = moreInfoTextStyle)
            Spacer(modifier = Modifier.weight(1f))
            Text(uuid, style = moreInfoTextStyle)
        }
        FlatLargeVerticalSpacer()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painter = painterResource(id = R.drawable.ic_room_type), contentDescription = "")
            Spacer(modifier = Modifier.width(4.dp))
            Text(stringResource(R.string.room_number), style = moreInfoTextStyle)
            Spacer(modifier = Modifier.weight(1f))
            // TODO 本土化及格式化
            val type = when (roomType) {
                RoomType.BigClass -> "大班课"
                RoomType.SmallClass -> "小班课"
                RoomType.OneToOne -> "一对一"
            } + if (isPeriodic) {
                "（周期）"
            } else {
                ""
            }
            Text(type, style = moreInfoTextStyle)
        }
        FlatNormalVerticalSpacer()
        Divider(color = FlatColorDivider, thickness = 1.dp)
    }
}

private val timeTextStyle = TextStyle(
    FlatColorTextPrimary,
    fontSize = 24.sp,
    fontWeight = FontWeight.Bold
)

private val dateTextStyle = TextStyle(
    FlatColorTextPrimary,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal
)

@Composable
private fun TimeDisplay(begin: Long, end: Long, state: RoomStatus) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.End) {
            Text(FlatFormatter.time(begin), style = timeTextStyle)
            FlatSmallVerticalSpacer()
            Text(FlatFormatter.formatLongDate(begin), style = dateTextStyle)
        }
        FlatLargeHorizontalSpacer()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlatSmallVerticalSpacer()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEEF0F6)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    FlatFormatter.diffTime(begin, end),
                    Modifier.padding(horizontal = 18.dp, vertical = 3.dp),
                    style = FlatSmallTextStyle,
                )
            }
            FlatSmallVerticalSpacer()
            RoomState(state, Modifier.padding(4.dp))
        }
        FlatLargeHorizontalSpacer()
        Column(horizontalAlignment = Alignment.Start) {
            Text(FlatFormatter.time(end), style = timeTextStyle)
            FlatSmallVerticalSpacer()
            Text(FlatFormatter.formatLongDate(end), style = dateTextStyle)
        }
    }
}

@Composable
private fun RoomState(state: RoomStatus, modifier: Modifier) {
    when (state) {
        RoomStatus.Idle ->
            Text(
                stringResource(R.string.home_room_state_idle),
                modifier,
                style = typography.body2,
                color = FlatColorRed
            )
        RoomStatus.Started, RoomStatus.Paused ->
            Text(
                stringResource(R.string.home_room_state_started),
                modifier,
                style = typography.body2,
                color = FlatColorBlue
            )
        RoomStatus.Stopped ->
            Text(
                stringResource(R.string.home_room_state_end),
                modifier,
                style = typography.body2,
                color = FlatColorTextSecondary
            )
    }
}

//@Composable
//@Preview(backgroundColor = 0xFFFFFFFF)
//private fun TimeDisplayPreview() {
//    TimeDisplay(begin = 1617695346000, end = 1617695346000 + 1800_000, state = RoomStatus.Idle)
//}
//
//@Composable
//@Preview(backgroundColor = 0xFFFFFFFF)
//private fun MoreInfoPreview() {
//    MoreRomeInfoDisplay("1234565f2259d5069bc0d50f225", RoomType.BigClass, true)
//}
//
//@Composable
//@Preview
//private fun OperationsPreview() {
//    Operations(
//        Modifier
//            .fillMaxWidth()
//            .padding(vertical = 32.dp, horizontal = 16.dp)
//    ) {
//    }
//}

//@Composable
//@Preview(backgroundColor = 0xFFFFFFFF)
//private fun RoomsDisplayPreview() {
//    val json =
//        "{\"periodic\":{\"ownerUUID\":\"722f7f6d-cc0f-4e63-a543-446a3b7bd659\",\"ownerUserName\":\"冯利斌\",\"roomType\":\"OneToOne\",\"endTime\":1622188800000,\"rate\":50,\"title\":\"超长周期房间\",\"weeks\":[0,1,2,3,4,5,6]},\"rooms\":[{\"roomUUID\":\"ded64f44-6a50-488b-a6d7-0fa49bf38ddd\",\"beginTime\":1617955200000,\"endTime\":1683275400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"8d348dcb-881b-4f31-bc39-3019b6c386a4\",\"beginTime\":1618041600000,\"endTime\":1683361800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"260ca671-8dab-4ac0-8461-ed64cdd5aac4\",\"beginTime\":1618128000000,\"endTime\":1683448200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"ed94d2ce-c85e-481d-ab05-d9656f2b305c\",\"beginTime\":1618214400000,\"endTime\":1683534600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"1962b35c-e472-42fc-87af-9c9b95f3e641\",\"beginTime\":1618300800000,\"endTime\":1683621000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"41fed574-26d6-40e5-862c-0fad9bdc92a8\",\"beginTime\":1618387200000,\"endTime\":1683707400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"371dbb7e-744b-4df9-8e75-9623823dcb41\",\"beginTime\":1618473600000,\"endTime\":1683793800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"d34765db-168f-4b57-9b83-bd2364bc851c\",\"beginTime\":1618560000000,\"endTime\":1683880200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"5aba1cbb-19e0-4752-a104-b84d8d8d4b1a\",\"beginTime\":1618646400000,\"endTime\":1683966600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"a25716ab-ee4d-49fe-a873-b152eefb7c33\",\"beginTime\":1618732800000,\"endTime\":1684053000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"ff60692f-6cb0-4a7d-86f3-d33201911979\",\"beginTime\":1618819200000,\"endTime\":1684139400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"7d92d59d-3b42-408a-bbbc-3d597f773131\",\"beginTime\":1618905600000,\"endTime\":1684225800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"c3108094-82eb-4783-8274-37c3c96f499b\",\"beginTime\":1618992000000,\"endTime\":1684312200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"3c5b9dca-174a-453e-a16c-27072d80911b\",\"beginTime\":1619078400000,\"endTime\":1684398600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"b6dacbe1-1252-44ec-aa5b-f9076563f6ab\",\"beginTime\":1619164800000,\"endTime\":1684485000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"ccb62387-a7a3-4f13-b908-9f36bffeb610\",\"beginTime\":1619251200000,\"endTime\":1684571400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"c5976577-f2de-43aa-abe8-77315582dbb9\",\"beginTime\":1619337600000,\"endTime\":1684657800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"caa58949-9727-4c3a-b743-dd38b66f4d8a\",\"beginTime\":1619424000000,\"endTime\":1684744200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"7097f3a7-1f2b-46e3-ad9e-35fa814f0727\",\"beginTime\":1619510400000,\"endTime\":1684830600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"2d834e13-72e3-4d38-b38c-01458a022342\",\"beginTime\":1619596800000,\"endTime\":1684917000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"6e2e6776-18bc-4c2a-88ef-40824c710d3e\",\"beginTime\":1619683200000,\"endTime\":1685003400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"d5b9e2e9-8914-46f1-bdc2-a8b1fa03f0b6\",\"beginTime\":1619769600000,\"endTime\":1685089800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"3c79aa5a-c711-4459-b9e2-a7b5f383b917\",\"beginTime\":1619856000000,\"endTime\":1685176200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"5c1e8916-3f6b-4a5d-9fcb-9bbfd8f791b9\",\"beginTime\":1619942400000,\"endTime\":1685262600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"53dbaccc-6778-4110-a7b6-96496d006b49\",\"beginTime\":1620028800000,\"endTime\":1685349000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"8dc38681-cdff-4a7a-9baf-9a32a519dbc8\",\"beginTime\":1620115200000,\"endTime\":1685435400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"2cc352ca-dc33-43a5-b051-9240493186db\",\"beginTime\":1620201600000,\"endTime\":1685521800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"30de0ee9-dc97-4286-863d-3a6f3eb0857b\",\"beginTime\":1620288000000,\"endTime\":1685608200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"d14bdab2-332a-4787-b6c9-cd93b0e06e80\",\"beginTime\":1620374400000,\"endTime\":1685694600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"e36333da-bc43-4135-82d8-6479e3de269f\",\"beginTime\":1620460800000,\"endTime\":1685781000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"a7ec57a2-15d5-4e7a-a61d-fd0d17adf0e4\",\"beginTime\":1620547200000,\"endTime\":1685867400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"3ab3b24c-2421-42f6-b636-ba181b2619b2\",\"beginTime\":1620633600000,\"endTime\":1685953800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"3f064640-d5e4-4100-818f-36eb76158b67\",\"beginTime\":1620720000000,\"endTime\":1686040200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"c30be516-f669-404c-9519-24e875f1049e\",\"beginTime\":1620806400000,\"endTime\":1686126600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"65f33294-adb0-4c5b-b5f5-76021c39f48a\",\"beginTime\":1620892800000,\"endTime\":1686213000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"0eaabb1c-6440-4c0c-b8ac-d9a3ee3b9f1c\",\"beginTime\":1620979200000,\"endTime\":1686299400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"65049984-d276-4b01-8613-36f22f70dc01\",\"beginTime\":1621065600000,\"endTime\":1686385800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"cbbabb28-e331-494d-b1ac-271667800511\",\"beginTime\":1621152000000,\"endTime\":1686472200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"0a32aefe-e448-4676-9e85-f525e021b995\",\"beginTime\":1621238400000,\"endTime\":1686558600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"aa41eeef-519b-4d62-a8d5-f42f82fe3a26\",\"beginTime\":1621324800000,\"endTime\":1686645000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"edc64edc-ef0a-46db-9b5f-66bc819881b0\",\"beginTime\":1621411200000,\"endTime\":1686731400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"d6e77c8f-7c5e-45cc-ad0e-cdb6199b3dae\",\"beginTime\":1621497600000,\"endTime\":1686817800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"a905061a-c7bb-42b3-8bb0-5104c7deb2bb\",\"beginTime\":1621584000000,\"endTime\":1686904200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"2ee7b648-3f9c-4b96-8efb-187b44e7405f\",\"beginTime\":1621670400000,\"endTime\":1686990600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"022d92c4-f0b4-4113-a158-6ee03ead7293\",\"beginTime\":1621756800000,\"endTime\":1687077000000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"5bc2e243-feb7-42e4-a666-f743a9c637f3\",\"beginTime\":1621843200000,\"endTime\":1687163400000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"f964ccf6-4b20-441b-a85b-730a74acaf9f\",\"beginTime\":1621929600000,\"endTime\":1687249800000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"45ce80fb-6c80-43ce-a3c4-4ea0c28e80eb\",\"beginTime\":1622016000000,\"endTime\":1687336200000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"95f4d7f1-02b7-448a-9e2b-b8ff867bb3e3\",\"beginTime\":1622102400000,\"endTime\":1687422600000,\"roomStatus\":\"Idle\"},{\"roomUUID\":\"c475e3e4-cca9-4d83-8436-fb94f3238f81\",\"beginTime\":1622188800000,\"endTime\":1687509000000,\"roomStatus\":\"Idle\"}]}"
//    val roomPeriodic = Gson().fromJson(json, RoomDetailPeriodic::class.java)
//    LazyColumn {
//        item {
//            PeriodicSubRoomsDisplay(roomPeriodic.rooms)
//        }
//    }
//}

//@Composable
//@Preview(backgroundColor = 0xFFFFFFFF)
//private fun BasePeriodicDisplayPreview() {
//    val json = "{\n" +
//            "        \"periodic\": {\n" +
//            "            \"ownerUUID\": \"722f7f6d-cc0f-4e63-a543-446a3b7bd659\",\n" +
//            "            \"ownerUserName\": \"冯利斌\",\n" +
//            "            \"roomType\": \"OneToOne\",\n" +
//            "            \"endTime\": 1622188800000,\n" +
//            "            \"rate\": 50,\n" +
//            "            \"title\": \"超长周期房间\",\n" +
//            "            \"weeks\": [\n" +
//            "                0,\n" +
//            "                1,\n" +
//            "                2,\n" +
//            "                3,\n" +
//            "                4,\n" +
//            "                5,\n" +
//            "                6\n" +
//            "            ]\n" +
//            "        }"
//    val roomPeriodic = Gson().fromJson(json, RoomPeriodic::class.java)
//    FlatAndroidTheme {
//        PeriodicInfoDisplay(roomPeriodic, 20)
//    }
//}

//@Composable
//@Preview(backgroundColor = 0xFFFFFFFF)
//private fun InviteDialogPreview() {
//    InviteDialog(openDialog = remember { mutableStateOf(false) }, roomInfo = )
//}


internal sealed class DetailUiAction {
    object Back : DetailUiAction()
    object Invite : DetailUiAction()
    data class EnterRoom(val roomUUID: String, val periodicUUID: String?) : DetailUiAction()
    data class Playback(val roomUUID: String) : DetailUiAction()
    object ShowAllRooms : DetailUiAction()
    object ModifyRoom : DetailUiAction()
    object CancelRoom : DetailUiAction()
    object DeleteRoom : DetailUiAction()

    // AllRoom
    object AllRoomBack : DetailUiAction()
}