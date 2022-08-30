package io.agora.flat.ui.activity.home

import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.UserInfo

data class HomeUiState(
    val refreshing: Boolean = false,
    val roomList: List<RoomInfo> = listOf(),
    val userInfo: UserInfo = UserInfo("", "", ""),
    val networkActive: Boolean = true,
    val errorMessage: String? = null,
)