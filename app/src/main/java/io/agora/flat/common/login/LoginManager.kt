package io.agora.flat.common.login

import android.app.Activity
import android.content.*
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import dagger.hilt.android.qualifiers.ApplicationContext
import io.agora.flat.Constants
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginManager @Inject constructor(
    @ApplicationContext val context: Context,
) {
    private var api: IWXAPI? = null
    private var wechatReceiver: BroadcastReceiver? = null
    var actionClazz: Class<out Activity>? = null

    fun registerApp() {
        ensureInit()
        api?.registerApp(Constants.WX_APP_ID)
    }

    private fun ensureInit() {
        if (api == null) {
            api = WXAPIFactory.createWXAPI(context, Constants.WX_APP_ID, true)
        }
    }

    fun registerReceiver(context: Context) {
        if (wechatReceiver != null) {
            wechatReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    registerApp()
                }
            }
        }
        context.registerReceiver(wechatReceiver, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
    }

    fun unregisterReceiver(context: Context) {
        if (wechatReceiver != null) {
            context.unregisterReceiver(wechatReceiver)
            wechatReceiver = null
        }
    }

    fun callWeChatAuth() {
        val req = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "wechat_sdk_flat"
        }
        api?.sendReq(req)
    }

    fun wechatAuthSuccess(context: Context, code: String) {
        if (actionClazz == null) {
            return
        }
        val intent = Intent(context, actionClazz).apply {
            putExtra(Constants.Login.KEY_LOGIN_STATE, Constants.Login.AUTH_SUCCESS)
            putExtra(Constants.Login.KEY_LOGIN_RESP, code)
        }
        context.startActivity(intent)
    }

    fun wechatAuthFail(context: Context, state: Int, errCode: Int, errMessage: String) {
        if (actionClazz == null) {
            return
        }
        val intent = Intent(context, actionClazz).apply {
            putExtra(Constants.Login.KEY_LOGIN_STATE, state)
            putExtra(Constants.Login.KEY_ERROR_CODE, errCode)
            putExtra(Constants.Login.KEY_ERROR_MESSAGE, errMessage)
        }
        context.startActivity(intent)
    }

    fun handleGithubAuth(context: Context, oldIntent: Intent) {
        if (actionClazz == null) {
            return
        }
        val intent = Intent(oldIntent).apply {
            component = ComponentName(context, actionClazz!!)
        }
        context.startActivity(intent)
    }
}
