package io.agora.flat.ui.compose

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.SwipeRefreshIndicator
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState

@Composable
fun FlatSwipeRefresh(refreshing: Boolean, onRefresh: () -> Unit, content: @Composable () -> Unit) {
    SwipeRefresh(
        state = rememberSwipeRefreshState(refreshing),
        onRefresh = onRefresh,
        indicator = { state, trigger ->
            SwipeRefreshIndicator(
                state = state,
                refreshTriggerDistance = trigger,
                contentColor = MaterialTheme.colors.primary,
            )
        },
        content = content
    )
}
