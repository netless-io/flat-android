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
    private val width = 120 * 3
    private val height = 360 * 3

    lateinit var viewModelScope: CoroutineScope
    lateinit var roomUUID: String

    private var timer: Job? = null
    private var startRecordJob: Job? = null

    private var recordState = MutableStateFlow<RecordState?>(null)
    private var videoUsers = MutableStateFlow<List<RoomUser>>(emptyList())

    fun reset(roomUUID: String, scope: CoroutineScope) {
        this.roomUUID = roomUUID
        viewModelScope = scope

        viewModelScope.launch {
            userManager.observeUsers().collect {
                // TODO here ensure isSpeak or onStage contains owners
                videoUsers.value = it.filter { value -> value.isSpeak }
            }
        }
    }

    fun observeRecordState(): Flow<RecordState?> {
        return recordState.asStateFlow()
    }

    fun startRecord() {
        if (videoUsers.value.isEmpty()) {
            startRecordJob = viewModelScope.launch {
                videoUsers.collect {
                    if (videoUsers.value.isNotEmpty()) {
                        startRecord()
                    }
                }
            }
            return
        }
        startRecordJob?.cancel()

        viewModelScope.launch {
            val acquireResp = cloudRecordRepository.acquireRecord(roomUUID)
            if (acquireResp is Success) {
                val transcodingConfig = TranscodingConfig(
                    width,
                    height,
                    15,
                    500,
                    mixedVideoLayout = 3,
                    layoutConfig = getLayoutConfig(),
                    backgroundConfig = getBackgroundConfig()
                )
                val startResp = cloudRecordRepository.startRecordWithAgora(
                    roomUUID,
                    acquireResp.data.resourceId,
                    transcodingConfig
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
    }

    fun stopRecord() {
        viewModelScope.launch {
            recordState.value?.run {
                val resp = cloudRecordRepository.stopRecordWithAgora(
                    roomUUID,
                    resourceId,
                    sid,
                )
                if (resp.isSuccess) {
                    recordState.value = null
                }
                stopTimer()
            }
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
            cloudRecordRepository.updateRecordLayoutWithAgora(roomUUID, recordState.value!!.resourceId, config)
        }
    }

    private fun getBackgroundConfig(): List<BackgroundConfig> {
        return videoUsers.value.map { user: RoomUser ->
            BackgroundConfig(uid = user.rtcUID.toString(), image_url = user.avatarURL)
        }
    }

    private fun getLayoutConfig(): List<LayoutConfig> {
        return videoUsers.value.mapIndexed { index: Int, user: RoomUser ->
            LayoutConfig(
                uid = user.rtcUID.toString(),
                x_axis = 12f / 120,
                y_axis = (8f + index * 80) / 360,
                width = 96f / 120,
                height = 72f / 360,
            )
        }
    }
}

data class RecordState constructor(
    val resourceId: String,
    val sid: String,
    val recordTime: Long = 0,
)