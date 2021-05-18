package io.agora.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.util.FlatFormatter
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val appKVCenter: AppKVCenter
) : ViewModel() {
    private val userInfo = MutableStateFlow(appKVCenter.getUserInfo() ?: UserInfo("", "", ""))
    private val selectedCategory = MutableStateFlow(RoomCategory.Current)
    private val roomList = MutableStateFlow(listOf<RoomInfo>())
    private val roomHistoryList = MutableStateFlow(listOf<RoomInfo>())
    private val refreshing = MutableStateFlow(false)

    private val _state = MutableStateFlow(HomeViewState())

    val state: StateFlow<HomeViewState>
        get() = _state

    // TODO
    private var page: Int = 1;
    private var historyPage: Int = 1;

    init {
        viewModelScope.launch {
            combine(
                selectedCategory,
                roomList,
                roomHistoryList,
                refreshing,
                userInfo,
            ) { selectedCategory, roomLists, roomHistoryList, refreshing, userInfo ->
                HomeViewState(
                    selectedHomeCategory = selectedCategory,
                    roomList = roomLists,
                    roomHistoryList = roomHistoryList,
                    refreshing = refreshing,
                    userInfo = userInfo,
                    errorMessage = null /* TODO */
                )
            }.catch { throwable ->
                throw throwable
            }.collect {
                _state.value = it
            }
        }

        reloadRoomList()
    }

    fun onRoomCategorySelected(category: RoomCategory) {
        selectedCategory.value = category
    }

    fun reloadRoomList() {
        viewModelScope.launch {
            refreshing.value = true
            // TODO
            loadRooms()
            loadHistoryRooms()
            delay(2000)
            refreshing.value = false
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            when (val response = roomRepository.getRoomListAll(page)) {
                is Success -> {
                    // val list = ArrayList(response.data) + roomList.value
                    val list = ArrayList(response.data)
                    addShowDayHeadFlag(list)
                    roomList.value = list
                }
                is ErrorResult -> {
                    when (response.error.status) {
                        // handle error
                    }
                }
            }
        }
    }

    fun loadHistoryRooms() {
        viewModelScope.launch {
            when (val response = roomRepository.getRoomListHistory(historyPage)) {
                is Success -> {
                    // val list = ArrayList(response.data) + roomHistoryList.value
                    val list = ArrayList(response.data)
                    addShowDayHeadFlag(list)
                    roomHistoryList.value = list
                }
                is ErrorResult -> {
                    when (response.error.status) {
                        // handle error
                    }
                }
            }
        }
    }

    /**
     * 添加节点
     */
    private fun addShowDayHeadFlag(list: List<RoomInfo>): List<RoomInfo> {
        var lastDay = ""
        list.forEach { roomInfo ->
            val formatToMMDD = FlatFormatter.dateDash(roomInfo.beginTime)
            if (formatToMMDD != lastDay) {
                roomInfo.showDayHead = true
                lastDay = formatToMMDD
            } else {
                roomInfo.showDayHead = false
            }
        }
        return list;
    }
}

enum class RoomCategory {
    // 房间列表
    Current,

    // 历史房间
    History
}

data class HomeViewState(
    val refreshing: Boolean = false,
    val selectedHomeCategory: RoomCategory = RoomCategory.Current,
    val roomList: List<RoomInfo> = listOf(),
    val roomHistoryList: List<RoomInfo> = listOf(),
    val userInfo: UserInfo = UserInfo("", "", ""),
    val errorMessage: String? = null
)