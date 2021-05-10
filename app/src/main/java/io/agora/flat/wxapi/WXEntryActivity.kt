package io.agora.flat.wxapi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.ErrorResult
import io.agora.flat.data.Success
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.util.showToast
import kotlinx.coroutines.launch
import javax.inject.Inject


@AndroidEntryPoint
class WXEntryActivity : ComponentActivity(), IWXAPIEventHandler {
    private val TAG = WXEntryActivity::class.simpleName

    @Inject
    lateinit var userRepository: UserRepository

    @Inject
    lateinit var appKVCenter: AppKVCenter

    private lateinit var api: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, false)
        api.handleIntent(intent, this)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        api.handleIntent(intent, this)
    }

    override fun onReq(p0: BaseReq?) {

    }

    override fun onResp(baseResp: BaseResp?) {
        when (baseResp?.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                val code: String = (baseResp as SendAuth.Resp).code
                Log.d(TAG, "Login Code $code")

                authWeChat(code)
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> {
                onAuthFail("拒绝授权")
            }
            BaseResp.ErrCode.ERR_USER_CANCEL -> {
                onAuthFail("用户取消")
            }
            else -> {
                onAuthFail("授权失败")
            }
        }
    }

    private fun authWeChat(code: String) {
        lifecycleScope.launch {
            when (val resp = userRepository.loginWeChatCallback(appKVCenter.getAuthUUID(), code)) {
                is Success -> onAuthSuccess("登录成功")
                is ErrorResult -> onAuthFail("授权失败（${resp.error.code}）")
            }
        }
    }

    private fun onAuthSuccess(message: String) {
        showToast(message)
        finish()
    }

    private fun onAuthFail(message: String) {
        showToast(message)
        finish()
    }
}