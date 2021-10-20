package io.agora.flat.ui.activity.home

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.coil.rememberCoilPainter
import com.google.gson.Gson
import io.agora.flat.R
import io.agora.flat.common.Navigator
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.*
import io.agora.flat.util.FlatFormatter

@Composable
fun Home() {
    val context = LocalContext.current
    val viewModel = viewModel(HomeViewModel::class.java)
    val viewState by viewModel.state.collectAsState()

    Home(viewState) { action ->
        when (action) {
            HomeViewAction.Reload -> viewModel.reloadRoomList()
            is HomeViewAction.SelectCategory -> viewModel.onRoomCategorySelected(action.category)
            HomeViewAction.SetNetwork -> Navigator.gotoNetworkSetting(context)
        }
    }
}

@Composable
private fun Home(viewState: HomeViewState, actioner: (HomeViewAction) -> Unit) {
    FlatColumnPage {
        // 顶部栏
        FlatHomeTopBar(userAvatar = viewState.userInfo.avatar)
        if (!viewState.networkActive) FlatNetworkError {
            actioner(HomeViewAction.SetNetwork)
        }
        // 操作区
        TopOperations()
        // 房间列表区
        FlatSwipeRefresh(refreshing = viewState.refreshing, onRefresh = { actioner(HomeViewAction.Reload) }) {
            HomeRoomContent(
                modifier = MaxWidthSpread,
                selectedHomeCategory = viewState.category,
                onCategorySelected = { actioner(HomeViewAction.SelectCategory(it)) },
                roomList = viewState.roomList,
                roomHistory = viewState.historyList
            )
        }
    }
}

@Composable
fun FlatNetworkError(onClick: () -> Unit) {
    Box(Modifier
        .fillMaxWidth()
        .height(40.dp)
        .clickable(onClick = onClick)
        .background(FlatColorRedLight)
        .padding(horizontal = 16.dp)
    ) {
        Text(stringResource(R.string.network_error), Modifier.align(Alignment.CenterStart), FlatColorRed)
        Image(painterResource(R.drawable.ic_arrow_right_red), "", Modifier.align(Alignment.CenterEnd))
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
    Box(Modifier.weight(1f), Alignment.TopCenter) {
        Column(Modifier
            .padding(top = 16.dp, bottom = 24.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 48.dp),
                onClick = onClick,
            ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painterResource(id), null)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(tip), style = FlatSmallTextStyle)
        }
    }
}

@Composable
fun FlatHomeTopBar(userAvatar: String) {
    FlatTopAppBar(
        title = { Text(stringResource(R.string.title_home), style = FlatTitleTextStyle) },
        actions = {
            Box {
                val context = LocalContext.current
                var expanded by remember { mutableStateOf(false) }

                IconButton(onClick = { expanded = true }) {
                    Image(
                        painter = rememberCoilPainter(userAvatar),
                        contentDescription = null,
                        modifier = Modifier
                            .size(24.dp, 24.dp)
                            .clip(shape = RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                val state = LocalLifecycleOwner.current.lifecycle.observeAsSate()
                if (state.value == Lifecycle.Event.ON_PAUSE) {
                    expanded = false
                }
                DropdownMenu(
                    modifier = Modifier.wrapContentSize(),
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    DropdownMenuItem(onClick = {
                        Navigator.launchSettingActivity(context)
                        // expanded = false
                    }) {
                        Image(painterResource(R.drawable.ic_user_profile_head), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.title_my_profile), Modifier.width(100.dp))
                    }
                    DropdownMenuItem(onClick = {
                        Navigator.launchMyProfileActivity(context)
                        // expanded = false
                    }) {
                        Image(painterResource(R.drawable.ic_user_profile_aboutus), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.title_user_info), Modifier.width(100.dp))
                    }
                }
            }
        }
    )
}

@Composable
private fun HomeRoomContent(
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
            onCategorySelected,
            Modifier.fillMaxWidth()
        )

        when (selectedHomeCategory) {
            RoomCategory.Current -> {
                HomeRoomList(Modifier.fillMaxSize(), roomList, RoomCategory.Current)
            }
            RoomCategory.History -> {
                HomeRoomList(Modifier.fillMaxSize(), roomHistory, RoomCategory.History)
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
        HomeTabIndicator(Modifier.tabIndicatorOffset(tabPositions[selectedIndex]), FlatColorBlue)
    }

    TabRow(
        selectedTabIndex = selectedIndex,
        modifier = modifier,
        indicator = indicator,
        backgroundColor = MaterialTheme.colors.surface) {
        categories.forEachIndexed { index, category ->
            val text = when (category) {
                RoomCategory.Current -> stringResource(R.string.home_room_list)
                RoomCategory.History -> stringResource(R.string.home_room_history)
            }

            Tab(
                selected = index == selectedIndex,
                onClick = { onCategorySelected(category) },
                text = {
                    Text(text = text, style = MaterialTheme.typography.body2)
                }
            )
        }
    }
}

@Composable
private fun HomeTabIndicator(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colors.onSurface,
) {
    Spacer(modifier
        .height(2.dp)
        .padding(horizontal = 5.dp)
        .background(color, RoundedCornerShape(topStartPercent = 100, topEndPercent = 100)))
}

@Composable
private fun HomeRoomList(modifier: Modifier, roomList: List<RoomInfo>, category: RoomCategory) {
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
        EmptyView(imgRes, message, modifier.verticalScroll(rememberScrollState()))
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
                Box(Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp), Alignment.TopCenter) {
                    Text(stringResource(R.string.loaded_all), style = FlatSmallTipTextStyle)
                }
            }
        }
    }
}

@Composable
private fun RoomListItem(roomInfo: RoomInfo, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography

    Column(modifier) {
        if (roomInfo.showDayHead) {
            Row(Modifier.padding(start = 16.dp, end = 16.dp, top = 12.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Image(painterResource(R.drawable.ic_home_calendar), contentDescription = "")
                Spacer(modifier = Modifier.size(4.dp))
                Text(FlatFormatter.dateMisc(roomInfo.beginTime), style = typography.body1)
            }
        }
        Row(Modifier.padding(16.dp, 12.dp)) {
            Column(Modifier.weight(1f)) {
                Text(roomInfo.title, style = typography.body1)
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "${FlatFormatter.time(roomInfo.beginTime)} ~ ${FlatFormatter.time(roomInfo.endTime)}",
                    style = typography.body2
                )
            }
            Spacer(Modifier.width(12.dp))
            FlatRoomStatusText(roomInfo.roomStatus, Modifier.align(Alignment.Bottom))
        }
        Spacer(
            Modifier
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

@Preview(showBackground = true, uiMode = 0x30)
@Composable
fun HomePreview() {
    val viewState = HomeViewState(
        networkActive = false
    )
    Home(viewState) {

    }
}