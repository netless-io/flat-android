package link.netless.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import link.netless.flat.data.ErrorResult
import link.netless.flat.data.Success
import link.netless.flat.data.model.RoomInfo
import link.netless.flat.data.repository.RoomRepository
import link.netless.flat.util.formatToMMDD
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class HomeViewModel @Inject constructor(private val roomRepository: RoomRepository) : ViewModel() {
    private val selectedCategory = MutableStateFlow(RoomCategory.Current)
    private val roomList = MutableStateFlow(listOf<RoomInfo>())
    private val roomHistoryList = MutableStateFlow(listOf<RoomInfo>())
    private val refreshing = MutableStateFlow(false)

    private val _state = MutableStateFlow(HomeViewState())

    val state: StateFlow<HomeViewState>
        get() = _state

    init {
        viewModelScope.launch {
            combine(
                selectedCategory,
                roomList,
                roomHistoryList,
                refreshing,
            ) { selectedCategory, roomLists, roomHistoryList, refreshing ->
                HomeViewState(
                    selectedHomeCategory = selectedCategory,
                    roomList = roomLists,
                    roomHistoryList = roomHistoryList,
                    refreshing = refreshing,
                    errorMessage = null /* TODO */
                )
            }.catch { throwable ->
                throw throwable
            }.collect {
                _state.value = it
            }
        }
    }

    fun onRoomCategorySelected(category: RoomCategory) {
        selectedCategory.value = category
    }

    // TODO
    private var page: Int = 1;
    private var historyPage: Int = 1;

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
            val formatToMMDD = roomInfo.beginTime.formatToMMDD()
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
    val errorMessage: String? = null
)