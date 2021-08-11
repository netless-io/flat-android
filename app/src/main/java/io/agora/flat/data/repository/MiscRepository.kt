package io.agora.flat.data.repository

import io.agora.flat.data.Result
import io.agora.flat.data.api.MiscService
import io.agora.flat.data.model.PureRoomReq
import io.agora.flat.data.model.PureToken
import io.agora.flat.data.toResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiscRepository @Inject constructor(
    private val miscService: MiscService,
) {
    suspend fun generateRtcToken(roomUUID: String): Result<PureToken> {
        return withContext(Dispatchers.IO) {
            miscService.generateRtcToken(PureRoomReq(roomUUID)).toResult()
        }
    }

    suspend fun generateRtmToken(page: Int): Result<PureToken> {
        return withContext(Dispatchers.IO) {
            miscService.generateRtmToken().toResult()
        }
    }
}