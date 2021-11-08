package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.android.LanguageManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class LanguageViewModel @Inject constructor() : ViewModel() {
    private val _state = MutableStateFlow(LanguageState())
    val state = _state.asStateFlow()

    init {
        val items = listOf(
            LanguageManager.Item.Default,
            LanguageManager.Item.Chinese,
            LanguageManager.Item.English,
        )
        val language = LanguageManager.current()
        val index = items.indexOfFirst { it.language == language }

        _state.value = LanguageState(index, items)
    }

    fun selectIndex(index: Int) {
        _state.value = _state.value.copy(index = index)
    }

    fun save() {
        val index = state.value.index
        val items = state.value.items
        LanguageManager.update(items[index].language)
    }
}

data class LanguageState(
    // current selected
    val index: Int = 0,
    // all support and display language items
    val items: List<LanguageManager.Item> = listOf(),
)