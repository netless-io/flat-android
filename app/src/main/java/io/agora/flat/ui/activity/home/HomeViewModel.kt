package io.agora.flat.ui.activity.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.di.impl.EventBus
import io.agora.flat.di.interfaces.NetworkObserver
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.ui.util.ObservableLoadingCounter
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
    private val eventBus: EventBus,
    private val appKVCenter: AppKVCenter,
    private val cloudStorageRepository: CloudStorageRepository,
    private val networkObserver: NetworkObserver,
) : ViewModel() {
    private val userInfo = MutableStateFlow(appKVCenter.getUserInfo() ?: UserInfo("", "", ""))
    private val roomCategory = MutableStateFlow(RoomCategory.Current)
    private val roomList = MutableStateFlow(listOf<RoomInfo>())
    private val historyList = MutableStateFlow(listOf<RoomInfo>())
    private val refreshing = ObservableLoadingCounter()
    private val networkActive = networkObserver.observeNetworkActive()

    private val _state = MutableStateFlow(HomeViewState())
    val state: StateFlow<HomeViewState>
        get() = _state

    private var page: Int = 1
    private var historyPage: Int = 1

    init {
        viewModelScope.launch {
            combine(
                roomCategory,
                roomList,
                historyList,
                refreshing.observable,
                userInfo,
            ) { selectedCategory, roomLists, roomHistoryList, refreshing, userInfo ->
                HomeViewState(
                    category = selectedCategory,
                    roomList = roomLists,
                    historyList = roomHistoryList,
                    refreshing = refreshing,
                    userInfo = userInfo,
                    networkActive = _state.value.networkActive,
                    errorMessage = null,
                )
            }.collect {
                _state.value = it
            }
        }

        viewModelScope.launch {
            networkActive.collect {
                _state.value = _state.value.copy(networkActive = it)
            }
        }

        viewModelScope.launch {
            eventBus.events.filterIsInstance<RoomsUpdated>().collect {
                reloadRoomList()
            }
        }

        reloadRoomList()
    }

    fun onRoomCategorySelected(category: RoomCategory) {
        roomCategory.value = category
    }

    fun reloadRoomList() {
        viewModelScope.launch {
            refreshing.addLoader()
            reloadRooms()
            reloadHistoryRooms()
            delay(2000)
            refreshing.removeLoader()
        }
    }

    private fun reloadRooms() {
        viewModelScope.launch {
            when (val response = roomRepository.getRoomListAll(page)) {
                is Success -> {
                    val list = ArrayList(response.data)
                    roomList.value = addShowDayHeadFlag(list)
                }
                is Failure -> {
                    when (response.error.status) {
                        // handle error
                    }
                }
            }
        }
    }

    fun loadMoreRooms() {

    }

    fun loadMoreHistoryRooms() {

    }

    private fun reloadHistoryRooms() {
        viewModelScope.launch {
            when (val response = roomRepository.getRoomListHistory(historyPage)) {
                is Success -> {
                    val list = ArrayList(response.data)
                    historyList.value = addShowDayHeadFlag(list)
                }
                is Failure -> {
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
        return list
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
    val category: RoomCategory = RoomCategory.Current,
    val roomList: List<RoomInfo> = listOf(),
    val historyList: List<RoomInfo> = listOf(),
    val userInfo: UserInfo = UserInfo("", "", ""),
    val networkActive: Boolean = true,
    val errorMessage: String? = null,
)

sealed class HomeViewAction {
    object Reload : HomeViewAction()
    data class SelectCategory(val category: RoomCategory) : HomeViewAction()
    object SetNetwork : HomeViewAction()
}