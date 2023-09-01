package io.agora.flat.ui.activity.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import io.agora.flat.R
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.ui.compose.EmptyView
import io.agora.flat.ui.compose.*
import io.agora.flat.ui.theme.isDarkTheme

@Composable
fun HistoryScreen(
    onBackPressed: () -> Unit,
    onOpenRoomDetail: (rUUID: String, pUUID: String?) -> Unit,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val viewState by viewModel.state.collectAsState()

    HistoryScreen(
        viewState = viewState,
        onBackPressed = onBackPressed,
        onRefresh = viewModel::reloadHistories,
        onOpenRoomDetail = onOpenRoomDetail
    )
}

@Composable
private fun HistoryScreen(
    viewState: HistoryUiState,
    onBackPressed: () -> Unit,
    onRefresh: () -> Unit,
    onOpenRoomDetail: (String, String?) -> Unit
) {
    Column {
        BackTopAppBar(stringResource(R.string.home_room_history), onBackPressed = onBackPressed)
        FlatSwipeRefresh(
            refreshing = viewState.refreshing,
            onRefresh = onRefresh,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            HistoryList(
                modifier = Modifier.fillMaxSize(),
                histories = viewState.histories,
                onOpenRoomDetail = onOpenRoomDetail,
            )
        }
    }
}

@Composable
private fun HistoryList(modifier: Modifier, histories: List<RoomInfo>, onOpenRoomDetail: (String, String?) -> Unit) {
    val imgRes = if (isDarkTheme()) R.drawable.img_history_list_empty_dark else R.drawable.img_history_list_empty_light

    if (histories.isEmpty()) {
        EmptyView(
            modifier = modifier.verticalScroll(rememberScrollState()),
            imgRes = imgRes,
            message = R.string.home_no_history_room_tip
        )
    } else {
        LazyColumn(modifier) {
            items(
                count = histories.size,
                key = { index: Int ->
                    histories[index].roomUUID
                },
            ) {
                RoomItem(
                    histories[it],
                    Modifier.clickable(
                        onClick = {
                            onOpenRoomDetail(
                                histories[it].roomUUID,
                                histories[it].periodicUUID
                            )
                        },
                    )
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
@Preview(widthDp = 400, heightDp = 800, uiMode = 0x10)
@Preview(widthDp = 400, heightDp = 800, uiMode = 0x20)
private fun PreviewHistoryScreen() {
    FlatPage {
        HistoryScreen(
            HistoryUiState(histories = listOf(), false, false),
            {}, {}, { rUUID, pUUID -> }
        )
    }
}

