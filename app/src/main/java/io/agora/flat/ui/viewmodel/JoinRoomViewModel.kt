package io.agora.flat.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.agora.flat.common.ClipboardController
import io.agora.flat.data.AppDatabase
import io.agora.flat.data.model.RoomConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class JoinRoomViewModel @Inject constructor(
    private val appDatabase: AppDatabase,
    private val clipboard: ClipboardController,
) : ViewModel() {
    companion object {
        const val ROOM_UUID_PATTERN = """[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}"""
    }

    val roomUUID = MutableStateFlow("")

    fun updateRoomConfig(roomUUID: String, openVideo: Boolean, openAudio: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            appDatabase.roomConfigDao().insertOrUpdate(RoomConfig(roomUUID, openVideo, openAudio))
        }
    }

    fun checkClipboardText() {
        val ct = currentClipboardText()
        if (ct.isNotBlank()) {
            roomUUID.value = ct
        }
    }

    private fun currentClipboardText(): String {
        if (clipboard.getText().isNotBlank()) {
            val regex = ROOM_UUID_PATTERN.toRegex()
            val entire = regex.find(clipboard.getText())
            if (entire != null) {
                return entire.value
            }
        }
        return ""
    }
}

internal sealed class JoinRoomAction {
    object Close : JoinRoomAction()
    data class JoinRoom(val roomUUID: String, val openVideo: Boolean, val openAudio: Boolean) : JoinRoomAction()
}