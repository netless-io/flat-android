package io.agora.flat.ui.activity.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.model.UserInfo
import io.agora.flat.data.repository.CloudStorageRepository
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.di.interfaces.EventBus
import io.agora.flat.event.HomeRefreshEvent
import io.agora.flat.util.FlatFormatter
import io.agora.flat.util.ObservableLoadingCounter
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
) : ViewModel() {
    companion object {
        val TAG = HomeViewModel.javaClass.simpleName;
    }

    private val userInfo = MutableStateFlow(appKVCenter.getUserInfo() ?: UserInfo("", "", ""))
    private val selectedCategory = MutableStateFlow(RoomCategory.Current)
    private val roomList = MutableStateFlow(listOf<RoomInfo>())
    private val roomHistoryList = MutableStateFlow(listOf<RoomInfo>())
    private val refreshing = ObservableLoadingCounter()

    private val _state = MutableStateFlow(HomeViewState())

    val state: StateFlow<HomeViewState>
        get() = _state

    private var page: Int = 1;
    private var historyPage: Int = 1;

    init {
        viewModelScope.launch {
            combine(
                selectedCategory,
                roomList,
                roomHistoryList,
                refreshing.observable,
                userInfo,
            ) { selectedCategory, roomLists, roomHistoryList, refreshing, userInfo ->
                HomeViewState(
                    selectedHomeCategory = selectedCategory,
                    roomList = roomLists,
                    roomHistoryList = roomHistoryList,
                    refreshing = refreshing,
                    userInfo = userInfo,
                    errorMessage = null,
                )
            }.collect {
                _state.value = it
            }
        }

        viewModelScope.launch {
            eventBus.events.filter { it is HomeRefreshEvent }.collect {
                reloadRoomList()
            }
        }

        reloadRoomList()
    }

    fun onRoomCategorySelected(category: RoomCategory) {
        selectedCategory.value = category
    }

    fun reloadRoomList() {
        Log.d(TAG, "reload room list start")
        viewModelScope.launch {
            refreshing.addLoader()
            reloadRooms()
            reloadHistoryRooms()
            delay(2000)
            refreshing.removeLoader()
        }

        viewModelScope.launch {
            cloudStorageRepository.getFileList(1)
        }
    }

    private fun reloadRooms() {
        viewModelScope.launch {
            when (val response = roomRepository.getRoomListAll(page)) {
                is Success -> {
                    val list = ArrayList(response.data)
                    roomList.value = addShowDayHeadFlag(list)
                }
                is ErrorResult -> {
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
                    roomHistoryList.value = addShowDayHeadFlag(list)
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
    val errorMessage: String? = null,
)

sealed class HomeViewAction {
    object Reload : HomeViewAction()
    data class SelectCategory(val category: RoomCategory) : HomeViewAction()
}