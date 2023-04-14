package io.agora.flat.ui.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object RoomOverlayManager {
    const val AREA_ID_NO_OVERLAY = 0
    const val AREA_ID_FASTBOARD = 1

    // const val AREA_ID_PAINT = 2
    const val AREA_ID_SETTING = 3
    const val AREA_ID_MESSAGE = 4
    const val AREA_ID_CLOUD_STORAGE = 5
    const val AREA_ID_VIDEO_OP_CALL_OUT = 6
    const val AREA_ID_INVITE_DIALOG = 7
    const val AREA_ID_OWNER_EXIT_DIALOG = 8
    const val AREA_ID_USER_LIST = 9
    const val AREA_ID_ACCEPT_HANDUP = 10

    const val AREA_ID_APPS = 11

    private var showId = MutableStateFlow(AREA_ID_NO_OVERLAY)

    fun observeShowId(): Flow<Int> {
        return showId.asStateFlow()
    }

    fun getShowId(): Int {
        return showId.value
    }

    fun setShown(id: Int, shown: Boolean = true) {
        if (shown) {
            showId.value = id
        } else {
            showId.value = AREA_ID_NO_OVERLAY
        }
    }
}