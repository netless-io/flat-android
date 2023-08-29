package io.agora.flat.wxapi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.common.login.LoginManager
import io.agora.flat.data.AppEnv
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository
import javax.inject.Inject

@AndroidEntryPoint
class WXEntryActivity : ComponentActivity(), IWXAPIEventHandler {
    companion object {
        @JvmStatic
        private val TAG = WXEntryActivity::class.simpleName
    }

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appKVCenter: AppKVCenter

    @Inject
    lateinit var loginManager: LoginManager

    @Inject
    lateinit var appEnv: AppEnv

    private lateinit var api: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = WXAPIFactory.createWXAPI(this, appEnv.wechatId, false)
        wxApiHandleIntent()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        wxApiHandleIntent()
    }

    private fun wxApiHandleIntent() {
        if (!api.handleIntent(intent, this)) {
            finish()
        }
    }

    override fun onReq(baseReq: BaseReq?) {
        Log.d(TAG, "wx login onReq call $baseReq")
    }

    override fun onResp(baseResp: BaseResp) {
        Log.d(TAG, "wx login onResp call $baseResp")
        when (baseResp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                val code = (baseResp as SendAuth.Resp).code
                onAuthSuccess(code)
            }

            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                onAuthFail(Constants.Login.AUTH_DENIED, baseResp.errCode, baseResp.errStr)
            }

            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                onAuthFail(Constants.Login.AUTH_CANCEL, baseResp.errCode, baseResp.errStr)
            }

            else -> {
                onAuthFail(Constants.Login.AUTH_ERROR, baseResp.errCode, baseResp.errStr)
            }
        }
    }

    private fun onAuthSuccess(code: String) {
        loginManager.wechatAuthSuccess(this, code)
        finish()
    }

    private fun onAuthFail(state: Int, errCode: Int, errMessage: String) {
        loginManager.wechatAuthFail(this, state, errCode, errMessage)
        finish()
    }
}