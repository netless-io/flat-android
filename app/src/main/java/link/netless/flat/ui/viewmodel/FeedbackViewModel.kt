package link.netless.flat.ui.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import link.netless.flat.data.model.UserInfo
import link.netless.flat.data.repository.UserRepository
import link.netless.flat.util.Resource
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(): ViewModel() {
    val content: MutableLiveData<String> by lazy {
        MutableLiveData<String>()
    }

    fun uploadFeedback(text: String) {
        viewModelScope.launch {
            delay(2000)
        }
    }
}