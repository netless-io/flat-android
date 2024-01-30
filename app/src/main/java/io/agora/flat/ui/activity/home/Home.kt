package io.agora.flat.ui.activity.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.IconButton
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Red_1
import io.agora.flat.ui.theme.Red_3
import io.agora.flat.ui.theme.isDarkTheme
import io.agora.flat.ui.theme.isTabletMode

@Composable
fun HomeScreen(
    onOpenRoomCreate: () -> Unit,
    onOpenRoomJoin: () -> Unit,
    onOpenRoomDetail: (roomUUID: String, periodicUUID: String?) -> Unit,
    onOpenSetting: () -> Unit,
    onOpenUserProfile: () -> Unit,
    onOpenHistory: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val context = LocalContext.current
    val viewState by viewModel.state.collectAsState()

    HomeScreen(
        viewState = viewState,
        onOpenRoomCreate = onOpenRoomCreate,
        onOpenRoomJoin = onOpenRoomJoin,
        onOpenRoomDetail = onOpenRoomDetail,
        onOpenRoom = { roomUUID, periodicUUID -> Navigator.launchRoomPlayActivity(context, roomUUID, periodicUUID) },
        onOpenSetting = onOpenSetting,
        onOpenHistory = onOpenHistory,
        onReloadRooms = viewModel::reloadRooms,
        onSetNetwork = { Navigator.gotoNetworkSetting(context) }
    )
}

@Composable
private fun HomeScreen(
    viewState: HomeUiState,
    onOpenRoomCreate: () -> Unit,
    onOpenRoomJoin: () -> Unit,
    onOpenRoomDetail: (roomUUID: String, periodicUUID: String?) -> Unit,
    onOpenRoom: (roomUUID: String, periodicUUID: String?) -> Unit,
    onOpenSetting: () -> Unit,
    onOpenHistory: () -> Unit,
    onReloadRooms: () -> Unit,
    onSetNetwork: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        // 顶部栏
        FlatHomeTopBar(
            userAvatar = viewState.userInfo.avatar,
            onOpenSetting = onOpenSetting,
            onOpenHistory = onOpenHistory
        )
        if (!viewState.networkActive) FlatNetworkError { onSetNetwork() }
        // 操作区
        TopOperations(onOpenRoomCreate = onOpenRoomCreate, onOpenRoomJoin = onOpenRoomJoin)
        // 房间列表区
        FlatSwipeRefresh(refreshing = viewState.refreshing, onRefresh = onReloadRooms) {
            HomeRoomList(
                Modifier.fillMaxSize(),
                viewState.roomList,
                onGotoRoomDetail = onOpenRoomDetail,
                onOpenRoom = onOpenRoom
            )
        }
    }
}

@Composable
fun FlatNetworkError(onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(40.dp)
            .clickable(onClick = onClick)
            .background(Red_1)
            .padding(horizontal = 16.dp)
    ) {
        FlatTextBodyOne(
            stringResource(R.string.network_error),
            Modifier.align(Alignment.CenterStart),
            Red_3
        )
        Image(painterResource(R.drawable.ic_arrow_right_red), "", Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun TopOperations(onOpenRoomCreate: () -> Unit, onOpenRoomJoin: () -> Unit) {
    Row(Modifier.fillMaxWidth()) {
        OperationItem(Modifier.weight(1f), R.drawable.ic_home_join_room, R.string.join_room, onOpenRoomJoin)
        OperationItem(Modifier.weight(1f), R.drawable.ic_home_quick_start, R.string.quick_start_room, onOpenRoomCreate)
    }
}

@Composable
private fun OperationItem(
    modifier: Modifier = Modifier,
    @DrawableRes id: Int,
    @StringRes tip: Int,
    onClick: () -> Unit
) {
    Box(modifier, Alignment.TopCenter) {
        Column(
            Modifier
                .padding(vertical = 16.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rememberRipple(bounded = false),
                    onClick = onClick,
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = id),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(8.dp))
            FlatTextCaption(stringResource(tip), color = FlatTheme.colors.textPrimary)
        }
    }
}


@Composable
fun FlatHomeTopBar(
    userAvatar: String,
    onOpenSetting: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    FlatMainTopAppBar(stringResource(R.string.title_home)) {
        IconButton(onClick = onOpenHistory) {
            Image(
                painter = painterResource(R.drawable.ic_history),
                contentDescription = null,
                modifier = Modifier.size(24.dp, 24.dp)
            )
        }
        if (!isTabletMode()) {
            IconButton(onClick = onOpenSetting) {
                FlatAvatar(userAvatar, size = 24.dp)
            }
        }
    }
}

@Composable
private fun HomeRoomList(
    modifier: Modifier,
    roomList: List<RoomInfo>,
    onGotoRoomDetail: (String, String?) -> Unit,
    onOpenRoom: (String, String?) -> Unit,
) {
    val imgRes = if (isDarkTheme()) R.drawable.img_room_list_empty_dark else R.drawable.img_room_list_empty_light

    if (roomList.isEmpty()) {
        EmptyView(
            modifier = modifier.verticalScroll(rememberScrollState()),
            imgRes = imgRes,
            message = R.string.home_no_history_room_tip
        )
    } else {
        LazyColumn(modifier) {
            items(count = roomList.size, key = { index: Int ->
                roomList[index].roomUUID
            }) {
                RoomItem(
                    roomInfo = roomList[it],
                    onItemClick = { onGotoRoomDetail(roomList[it].roomUUID, roomList[it].periodicUUID) },
                    onStartClick = { onOpenRoom(roomList[it].roomUUID, roomList[it].periodicUUID) },
                )
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 20.dp), Alignment.TopCenter
                ) {
                    FlatTextCaption(stringResource(R.string.loaded_all))
                }
            }
        }
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
fun HomePreview() {
    val viewState = HomeUiState(
        networkActive = false
    )
    FlatPage {
        HomeScreen(
            viewState = viewState,
            onOpenRoomCreate = {},
            onOpenRoomJoin = {},
            onOpenRoomDetail = { _, _ -> },
            onOpenRoom = { _, _ -> },
            onOpenSetting = {},
            onOpenHistory = {},
            onReloadRooms = {},
            onSetNetwork = {}
        )
    }
}