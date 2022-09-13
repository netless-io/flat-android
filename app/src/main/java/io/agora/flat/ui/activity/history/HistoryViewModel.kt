package io.agora.flat.ui.activity.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.data.Failure
import io.agora.flat.data.Success
import io.agora.flat.data.model.RoomInfo
import io.agora.flat.data.repository.RoomRepository
import io.agora.flat.event.EventBus
import io.agora.flat.event.RoomsUpdated
import io.agora.flat.ui.util.ObservableLoadingCounter
import io.agora.flat.util.runAtLeast
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Here Use StateFlow To Manage State
 */
@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val roomRepository: RoomRepository,
    private val eventBus: EventBus,
) : ViewModel() {
    private val histories = MutableStateFlow(listOf<RoomInfo>())
    private val refreshing = ObservableLoadingCounter()

    val state: StateFlow<HistoryUiState> = combine(
        histories,
        refreshing.observable
    ) { histories, refreshing ->
        HistoryUiState(histories, refreshing)
    }.stateIn(
        viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HistoryUiState(),
    )

    private var historyPage: Int = 1

    init {
        viewModelScope.launch {
            eventBus.events.filterIsInstance<RoomsUpdated>().collect {
                reloadHistories()
            }
        }

        reloadHistories()
    }

    fun reloadHistories() {
        viewModelScope.launch {
            refreshing.addLoader()
            runAtLeast {
                when (val result = roomRepository.getRoomListHistory(historyPage)) {
                    is Success -> {
                        histories.value = ArrayList(result.data)
                    }
                    is Failure -> when (result.exception) {
                        // handle error
                    }
                }
            }
            refreshing.removeLoader()
        }
    }
}

