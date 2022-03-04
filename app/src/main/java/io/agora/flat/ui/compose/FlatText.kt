package io.agora.flat.ui.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import io.agora.flat.R
import io.agora.flat.data.model.RoomStatus
import io.agora.flat.ui.theme.*


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

@Composable
fun FlatTextLargeTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.h5,
    )
}

@Composable
fun FlatTextTitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.h6,
    )
}

@Composable
fun FlatTextSubtitle(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = MaterialTheme.typography.subtitle1,
    )
}

@Composable
fun FlatTextBodyOne(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = MaterialTheme.typography.body1,
    )
}

@Composable
fun FlatTextBodyOneSecondary(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = if (isDarkTheme()) FlatColorTextSecondaryDark else FlatColorTextSecondary,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = MaterialTheme.typography.body1,
    )
}


@Composable
fun FlatTextBodyTwo(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = MaterialTheme.typography.body2,
    )
}

@Composable
fun FlatTextButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = MaterialTheme.typography.button,
    )
}

@Composable
fun FlatTextCaption(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = if (isDarkTheme()) FlatColorTextSecondaryDark else FlatColorTextSecondary,
    overflow: TextOverflow = TextOverflow.Clip,
    softWrap: Boolean = true,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = text,
        modifier = modifier,
        color = color,
        overflow = overflow,
        softWrap = softWrap,
        maxLines = maxLines,
        style = MaterialTheme.typography.caption,
    )
}