package link.netless.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
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

    private var page: Int = 1;

    fun reloadRoomList() {
        viewModelScope.launch {
            refreshing.value = true
            loadRooms()
            loadHistoryRooms()
            delay(2000)
            refreshing.value = false
        }
    }

    fun loadRooms() {
        viewModelScope.launch {
            roomRepository.getRoomListAll(page).catch {
                // showError
            }.collect {
                // TODO Kotlin
                launch(Dispatchers.Default) {
                    addShowDayHeadFlag(it + roomList.value)
                    roomList.value = it
                }
            }
        }
    }

    fun loadHistoryRooms() {
        viewModelScope.launch {
            roomRepository.getRoomListHistory(page).catch {
                // showError
            }.collect {
                launch(Dispatchers.Default) {
                    addShowDayHeadFlag(it + roomHistoryList.value)
                    roomHistoryList.value = it
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