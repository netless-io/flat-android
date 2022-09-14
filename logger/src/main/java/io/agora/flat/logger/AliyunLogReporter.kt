package io.agora.flat.logger

import android.content.Context
import com.aliyun.sls.android.producer.*
import io.agora.flat.di.interfaces.LogConfig
import io.agora.flat.di.interfaces.LogReporter
import java.io.File
import javax.inject.Inject

internal class AliyunLogReporter @Inject constructor(private val logConfig: LogConfig) : LogReporter {
    private var client: LogProducerClient? = null
    private var uid: String? = null

    override fun init(context: Context) {
        try {
            val endpoint: String = logConfig.endpoint
            val project: String = logConfig.project
            val logStore: String = logConfig.logstore
            val accessKeyId: String = logConfig.ak
            val accessKeySecret: String = logConfig.sk

            val config = LogProducerConfig(
                context,
                endpoint,
                project,
                logStore,
                accessKeyId,
                accessKeySecret,
            )
            config.setPacketLogBytes(1024 * 1024)
            config.setPacketLogCount(1024)
            config.setPacketTimeout(3000)
            config.setMaxBufferLimit(10 * 1024 * 1024)
            config.setSendThreadCount(1)
            config.setConnectTimeoutSec(10)
            config.setSendTimeoutSec(10)
            config.setDestroyFlusherWaitSec(2)
            config.setDestroySenderWaitSec(2)
            config.setCompressType(1)
            config.setMaxLogDelayTime(7 * 24 * 3600)
            config.setDropDelayLog(0)
            config.setDropUnauthorizedLog(0)
            config.setPersistent(1)
            config.setPersistentFilePath(File(context.filesDir, "log_data.dat").path)
            config.setPersistentForceFlush(0)
            config.setPersistentMaxFileCount(10)
            config.setPersistentMaxFileSize(1024 * 1024)
            config.setPersistentMaxLogCount(65536)
            val callback = LogProducerCallback { resultCode, reqId, errorMessage, logBytes, compressedBytes ->
                // ignore
            }
            client = LogProducerClient(config, callback)
        } catch (e: LogProducerException) {
            e.printStackTrace()
        }
    }

    override fun setUserId(id: String) {
        this.uid = id
    }

    override fun report(item: Map<String, String?>) {
        // debug mode not report
        if (BuildConfig.DEBUG) {
            return
        }
        val log = Log()
        log.putContent("uid", uid)
        item.forEach { (k, v) ->
            log.putContent(k, v)
        }
        client?.addLog(log)
    }
}