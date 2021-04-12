package link.netless.flat.ui.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import link.netless.flat.R
import link.netless.flat.data.model.RoomStatus
import link.netless.flat.ui.activity.ui.theme.FlatColorBlue
import link.netless.flat.ui.activity.ui.theme.FlatColorRed
import link.netless.flat.ui.activity.ui.theme.FlatColorTextSecondary


@Composable
fun FlatRoomStatusText(roomStatus: RoomStatus, modifier: Modifier) {
    val typography = MaterialTheme.typography

    when (roomStatus) {
        RoomStatus.Idle ->
            Text(
                modifier = modifier,
                text = stringResource(R.string.home_room_state_idle),
                style = typography.body2,
                color = FlatColorRed
            )
        RoomStatus.Started, RoomStatus.Paused ->
            Text(
                modifier = modifier,
                text = stringResource(R.string.home_room_state_started),
                style = typography.body2,
                color = FlatColorBlue
            )
        RoomStatus.Stopped ->
            Text(
                modifier = modifier,
                text = stringResource(R.string.home_room_state_end),
                style = typography.body2,
                color = FlatColorTextSecondary
            )
    }
}