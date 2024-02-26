package io.agora.flat.ui.activity.room

import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
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
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Red_6
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.ui.theme.isTabletMode
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
    var showAllRooms by remember { mutableStateOf(false) }
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

            DetailUiAction.ShowAllRooms -> showAllRooms = true
            DetailUiAction.AllRoomBack -> showAllRooms = false
            DetailUiAction.CancelRoom, DetailUiAction.DeleteRoom -> viewModel.cancelRoom()
            DetailUiAction.ModifyRoom -> {}
            is DetailUiAction.CopyRoomID -> {
                clipboard.putText(action.roomUUID)
                context.showToast(R.string.copy_success)
            }
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
            visible = showAllRooms,
            enter = fadeIn(initialAlpha = 0.3F, animationSpec = tween()),
            exit = fadeOut(animationSpec = tween())
        ) {
            PeriodicDetailScreen(viewState, actioner = actioner)
        }

        if (showInvite) {
            InviteDialog(viewState.roomInfo!!, { showInvite = false }) {
                clipboard.putText(it)
                context.showToast(R.string.copy_success)
                showInvite = false
            }
        }
    }
}

@Composable
private fun RoomDetailScreen(viewState: RoomDetailViewState, actioner: (DetailUiAction) -> Unit) {
    Column {
        BackTopAppBar(
            title = viewState.roomInfo?.title ?: stringResource(R.string.title_room_detail),
            onBackPressed = { actioner(DetailUiAction.Back) },
        ) {
            viewState.roomInfo?.run {
                AppBarMoreButton(viewState.isOwner, roomStatus, viewState.periodicRoomInfo != null, actioner)
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f),
            Alignment.Center,
        ) {
            if (viewState.roomInfo != null) {
                val roomInfo = viewState.roomInfo
                Column(Modifier.fillMaxWidth()) {
                    RomeInfoDisplay(roomInfo) { actioner(DetailUiAction.CopyRoomID(it)) }
                    BottomOperations(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f), roomInfo, actioner
                    )
                }
            }
            if (viewState.loading) {
                FlatPageLoading()
            }
        }
    }
}

@Composable
private fun RomeInfoDisplay(roomInfo: UIRoomInfo, onCopy: (String) -> Unit) {
    val type = when (roomInfo.roomType) {
        RoomType.BigClass -> stringResource(R.string.room_type_big_class)
        RoomType.SmallClass -> stringResource(R.string.room_type_small_class)
        RoomType.OneToOne -> stringResource(R.string.room_type_one_to_one)
    }
    val time = "${FlatFormatter.date(roomInfo.beginTime)} ${
        FlatFormatter.timeDuring(roomInfo.beginTime, roomInfo.endTime)
    }"
    val state = when (roomInfo.roomStatus) {
        RoomStatus.Idle -> stringResource(R.string.home_room_state_idle)
        RoomStatus.Started, RoomStatus.Paused -> stringResource(R.string.home_room_state_started)
        RoomStatus.Stopped -> stringResource(R.string.home_room_state_end)
    }
    val inviteCode = roomInfo.inviteCode

    Spacer(modifier = Modifier.height(16.dp))
    Row(Modifier.padding(horizontal = 16.dp)) {
        Image(painterResource(R.drawable.ic_room_detail_type), contentDescription = "")
        Spacer(Modifier.width(8.dp))
        Column {
            FlatTextSubtitle(stringResource(R.string.room_type))
            Spacer(Modifier.height(8.dp))
            FlatTextBodyTwo(type, color = FlatTheme.colors.textPrimary)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(Modifier.padding(horizontal = 16.dp)) {
        Image(painterResource(R.drawable.ic_room_detail_id), contentDescription = "")
        Spacer(Modifier.width(8.dp))
        Column {
            FlatTextSubtitle(stringResource(R.string.room_id))
            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.clickable { onCopy(inviteCode) },
                verticalAlignment = Alignment.CenterVertically
            ) {
                FlatTextBodyTwo(inviteCode.toInviteCodeDisplay(), color = FlatTheme.colors.textPrimary)
                Spacer(Modifier.width(8.dp))
                Image(painterResource(R.drawable.ic_room_detail_copy), contentDescription = "")
            }
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(Modifier.padding(horizontal = 16.dp)) {
        Image(painterResource(R.drawable.ic_room_detail_time), contentDescription = "")
        Spacer(Modifier.width(8.dp))
        Column {
            FlatTextSubtitle("时间")
            Spacer(Modifier.height(8.dp))
            FlatTextBodyTwo(time, color = FlatTheme.colors.textPrimary)
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    Row(Modifier.padding(horizontal = 16.dp)) {
        Image(painterResource(R.drawable.ic_room_detail_state), contentDescription = "")
        Spacer(Modifier.width(8.dp))
        Column {
            FlatTextSubtitle("状态")
            Spacer(Modifier.height(8.dp))
            FlatTextBodyTwo(state, color = FlatTheme.colors.textPrimary)
        }
    }
}

@ExperimentalAnimationApi
@Composable
private fun PeriodicDetailScreen(viewState: RoomDetailViewState, actioner: (DetailUiAction) -> Unit) {
    FlatColumnPage {
        BackHandler(onBack = { actioner(DetailUiAction.AllRoomBack) })
        BackTopAppBar(stringResource(R.string.title_room_all), onBackPressed = { actioner(DetailUiAction.AllRoomBack) })
        viewState.periodicRoomInfo?.run {
            LazyColumn {
                item { PeriodicInfoDisplay(periodic, rooms.size) }
                item { PeriodicSubRoomsDisplay(rooms) }
            }
        }
    }
}

@Composable
private fun AppBarMoreButton(
    isOwner: Boolean,
    roomStatus: RoomStatus,
    isPeriodic: Boolean,
    actioner: (DetailUiAction) -> Unit
) {
    Box {
        var expanded by remember { mutableStateOf(false) }

        @Suppress("NAME_SHADOWING")
        val actioner: (DetailUiAction) -> Unit = {
            expanded = false
            actioner(it)
        }

        IconButton(onClick = { expanded = true }, enabled = !(isOwner && roomStatus == RoomStatus.Started)) {
            Icon(Icons.Outlined.MoreHoriz, contentDescription = null)
        }

        DropdownMenu(
            modifier = Modifier.wrapContentSize(),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            if (isPeriodic) {
                DropdownMenuItem(onClick = { actioner(DetailUiAction.ShowAllRooms) }) {
                    FlatTextBodyOne(stringResource(R.string.show_all))
                }
            }
            if (isOwner && roomStatus == RoomStatus.Idle) {
                DropdownMenuItem(onClick = { actioner(DetailUiAction.ModifyRoom) }) {
                    FlatTextBodyOne(stringResource(R.string.modify_room))
                }
                DropdownMenuItem(onClick = { actioner(DetailUiAction.CancelRoom) }) {
                    FlatTextBodyOne(stringResource(R.string.cancel_room), color = Red_6)
                }
            }
            if (!isOwner && roomStatus != RoomStatus.Stopped) {
                DropdownMenuItem(onClick = { actioner(DetailUiAction.CancelRoom) }) {
                    FlatTextBodyOne(stringResource(R.string.remove_room), color = Red_6)
                }
            }
            if (roomStatus == RoomStatus.Stopped) {
                DropdownMenuItem(onClick = { actioner(DetailUiAction.DeleteRoom) }) {
                    FlatTextBodyOne(stringResource(R.string.delete_history), color = Red_6)
                }
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
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                FlatNormalVerticalSpacer()
                FlatTextBodyOne(stringArrayResource(R.array.months)[month], Modifier.padding(vertical = 4.dp))
                FlatNormalVerticalSpacer()
                FlatDivider()
                FlatSmallVerticalSpacer()
            }
            lastMonth = month
        }
        PeriodicSubRoomItem("$day", dayOfWeekText, timeDuring, it.roomStatus)
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
            dayText,
            Modifier
                .padding(horizontal = 16.dp)
                .widthIn(min = 16.dp)
        )
        Spacer(Modifier.weight(1f))
        FlatTextBodyTwo(dayOfWeekText, Modifier.padding(horizontal = 8.dp))
        Spacer(Modifier.weight(1f))
        FlatTextBodyTwo(timeDuring, Modifier.padding(horizontal = 16.dp))
        FlatRoomStatusText(roomStatus, Modifier.padding(horizontal = 24.dp))
        Box(Modifier.padding(horizontal = 12.dp, vertical = 5.dp), Alignment.Center) {
            Icon(Icons.Outlined.MoreHoriz, null,
                Modifier
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
                    .padding(16.dp), actioner = actioner
            )
        } else {
            Operations(
                roomInfo,
                Modifier
                    .fillMaxWidth()
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
    RoomStatus.Idle, RoomStatus.Paused, RoomStatus.Started -> Row(modifier) {
        FlatSmallSecondaryTextButton(stringResource(R.string.copy_invite)) {
            actioner(DetailUiAction.Invite)
        }
        FlatNormalHorizontalSpacer()
        FlatSmallPrimaryTextButton(stringResource(R.string.enter), onClick = {
            actioner(DetailUiAction.EnterRoom(roomInfo.roomUUID, roomInfo.periodicUUID))
        })
    }

    RoomStatus.Stopped -> Row(modifier) {
        FlatSmallPrimaryTextButton(stringResource(id = R.string.replay),
            enabled = roomInfo.hasRecord,
            onClick = { actioner(DetailUiAction.Playback(roomInfo.roomUUID)) })
    }
}

@Composable
private fun Operations(
    roomInfo: UIRoomInfo,
    modifier: Modifier,
    actioner: (DetailUiAction) -> Unit,
) = when (roomInfo.roomStatus) {
    RoomStatus.Idle, RoomStatus.Paused, RoomStatus.Started -> Column(modifier) {
        FlatSecondaryTextButton(stringResource(R.string.copy_invite), onClick = {
            actioner(DetailUiAction.Invite)
        })
        FlatNormalVerticalSpacer()
        FlatPrimaryTextButton(stringResource(R.string.enter), onClick = {
            actioner(DetailUiAction.EnterRoom(roomInfo.roomUUID, roomInfo.periodicUUID))
        })
    }

    RoomStatus.Stopped -> Column(modifier) {
        FlatPrimaryTextButton(stringResource(id = R.string.replay), enabled = roomInfo.hasRecord, onClick = {
            actioner(DetailUiAction.Playback(roomInfo.roomUUID))
        })
    }
}

@Composable
private fun InviteDialog(roomInfo: UIRoomInfo, onDismissRequest: () -> Unit, onCopy: (String) -> Unit) {
    val datetime = FlatFormatter.dateWithDuring(roomInfo.beginTime, roomInfo.endTime)
    val linkCode = if (roomInfo.isPmi) roomInfo.inviteCode else roomInfo.roomUUID
    val inviteLink = "${roomInfo.baseInviteUrl}/join/$linkCode"

    val inviteText = stringResource(
        if (roomInfo.isPmi) R.string.invite_pmi_text_format else R.string.invite_text_format,
        roomInfo.username,
        roomInfo.title,
        datetime,
        roomInfo.inviteCode.toInviteCodeDisplay(),
        inviteLink
    )

    val inviteTitle = stringResource(
        if (roomInfo.isPmi) R.string.invite_pmi_title_format else R.string.invite_title_format,
        roomInfo.username
    )

    Dialog(onDismissRequest) {
        Surface(shape = Shapes.large) {
            Column(Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                Text(inviteTitle)
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
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
private fun RoomDetailPreview() {
    val state = RoomDetailViewState(
        roomInfo = UIRoomInfo(
            roomUUID = "722f7f6d-cc0f-4e63-a543-446a3b7bd659",
            periodicUUID = "722f7f6d-cc0f-4e63-a543-446a3b7bd659",
            title = "Long Long Room Theme Title Long Long Room Theme Title",
            roomType = RoomType.SmallClass,
            inviteCode = "1111111111-1111111111-1111111111-1111111111",
            username = "UserXXX",
            baseInviteUrl = "",
        ),
        userUUID = "722f7f6d-cc0f-4e63-a543-446a3b7bd659",
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
        ) {}
    }
}

internal sealed class DetailUiAction {
    object Back : DetailUiAction()
    object Invite : DetailUiAction()
    data class EnterRoom(val roomUUID: String, val periodicUUID: String?) : DetailUiAction()
    data class Playback(val roomUUID: String) : DetailUiAction()
    data class CopyRoomID(val roomUUID: String) : DetailUiAction()
    object ShowAllRooms : DetailUiAction()
    object ModifyRoom : DetailUiAction()
    object CancelRoom : DetailUiAction()
    object DeleteRoom : DetailUiAction()

    // AllRoom
    object AllRoomBack : DetailUiAction()
}