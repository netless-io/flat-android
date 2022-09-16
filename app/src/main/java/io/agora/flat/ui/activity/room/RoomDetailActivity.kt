package io.agora.flat.ui.activity.room

import android.os.Bundle
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.common.android.rememberAndroidClipboardController
import io.agora.flat.data.model.*
import io.agora.flat.ui.activity.base.BaseComposeActivity
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.ui.viewmodel.RoomDetailViewModel
import io.agora.flat.ui.viewmodel.RoomDetailViewState
import io.agora.flat.ui.viewmodel.UIRoomInfo
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.delayLaunch
import io.agora.flat.util.showToast
import io.agora.flat.util.toInviteCodeDisplay
import java.util.*

@AndroidEntryPoint
class RoomDetailActivity : BaseComposeActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {

        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun RoomDetailScreen(
    navController: NavController,
    viewModel: RoomDetailViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()
    val cancelSuccess = viewModel.cancelSuccess.collectAsState()
    var visible by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    var showInvite by remember { mutableStateOf(false) }
    val clipboard = rememberAndroidClipboardController()

    val actioner: (DetailUiAction) -> Unit = { action ->
        when (action) {
            DetailUiAction.Back -> {
                navController.popBackStack()
            }
            is DetailUiAction.EnterRoom -> {
                Navigator.launchRoomPlayActivity(context, action.roomUUID, action.periodicUUID)
                scope.delayLaunch {
                    navController.popBackStack()
                }
            }
            DetailUiAction.Invite -> {
                showInvite = true
            }
            is DetailUiAction.Playback -> {
                Navigator.launchPlaybackActivity(context, action.roomUUID)
                scope.delayLaunch {
                    navController.popBackStack()
                }
            }
            DetailUiAction.ShowAllRooms -> visible = true
            DetailUiAction.AllRoomBack -> visible = false
            DetailUiAction.CancelRoom, DetailUiAction.DeleteRoom -> viewModel.cancelRoom()
            DetailUiAction.ModifyRoom -> {}
        }
    }

    if (cancelSuccess.value) {
        LaunchedEffect(cancelSuccess) {
            if (cancelSuccess.value) {
                navController.popBackStack()
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        RoomDetailScreen(viewState, actioner = actioner)
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(initialAlpha = 0.3F, animationSpec = tween()),
            exit = fadeOut(animationSpec = tween())
        ) {
            PeriodicDetailScreen(viewState, actioner = actioner)
        }

        if (showInvite) {
            InviteDialog(viewState.roomInfo!!, { showInvite = false }) {
                clipboard.putText(it)
                showInvite = false
                context.showToast(R.string.copy_success)
            }
        }
    }
}

@Composable
private fun RoomDetailScreen(viewState: RoomDetailViewState, actioner: (DetailUiAction) -> Unit) {
    Column {
        BackTopAppBar(
            viewState.roomInfo?.title ?: stringResource(R.string.title_room_detail),
            onBackPressed = { actioner(DetailUiAction.Back) }) {
            viewState.roomInfo?.run {
                AppBarMoreButton(viewState.isOwner, roomStatus, actioner)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f), Alignment.Center
        ) {
            if (viewState.roomInfo != null) {
                val roomInfo = viewState.roomInfo
                Column(Modifier.fillMaxWidth()) {
                    TimeDisplay(
                        begin = roomInfo.beginTime,
                        end = roomInfo.endTime,
                        state = roomInfo.roomStatus
                    )
                    if (viewState.isPeriodicRoom) {
                        val periodicRoomInfo = viewState.periodicRoomInfo!!
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .animateContentSize(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            FlatTextBodyOneSecondary(
                                stringResource(
                                    R.string.view_all_room_format,
                                    periodicRoomInfo.rooms.size
                                ),
                                Modifier
                                    .padding(4.dp)
                                    .clickable { actioner(DetailUiAction.ShowAllRooms) },
                                color = MaterialTheme.colors.primary
                            )
                            FlatNormalVerticalSpacer()
                        }
                    }
                    MoreRomeInfoDisplay(roomInfo.inviteCode, roomInfo.roomType, viewState.isPeriodicRoom)

                    BottomOperations(MaxWidthSpread, roomInfo, actioner)
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
private fun PeriodicDetailScreen(viewState: RoomDetailViewState, actioner: (DetailUiAction) -> Unit) {
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
                item { PeriodicInfoDisplay(periodic, rooms.size) }

                item { PeriodicSubRoomsDisplay(rooms) }
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

        DetailDropdownMenu(isOwner, roomStatus, expanded, { expanded = false }, {
            expanded = false
            actioner(it)
        })
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
                FlatTextBodyOne(stringResource(R.string.modify_room))
            }
            DropdownMenuItem(onClick = { actioner(DetailUiAction.CancelRoom) }) {
                FlatTextBodyOne(stringResource(R.string.cancel_room), color = FlatColorRed)
            }
        }
        if (!isOwner && roomStatus != RoomStatus.Stopped) {
            DropdownMenuItem(onClick = { actioner(DetailUiAction.CancelRoom) }) {
                FlatTextBodyOne(stringResource(R.string.remove_room), color = FlatColorRed)
            }
        }
        if (roomStatus == RoomStatus.Stopped) {
            DropdownMenuItem(onClick = { actioner(DetailUiAction.DeleteRoom) }) {
                FlatTextBodyOne(stringResource(R.string.delete_history), color = FlatColorRed)
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
        val month = cal.get(Calendar.MONTH)
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val dayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val timeBegin = "${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE)}"

        cal.timeInMillis = it.endTime
        val timeEnd = "${cal.get(Calendar.HOUR_OF_DAY)}:${cal.get(Calendar.MINUTE)}"

        val dayText = "$day"
        val dayOfWeekText = stringArrayResource(R.array.weekdays_short)[when (dayOfWeek) {
            Calendar.SUNDAY -> 0
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> 0
        }]
        val timeDuring = "${timeBegin}~${timeEnd}"
        if (month != lastMonth) {
            val monthText = stringArrayResource(R.array.months)[month]
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                FlatNormalVerticalSpacer()
                FlatTextBodyOne(monthText, Modifier.padding(vertical = 4.dp))
                FlatNormalVerticalSpacer()
                FlatDivider()
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
        FlatTextBodyTwo(
            dayText, Modifier
                .padding(horizontal = 16.dp)
                .widthIn(min = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        FlatTextBodyTwo(dayOfWeekText, Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.weight(1f))
        FlatTextBodyTwo(timeDuring, Modifier.padding(horizontal = 16.dp))
        FlatRoomStatusText(roomStatus, Modifier.padding(horizontal = 24.dp))
        Box(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), Alignment.Center) {
            Icon(Icons.Outlined.MoreHoriz, null, Modifier
                .size(24.dp)
                .clickable { expanded = true })
            DropdownMenu(expanded, { expanded = false }, Modifier.wrapContentSize()) {
                DropdownMenuItem(onClick = { expanded = false }) {
                    FlatTextBodyOne(stringResource(R.string.title_room_detail))
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    FlatTextBodyOne(stringResource(R.string.modify_room))
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    FlatTextBodyOne(stringResource(R.string.cancel_room))
                }
                DropdownMenuItem(onClick = { expanded = false }) {
                    FlatTextBodyOne(stringResource(R.string.copy_invite))
                }
            }
        }
    }
}

@Composable
private fun PeriodicInfoDisplay(roomPeriodic: RoomPeriodic, number: Int) {
    val context = LocalContext.current
    val bgColor = if (isDarkTheme()) Blue_8 else Blue_0

    Box(
        Modifier
            .padding(16.dp)
            .clip(Shapes.medium)
            .background(MaterialTheme.colors.surface)
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            val weekInfo = roomPeriodic.weeks.joinToString(separator = "、") {
                context.resources.getStringArray(R.array.weekdays_short)[when (it) {
                    Week.Sunday -> 0
                    Week.Monday -> 1
                    Week.Tuesday -> 2
                    Week.Wednesday -> 3
                    Week.Thursday -> 4
                    Week.Friday -> 5
                    Week.Saturday -> 6
                }]
            }
            val type = when (roomPeriodic.roomType) {
                RoomType.BigClass -> stringResource(R.string.room_type_big_class)
                RoomType.SmallClass -> stringResource(R.string.room_type_small_class)
                RoomType.OneToOne -> stringResource(R.string.room_type_one_to_one)
            }
            val typeInfo = "房间类型：${type}（周期）"
            val desc = "结束于 ${FlatFormatter.longDateWithWeek(roomPeriodic.endTime)}，共 $number 场会议"
            FlatTextBodyTwo(weekInfo, Modifier.padding(2.dp), color = MaterialTheme.colors.primary)
            FlatSmallVerticalSpacer()
            FlatTextBodyTwo(typeInfo, Modifier.padding(2.dp))
            FlatSmallVerticalSpacer()
            FlatTextBodyTwo(desc, Modifier.padding(2.dp), maxLines = 1)
        }
    }
}

@Composable
private fun BottomOperations(
    modifier: Modifier,
    roomInfo: UIRoomInfo,
    actioner: (DetailUiAction) -> Unit,
) {
    Box(modifier) {
        if (isTabletMode()) {
            TabletOperations(
                roomInfo,
                Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp),
                actioner = actioner
            )
        } else {
            Operations(
                roomInfo,
                MaxWidth
                    .align(Alignment.BottomCenter)
                    .padding(16.dp, 32.dp),
                actioner = actioner
            )
        }
    }
}

@Composable
private fun TabletOperations(
    roomInfo: UIRoomInfo,
    modifier: Modifier,
    actioner: (DetailUiAction) -> Unit,
) = when (roomInfo.roomStatus) {
    RoomStatus.Idle, RoomStatus.Paused, RoomStatus.Started ->
        Row(modifier) {
            FlatSmallSecondaryTextButton(stringResource(R.string.copy_invite)) {
                actioner(DetailUiAction.Invite)
            }
            FlatNormalHorizontalSpacer()
            FlatSmallPrimaryTextButton(
                enterRoomString(roomInfo.isOwner, roomInfo.roomStatus),
                onClick = {
                    actioner(DetailUiAction.EnterRoom(roomInfo.roomUUID, roomInfo.periodicUUID))
                }
            )
        }
    RoomStatus.Stopped ->
        Row(modifier) {
            FlatSmallPrimaryTextButton(
                stringResource(id = R.string.replay), enabled = roomInfo.hasRecord,
                onClick = { actioner(DetailUiAction.Playback(roomInfo.roomUUID)) }
            )
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
            FlatSecondaryTextButton(stringResource(R.string.copy_invite), onClick = {
                actioner(DetailUiAction.Invite)
            })
            FlatNormalVerticalSpacer()
            FlatPrimaryTextButton(enterRoomString(roomInfo.isOwner, roomInfo.roomStatus), onClick = {
                actioner(DetailUiAction.EnterRoom(roomInfo.roomUUID, roomInfo.periodicUUID))
            })
        }
    RoomStatus.Stopped ->
        Column(modifier) {
            FlatPrimaryTextButton(stringResource(id = R.string.replay), enabled = roomInfo.hasRecord, onClick = {
                actioner(DetailUiAction.Playback(roomInfo.roomUUID))
            })
        }
}

@Composable
private fun enterRoomString(isOwner: Boolean, roomStatus: RoomStatus): String =
    if (isOwner && roomStatus == RoomStatus.Idle) {
        stringResource(id = R.string.start)
    } else {
        stringResource(R.string.enter)
    }

@Composable
private fun InviteDialog(roomInfo: UIRoomInfo, onDismissRequest: () -> Unit, onCopy: (String) -> Unit) {
    val datetime =
        "${FlatFormatter.date(roomInfo.beginTime)} ${FlatFormatter.timeDuring(roomInfo.beginTime, roomInfo.endTime)}"
    val inviteLink = roomInfo.baseInviteUrl + "/join/" + roomInfo.roomUUID
    val inviteText = stringResource(
        R.string.invite_text_format,
        roomInfo.username,
        roomInfo.title,
        datetime,
        roomInfo.inviteCode.toInviteCodeDisplay(),
        inviteLink
    )

    Dialog(onDismissRequest) {
        Surface(shape = Shapes.large) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(stringResource(R.string.invite_title_format, roomInfo.username))
                FlatLargeVerticalSpacer()
                Row {
                    Text(stringResource(R.string.room_theme))
                    Spacer(Modifier.width(16.dp))
                    Spacer(Modifier.weight(1f))
                    Text(roomInfo.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FlatNormalVerticalSpacer()
                Row {
                    Text(stringResource(R.string.room_id))
                    Spacer(Modifier.width(16.dp))
                    Spacer(Modifier.weight(1f))
                    Text(roomInfo.inviteCode.toInviteCodeDisplay(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FlatNormalVerticalSpacer()
                Row {
                    Text(stringResource(R.string.time_start))
                    Spacer(Modifier.width(16.dp))
                    Spacer(Modifier.weight(1f))
                    Text(datetime, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                FlatLargeVerticalSpacer()
                FlatPrimaryTextButton(stringResource(R.string.copy_link_to_invite), onClick = { onCopy(inviteText) })
            }
        }
    }
}

@Composable
private fun MoreRomeInfoDisplay(uuid: String, roomType: RoomType, isPeriodic: Boolean) {
    val type = when (roomType) {
        RoomType.BigClass -> stringResource(R.string.room_type_big_class)
        RoomType.SmallClass -> stringResource(R.string.room_type_small_class)
        RoomType.OneToOne -> stringResource(R.string.room_type_one_to_one)
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        FlatDivider()
        FlatNormalVerticalSpacer()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_room), contentDescription = "")
            Spacer(Modifier.width(4.dp))
            FlatTextBodyOne(stringResource(R.string.room_id))
            Spacer(Modifier.weight(1f))
            FlatTextBodyOneSecondary(uuid.toInviteCodeDisplay(), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FlatLargeVerticalSpacer()
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(painterResource(R.drawable.ic_room_type), contentDescription = "")
            Spacer(Modifier.width(4.dp))
            FlatTextBodyOne(stringResource(R.string.room_type))
            Spacer(Modifier.weight(1f))
            FlatTextBodyOneSecondary(type, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        FlatNormalVerticalSpacer()
        FlatDivider()
    }
}

@Composable
private fun TimeDisplay(begin: Long, end: Long, state: RoomStatus) {
    val context = LocalContext.current

    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(horizontalAlignment = Alignment.End) {
            FlatTextLargeTitle(FlatFormatter.time(begin))
            FlatSmallVerticalSpacer()
            FlatTextBodyOne(FlatFormatter.longDate(begin))
        }
        FlatLargeHorizontalSpacer()
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            FlatSmallVerticalSpacer()
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colors.surface),
                contentAlignment = Alignment.Center
            ) {
                FlatTextBodyTwo(
                    FlatFormatter.diffTime(context, begin, end),
                    Modifier.padding(horizontal = 18.dp, vertical = 3.dp),
                )
            }
            FlatSmallVerticalSpacer()
            RoomState(state, Modifier.padding(4.dp))
        }
        FlatLargeHorizontalSpacer()
        Column(horizontalAlignment = Alignment.Start) {
            FlatTextLargeTitle(FlatFormatter.time(end))
            FlatSmallVerticalSpacer()
            FlatTextBodyTwo(FlatFormatter.longDate(end))
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

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun RoomDetailPreview() {
    val state = RoomDetailViewState(
        roomInfo = UIRoomInfo(
            roomUUID = "722f7f6d-cc0f-4e63-a543-446a3b7bd659",
            title = "Long Long Room Theme Title Long Long Room Theme Title",
            roomType = RoomType.SmallClass,
            inviteCode = "1111111111-1111111111-1111111111-1111111111",
            username = "UserXXX",
            baseInviteUrl = "",
        ),
        userUUID = "722f7f6d-cc0f-4e63-a543-446a3b7bd659"
    )
    FlatPage {
        RoomDetailScreen(state) {}
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun InviteDialogPreview() {
    FlatPage {
        InviteDialog(roomInfo = UIRoomInfo(
            roomUUID = "722f7f6d-cc0f-4e63-a543-446a3b7bd659",
            title = "Long Long Room Theme Title Long Long Room Theme Title",
            roomType = RoomType.SmallClass,
            inviteCode = "1111111111-1111111111-1111111111-1111111111",
            username = "UserXXX",
            baseInviteUrl = "",
        ), {}, {})
    }
}

@Composable
@Preview(device = Devices.PIXEL_C)
private fun BottomOperationsPreview() {
    FlatPage {
        BottomOperations(
            modifier = Modifier.size(width = 400.dp, height = 300.dp),
            roomInfo = UIRoomInfo(
                roomUUID = "722f7f6d-cc0f-4e63-a543-446a3b7bd659",
                title = "Long Long Room Theme Title Long Long Room Theme Title",
                roomType = RoomType.SmallClass,
                inviteCode = "1111111111-1111111111-1111111111-1111111111",
                username = "UserXXX",
                baseInviteUrl = "",
            ),
        ) {
        }
    }
}

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