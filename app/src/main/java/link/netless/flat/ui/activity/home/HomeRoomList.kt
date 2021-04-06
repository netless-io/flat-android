package link.netless.flat.ui.activity.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.gson.Gson
import link.netless.flat.R
import link.netless.flat.data.model.RoomInfo
import link.netless.flat.data.model.RoomStatus
import link.netless.flat.ui.activity.ui.theme.FlatColorBlue
import link.netless.flat.ui.activity.ui.theme.FlatColorDivider
import link.netless.flat.ui.activity.ui.theme.FlatColorRed
import link.netless.flat.ui.activity.ui.theme.FlatColorTextSecondary
import link.netless.flat.util.formatToHHmm
import link.netless.flat.util.formatToMMDDWeek

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

        when (selectedHomeCategory) {
            RoomCategory.Current -> {
                CurrentRoomList(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f), roomList
                )
            }
            RoomCategory.History -> {
                CurrentRoomList(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f), roomHistory
                )
            }
        }
    }
}

@Composable
private fun HomeRoomTabs(
    categories: List<RoomCategory>,
    selectedCategory: RoomCategory,
    onCategorySelected: (RoomCategory) -> Unit,
    modifier: Modifier = Modifier
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
    color: Color = MaterialTheme.colors.onSurface
) {
    Spacer(
        modifier
            .height(2.dp)
            .background(color, RoundedCornerShape(topStartPercent = 100, topEndPercent = 100))
    )
}

@Composable
fun CurrentRoomList(modifier: Modifier, roomList: List<RoomInfo>) {
    LazyColumn {
        items(count = roomList.size, key = { index: Int ->
            roomList[index].roomUUID
        }) {
            RoomListItem(roomList[it], Modifier)
        }
    }
}

@Composable
fun RoomListItem(roomInfo: RoomInfo, modifier: Modifier = Modifier) {
    val typography = MaterialTheme.typography

    Column() {
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
                    text = roomInfo.beginTime.formatToMMDDWeek(),
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
                text = "${roomInfo.beginTime.formatToHHmm()} ~ ${roomInfo.endTime.formatToHHmm()}",
                style = typography.body2
            )
            when (roomInfo.roomStatus) {
                RoomStatus.Idle ->
                    Text(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        text = stringResource(R.string.home_room_state_idle),
                        style = typography.body2,
                        color = FlatColorRed
                    )
                RoomStatus.Started, RoomStatus.Paused ->
                    Text(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        text = stringResource(R.string.home_room_state_started),
                        style = typography.body2,
                        color = FlatColorBlue
                    )
                RoomStatus.Stopped ->
                    Text(
                        modifier = Modifier.align(Alignment.BottomEnd),
                        text = stringResource(R.string.home_room_state_end),
                        style = typography.body2,
                        color = FlatColorTextSecondary
                    )
            }
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