package io.agora.flat.ui.manager

import dagger.hilt.android.scopes.ActivityRetainedScoped
import io.agora.flat.data.Success
import io.agora.flat.data.model.*
import io.agora.flat.data.repository.CloudRecordRepository
import io.agora.flat.util.Ticker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@ActivityRetainedScoped
class RecordManager @Inject constructor(
    private val cloudRecordRepository: CloudRecordRepository,
    private val userManager: UserManager,
) {
    lateinit var viewModelScope: CoroutineScope
    lateinit var roomUUID: String

    private var timer: Job? = null

    private var recordState = MutableStateFlow<RecordState?>(null)
    private var videoUsers = MutableStateFlow<List<RoomUser>>(emptyList())

    fun reset(roomUUID: String, scope: CoroutineScope) {
        this.roomUUID = roomUUID
        viewModelScope = scope

        viewModelScope.launch {
            userManager.observeUsers().collect { users ->
                // TODO here ensure isSpeak or onStage contains owners
                val sortedList = users
                    .filter { value -> value.isOnStage && !value.isOwner }
                    .sortedBy { user -> user.rtcUID }
                val owner = users.filter { it.isOwner }
                videoUsers.value = owner + sortedList;
            }
        }
    }

    fun observeRecordState(): Flow<RecordState?> {
        return recordState.asStateFlow()
    }

    suspend fun startRecord() {
        val acquireResp = cloudRecordRepository.acquireRecord(roomUUID)
        if (acquireResp is Success) {
            val startResp = cloudRecordRepository.startRecordWithAgora(
                roomUUID,
                acquireResp.data.resourceId,
                transcodingConfig()
            )
            if (startResp is Success) {
                recordState.value = RecordState(
                    resourceId = startResp.data.resourceId,
                    sid = startResp.data.sid
                )
                startTimer()
            }
        }
    }

    suspend fun stopRecord() {
        recordState.value?.run {
            val resp = cloudRecordRepository.stopRecordWithAgora(roomUUID, resourceId, sid)
            if (resp is Success) {
                recordState.value = null
            }
            stopTimer()
        }
    }

    private fun startTimer() {
        timer?.cancel()
        timer = viewModelScope.launch {
            Ticker.tickerFlow(1000, 1000).collect {
                val state = recordState.value ?: return@collect
                recordState.value = state.copy(recordTime = state.recordTime + 1)
                updateRecordLayout()
            }
        }
    }

    private fun stopTimer() {
        timer?.cancel()
    }

    private fun updateRecordLayout() {
        viewModelScope.launch {
            val config = UpdateLayoutClientRequest(
                layoutConfig = getLayoutConfig(),
                backgroundConfig = getBackgroundConfig(),
            )
            cloudRecordRepository.updateRecordLayoutWithAgora(
                roomUUID,
                recordState.value!!.resourceId,
                recordState.value!!.sid,
                config
            )
        }
    }

    private val width = 144 * 17
    private val height = 108

    private fun transcodingConfig() = TranscodingConfig(
        width,
        height,
        15,
        500,
        mixedVideoLayout = 3,
        layoutConfig = getLayoutConfig(),
        backgroundConfig = getBackgroundConfig()
    )

    private fun getBackgroundConfig(): List<BackgroundConfig> {
        return videoUsers.value.map { user: RoomUser ->
            BackgroundConfig(uid = user.rtcUID.toString(), image_url = user.avatarURL)
        }
    }

    private fun getLayoutConfig(): List<LayoutConfig> {
        return videoUsers.value.mapIndexed { index: Int, user: RoomUser ->
            LayoutConfig(
                uid = user.rtcUID.toString(),
                x_axis = index * 144 / width.toFloat(),
                y_axis = 0f,
                width = 144 / width.toFloat(),
                height = 108 / height.toFloat(),
            )
        }
    }
}

data class RecordState constructor(
    val resourceId: String,
    val sid: String,
    val recordTime: Long = 0,
)