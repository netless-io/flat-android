package io.agora.flat.ui.activity.base

import androidx.lifecycle.viewModelScope
import io.agora.flat.util.Ticker
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

abstract class BaseAccountViewModel : BaseViewModel() {
    protected val remainTime = MutableStateFlow(0L)
    private var countDownJob: Job? = null

    protected fun startCountDown() {
        countDownJob?.cancel()
        countDownJob = viewModelScope.launch {
            Ticker.countDownFlow(60).collect {
                remainTime.value = it
            }
        }
    }
}