package io.agora.flat.data.repository

import io.agora.flat.data.Result
import io.agora.flat.data.model.PureRoomReq
import io.agora.flat.data.model.PureToken
import io.agora.flat.data.model.RtmCensorReq
import io.agora.flat.data.toResult
import io.agora.flat.http.api.MiscService
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

    // suspend fun logError(message: String) {
    //     return withContext(Dispatchers.IO) {
    //         miscService.logError(LogErrorReq(message)).toResult()
    //     }
    // }

    suspend fun censorRtm(text: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = miscService.censorRtm(RtmCensorReq(text)).toResult()
            return@withContext result.get()?.valid == true
        }
    }
}