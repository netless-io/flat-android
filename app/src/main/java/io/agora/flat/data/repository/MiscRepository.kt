package io.agora.flat.data.repository

import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.Result
import io.agora.flat.data.model.PureRoomReq
import io.agora.flat.data.model.PureToken
import io.agora.flat.data.model.RespNoData
import io.agora.flat.data.model.RtmCensorReq
import io.agora.flat.data.model.StreamAgreementReq
import io.agora.flat.data.toResult
import io.agora.flat.http.api.MiscService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MiscRepository @Inject constructor(
    private val miscService: MiscService,
    private val appKVCenter: AppKVCenter,
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

    suspend fun censorRtm(text: String): Boolean {
        return withContext(Dispatchers.IO) {
            val result = miscService.censorRtm(RtmCensorReq(text)).toResult()
            return@withContext result.get()?.valid == true
        }
    }

    suspend fun getRegionConfigs() {
        return withContext(Dispatchers.IO) {
            val result = miscService.getRegionConfigs().toResult()
            result.get()?.let {
                val joinEarly = it.server?.joinEarly ?: 10
                appKVCenter.setJoinEarly(joinEarly)
            }
        }
    }

    suspend fun getStreamAgreement(): Boolean? {
        return withContext(Dispatchers.IO) {
            val result = miscService.getStreamAgreement().toResult()
            return@withContext result.get()?.isAgree
        }
    }

    suspend fun setStreamAgreement(isAgree: Boolean): Result<RespNoData> {
        return withContext(Dispatchers.IO) {
            return@withContext miscService.setStreamAgreement(StreamAgreementReq(isAgree)).toResult()
        }
    }
}