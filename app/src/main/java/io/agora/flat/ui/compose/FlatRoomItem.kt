package io.agora.flat.ui.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.util.DateUtils
import io.agora.flat.util.FlatFormatter

@Composable
fun RoomItem(roomInfo: RoomInfo, modifier: Modifier = Modifier) {
    Row(modifier.padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
        FlatAvatar(avatar = roomInfo.ownerAvatarURL, size = 32.dp)
        Spacer(Modifier.width(16.dp))
        Box(Modifier.weight(1f)) {
            Row {
                Column(Modifier.weight(1f)) {
                    Spacer(Modifier.height(12.dp))
                    RoomItemTitle(roomInfo)
                    Spacer(Modifier.height(8.dp))
                    Row {
                        FlatRoomStatusText(roomInfo.roomStatus)
                        Spacer(Modifier.width(4.dp))
                        RoomItemTime(roomInfo.beginTime, roomInfo.endTime)
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.width(16.dp))
                Image(
                    painterResource(R.drawable.ic_arrow_right),
                    contentDescription = null,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            FlatDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun RoomItemTitle(roomInfo: RoomInfo) {
    Row {
        FlatTextSubtitle(roomInfo.title, color = FlatTheme.colors.textTitle)
        if (roomInfo.roomStatus != RoomStatus.Stopped && roomInfo.isPeriodic) {
            Spacer(Modifier.width(4.dp))
            Image(
                painterResource(R.drawable.ic_home_calendar),
                null,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
            )
        }
        if (roomInfo.roomStatus == RoomStatus.Stopped && roomInfo.hasRecord) {
            Spacer(Modifier.width(4.dp))
            Image(
                painterResource(R.drawable.ic_has_record),
                null,
                colorFilter = ColorFilter.tint(MaterialTheme.colors.primary)
            )
        }
    }
}

@Composable
private fun RoomItemTime(beginTime: Long, endTime: Long) {
    val context = LocalContext.current
    FlatTextBodyTwo(
        getDisplayTime(context, beginTime, endTime),
        color = FlatTheme.colors.textPrimary
    )
}

private fun getDisplayTime(context: Context, beginTime: Long, endTime: Long): String {
    val duringTime = FlatFormatter.timeDuring(beginTime, endTime)
    return when {
        DateUtils.isToday(beginTime) -> context.getString(R.string.home_room_info_today_format, duringTime)
        DateUtils.isTomorrow(beginTime) -> context.getString(R.string.home_room_info_tomorrow_format, duringTime)
        DateUtils.isYesterday(beginTime) -> context.getString(R.string.home_room_info_yesterday_format, duringTime)
        DateUtils.isThisYear(beginTime) -> "${FlatFormatter.date(beginTime)} $duringTime"
        else -> "${FlatFormatter.longDate(beginTime)} $duringTime"
    }
}

@Composable
@Preview(widthDp = 400, uiMode = 0x10, locale = "zh")
@Preview(widthDp = 400, uiMode = 0x20)
fun RoomItemPreview() {
    var roomInfo = ComposePreviewData.roomInfo
    roomInfo = roomInfo.copy(roomStatus = RoomStatus.Started)
    FlatPage {
        RoomItem(roomInfo, Modifier)
    }
}