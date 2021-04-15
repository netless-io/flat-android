package io.agora.flat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor() : ViewModel() {
    val content: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun uploadFeedback(text: String) {
        viewModelScope.launch {
            delay(2000)
        }
    }
}