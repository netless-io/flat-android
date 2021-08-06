package io.agora.flat.ui.activity.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.coil.rememberCoilPainter
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import com.google.gson.Gson
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.ui.compose.CustomInteractionSource
import io.agora.flat.ui.compose.FlatColumnPage
import io.agora.flat.ui.compose.FlatRoomStatusText
import io.agora.flat.ui.compose.FlatTopAppBar
import io.agora.flat.ui.theme.*
import io.agora.flat.util.FlatFormatter

@Composable
fun Home() {
    val viewModel = viewModel(HomeViewModel::class.java)
    val viewState by viewModel.state.collectAsState()

    Home(viewState) { action ->
        when (action) {
            HomeViewAction.Reload -> viewModel.reloadRoomList()
            is HomeViewAction.SelectCategory -> viewModel.onRoomCategorySelected(action.category)
        }
    }
}

@Composable
private fun Home(viewState: HomeViewState, actioner: (HomeViewAction) -> Unit) {
    FlatColumnPage {
        // 顶部栏
        FlatHomeTopBar(userAvatar = viewState.userInfo.avatar)
        // 操作区
        TopOperations()
        // 房间列表区
        SwipeRefresh(
            state = rememberSwipeRefreshState(viewState.refreshing),
            onRefresh = { actioner(HomeViewAction.Reload) },
            indicator = { state, trigger ->
                SwipeRefreshIndicator(
                    state = state,
                    refreshTriggerDistance = trigger,
                    contentColor = MaterialTheme.colors.primary,
                )
            }) {
            HomeRoomLists(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                selectedHomeCategory = viewState.selectedHomeCategory,
                onCategorySelected = { actioner(HomeViewAction.SelectCategory(it)) },
                roomList = viewState.roomList,
                roomHistory = viewState.roomHistoryList
            )
        }
    }
}

@Composable
private fun TopOperations() {
    val context = LocalContext.current

    Row(Modifier.fillMaxWidth()) {
        OperationItem(R.drawable.ic_home_create_room, R.string.create_room) {
            Navigator.launchCreateRoomActivity(context)
        }
        OperationItem(R.drawable.ic_home_join_room, R.string.join_room) {
            Navigator.launchJoinRoomActivity(context)
        }
        // Perhaps, the future will support
        // OperationItem(R.drawable.ic_home_subscribe_room, R.string.subscribe_room) {
        //     // Navigator.launchSubscribeRoomActivity(context)
        //     context.showDebugToast(R.string.toast_in_development)
        // }
    }
}

@Composable
private fun RowScope.OperationItem(@DrawableRes id: Int, @StringRes tip: Int, onClick: () -> Unit) {
    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .padding(top = 16.dp, bottom = 24.dp)
                .clickable(onClick = onClick),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id),
                contentDescription = "",
                contentScale = ContentScale.Crop,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(stringResource(tip), style = FlatSmallTextStyle)
        }
    }
}

@Composable
fun FlatHomeTopBar(userAvatar: String) {
    FlatTopAppBar(
        title = {
            Text(stringResource(R.string.title_home), style = FlatTitleTextStyle)
        },
        actions = {
            Box {
                val context = LocalContext.current
                var expanded by remember { mutableStateOf(false) }

                IconButton(
                    onClick = { expanded = true }) {
                    Image(
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .clip(shape = RoundedCornerShape(12.dp)),
                        painter = rememberCoilPainter(userAvatar),
                        contentScale = ContentScale.Crop,
                        contentDescription = null
                    )
                }
                DropdownMenu(
                    modifier = Modifier.wrapContentSize(),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(onClick = {
                        Navigator.launchSettingActivity(context)
                        expanded = false
                    }) {
                        Image(painterResource(R.drawable.ic_user_profile_head), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("我的资料", Modifier.width(100.dp))
                    }
                    DropdownMenuItem(onClick = {
                        Navigator.launchMyProfileActivity(context)
                        expanded = false
                    }) {
                        Image(painterResource(R.drawable.ic_user_profile_aboutus), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("个人信息", Modifier.width(100.dp))
                    }
                }
            }
        }
    )
}

@Composable
fun HomeRoomLists(
    modifier: Modifier,
    selectedHomeCategory: RoomCategory,
    onCategorySelected: (RoomCategory) -> Unit,
    roomList: List<RoomInfo>,
    roomHistory: List<RoomInfo>,
) {
    Column(modifier) {
        HomeRoomTabs(
            listOf(RoomCategory.Current, RoomCategory.History),
            selectedHomeCategory,
            onCategorySelected = onCategorySelected,
            Modifier.fillMaxWidth()
        )

        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            when (selectedHomeCategory) {
                RoomCategory.Current -> {
                    RoomList(Modifier.fillMaxSize(), roomList, RoomCategory.Current)
                }
                RoomCategory.History -> {
                    RoomList(Modifier.fillMaxSize(), roomHistory, RoomCategory.History)
                }
            }
        }

    }
}

@Composable
private fun HomeRoomTabs(
    categories: List<RoomCategory>,
    selectedCategory: RoomCategory,
    onCategorySelected: (RoomCategory) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = categories.indexOfFirst { it == selectedCategory }
    val indicator = @Composable { tabPositions: List<TabPosition> ->
        HomeTabIndicator(
            Modifier.tabIndicatorOffset(tabPositions[selectedIndex]),
            FlatColorBlue
        )
    }

    TabRow(
        selectedTabIndex = selectedIndex,
        indicator = indicator,
        backgroundColor = MaterialTheme.colors.surface,
        divider = {},
        modifier = modifier
    ) {
        categories.forEachIndexed { index, category ->
            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(
                        text = when (category) {
                            RoomCategory.Current -> stringResource(R.string.home_room_list)
                            RoomCategory.History -> stringResource(R.string.home_history_record)
                        },
                        style = MaterialTheme.typography.body2
                    )
                }
            )
        }
    }
}

@Composable
fun HomeTabIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface,
) {
    Spacer(
        modifier
            .height(2.dp)
            .background(
                color,
                RoundedCornerShape(topStartPercent = 100, topEndPercent = 100),
            )
    )
}

@Composable
fun RoomList(modifier: Modifier, roomList: List<RoomInfo>, category: RoomCategory) {
    val context = LocalContext.current

    if (roomList.isEmpty()) {
        val imgRes = when (category) {
            RoomCategory.Current -> R.drawable.img_home_no_room
            RoomCategory.History -> R.drawable.img_home_no_history
        }
        val message = when (category) {
            RoomCategory.Current -> R.string.home_no_room_tip
            RoomCategory.History -> R.string.home_no_history_room_tip
        }
        EmptyView(imgRes, message, modifier)
    } else {
        LazyColumn(modifier) {
            items(count = roomList.size, key = { index: Int ->
                roomList[index].roomUUID
            }) {
                RoomListItem(
                    roomList[it],
                    Modifier.clickable(
                        indication = LocalIndication.current,
                        interactionSource = remember { CustomInteractionSource() }) {
                        Navigator.launchRoomDetailActivity(
                            context,
                            roomList[it].roomUUID,
                            roomList[it].periodicUUID
                        )
                    })
            }
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp), Alignment.TopCenter
                ) {
                    Text(
                        text = stringResource(R.string.loaded_all),
                        style = FlatSmallTipTextStyle
                    )
                }
            }
        }
    }
}

@Composable
fun RoomListItem(roomInfo: RoomInfo, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography

    Column(modifier) {
        if (roomInfo.showDayHead) {
            Row(
                Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_home_calendar),
                    contentDescription = null
                )
                Spacer(modifier = Modifier.size(4.dp))
                Text(
                    text = FlatFormatter.dateMisc(roomInfo.beginTime),
                    style = typography.body1
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .padding(16.dp, 12.dp)
        ) {
            Text(
                modifier = Modifier.align(Alignment.TopStart),
                text = roomInfo.title,
                style = typography.body1
            )
            Text(
                modifier = Modifier.align(Alignment.BottomStart),
                text = "${FlatFormatter.time(roomInfo.beginTime)} ~ ${FlatFormatter.time(roomInfo.endTime)}",
                style = typography.body2
            )
            FlatRoomStatusText(
                roomStatus = roomInfo.roomStatus,
                modifier = Modifier.align(Alignment.BottomEnd)
            )
        }
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(0.5.dp)
                .background(FlatColorDivider),
        )
    }
}

@Preview(showBackground = true)
@Composable
fun RoomListItemPreview() {
    val roomStr = "{\n" +
            "            \"roomUUID\": \"c97348d5-87e4-4154-8e10-c939ed9cb041\",\n" +
            "            \"periodicUUID\": null,\n" +
            "            \"ownerUUID\": \"722f7f6d-cc0f-4e63-a543-446a3b7bd659\",\n" +
            "            \"roomType\": \"OneToOne\",\n" +
            "            \"title\": \"XXX创建的房间\",\n" +
            "            \"beginTime\": 1615371918318,\n" +
            "            \"endTime\": 1615371955296,\n" +
            "            \"roomStatus\": \"Stopped\",\n" +
            "            \"ownerName\": \"XXX\",\n" +
            "            \"hasRecord\": true\n" +
            "        }"
    val roomInfo = Gson().fromJson(roomStr, RoomInfo::class.java)
    roomInfo.showDayHead = true
    RoomListItem(roomInfo, Modifier)
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    val viewState = HomeViewState(

    )
    Home(viewState) {

    }
}