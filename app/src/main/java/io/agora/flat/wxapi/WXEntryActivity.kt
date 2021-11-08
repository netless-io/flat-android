package io.agora.flat.wxapi

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.AndroidEntryPoint
import io.agora.flat.Constants
import io.agora.flat.Constants.Login.AUTH_SUCCESS
import io.agora.flat.data.AppKVCenter
import io.agora.flat.data.repository.UserRepository
import io.agora.flat.ui.activity.LoginActivity
import io.agora.flat.ui.activity.base.BaseActivity
import javax.inject.Inject

@AndroidEntryPoint
class WXEntryActivity : BaseActivity(), IWXAPIEventHandler {
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

    override fun onReq(baseReq: BaseReq?) {

    }

    override fun onResp(baseResp: BaseResp) {
        when (baseResp.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                val code = (baseResp as SendAuth.Resp).code
                Log.d(TAG, "Login Code $code")

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
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra(Constants.Login.KEY_LOGIN_STATE, AUTH_SUCCESS)
            putExtra(Constants.Login.KEY_LOGIN_RESP, code)
        }
        startActivity(intent)
        finish()
    }

    private fun onAuthFail(state: Int, errCode: Int, errMessage: String) {
        val intent = Intent(this, LoginActivity::class.java).apply {
            putExtra(Constants.Login.KEY_LOGIN_STATE, state)
            putExtra(Constants.Login.KEY_ERROR_CODE, errCode)
            putExtra(Constants.Login.KEY_ERROR_MESSAGE, errMessage)
        }
        startActivity(intent)
        finish()
    }
}