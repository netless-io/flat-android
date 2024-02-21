package io.agora.flat.ui.compose

import android.content.Context
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ProvideTextStyle
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.agora.flat.R
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.ui.theme.FlatTheme
import io.agora.flat.ui.theme.Gray_0
import io.agora.flat.ui.theme.Green_6
import io.agora.flat.ui.theme.Shapes
import io.agora.flat.ui.theme.isDarkTheme
import io.agora.flat.util.DateUtils
import io.agora.flat.util.FlatFormatter
import java.util.Calendar
import kotlin.math.ceil

@Composable
fun RoomItem(
    roomInfo: RoomInfo,
    modifier: Modifier = Modifier,
    onItemClick: () -> Unit = {},
    onStartClick: () -> Unit = {}
) {
    Row(
        modifier
            .clickable(onClick = onItemClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlatAvatar(avatar = roomInfo.ownerAvatarURL, size = 32.dp)
        Spacer(Modifier.width(16.dp))
        Box {
            Row {
                Column(Modifier.weight(1f)) {
                    Spacer(Modifier.height(12.dp))
                    RoomItemTitle(roomInfo)
                    Spacer(Modifier.height(8.dp))
                    RoomItemTime(roomInfo.beginTime, roomInfo.endTime)
                    Spacer(Modifier.height(12.dp))
                }
                Spacer(Modifier.width(12.dp))
                RoomItemRightBox(
                    roomInfo,
                    modifier = Modifier.align(Alignment.CenterVertically),
                    onStartClick = onStartClick
                )
            }
            FlatDivider(modifier = Modifier.align(Alignment.BottomCenter))
        }
    }
}

@Composable
private fun RoomItemRightBox(roomInfo: RoomInfo, modifier: Modifier, onStartClick: () -> Unit = {}) {
    val appKVCenter = LocalAppKVCenter.current ?: AppKVCenter(LocalContext.current)
    val joinEarly = appKVCenter.getJoinEarly()
    val text = stringResource(R.string.enter)

    Box(modifier) {
        val now = Calendar.getInstance().timeInMillis
        val c1 = now + joinEarly * 60 * 1000
        val c2 = now + 60 * 60 * 1000

        when {
            roomInfo.roomStatus == RoomStatus.Stopped -> {
                FlatRoomStatusText(roomInfo.roomStatus)
            }

            roomInfo.beginTime in 0 until c1 -> {
                RoomItemJoinButton(text = text, onClick = onStartClick)
            }

            roomInfo.beginTime in c1 until c2 -> {
                Text(
                    text = stringResource(
                        id = R.string.home_room_start_at_min,
                        ceil((roomInfo.beginTime - now) / 1000 / 60.0).toInt()
                    ),
                    style = MaterialTheme.typography.body2,
                    color = Green_6
                )
            }

            else -> {
                FlatRoomStatusText(roomInfo.roomStatus)
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun RoomItemJoinButton(text: String, onClick: () -> Unit) {
    val darkMode = isDarkTheme()
    val contentColor = if (darkMode) Gray_0 else Gray_0

    Surface(
        onClick = onClick,
        shape = Shapes.small,
        color = MaterialTheme.colors.primary,
        contentColor = contentColor.copy(alpha = 1f),
        interactionSource = remember { MutableInteractionSource() },
    ) {
        CompositionLocalProvider(LocalContentAlpha provides contentColor.alpha) {
            ProvideTextStyle(
                value = MaterialTheme.typography.button
            ) {
                Row(
                    Modifier
                        .defaultMinSize(minWidth = 60.dp, minHeight = 28.dp)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.body2,
                    )
                }
            }
        }
    }
}

@Composable
private fun RoomItemTitle(roomInfo: RoomInfo) {
    var width by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Row(
        Modifier
            .fillMaxWidth()
            .onGloballyPositioned {
                width = with(density) {
                    it.size.width.toDp()
                }
            }) {
        FlatTextSubtitle(
            roomInfo.title,
            modifier = Modifier.sizeIn(maxWidth = width - 28.dp),
            color = FlatTheme.colors.textTitle
        )
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
fun RoomItemPreview() {
    val now = Calendar.getInstance().timeInMillis
    val roomInfo = ComposePreviewData.roomInfo
    FlatColumnPage {
        RoomItem(roomInfo.copy(beginTime = now + 4 * 60 * 1000))
        RoomItem(roomInfo.copy(beginTime = now + 5 * 60 * 1000 + 2 * 1000))
        RoomItem(roomInfo.copy(beginTime = now + 10 * 60 * 1000))
        RoomItem(roomInfo.copy(beginTime = now + 100 * 60 * 1000))
        RoomItem(roomInfo.copy(beginTime = now + 100 * 60 * 1000, title = "Long Long Long Long Long Long Long Title"))
        RoomItem(
            roomInfo.copy(
                beginTime = now + 10 * 60 * 1000,
                periodicUUID = "123",
                title = "Long Long Long Long Long Long Long Title"
            )
        )
    }
}