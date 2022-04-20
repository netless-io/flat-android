package io.agora.flat.common.login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
    private var api: IWXAPI = WXAPIFactory.createWXAPI(context, Constants.WX_APP_ID, true);
    private var wechatReceiver: BroadcastReceiver? = null

    init {
        api.registerApp(Constants.WX_APP_ID)
    }

    fun onRegister(context: Context) {
        if (wechatReceiver != null) {
            wechatReceiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    api.registerApp(Constants.WX_APP_ID)
                }
            }
        }
        context.registerReceiver(wechatReceiver, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
    }

    fun onUnregister(context: Context) {
        if (wechatReceiver != null) {
            context.unregisterReceiver(wechatReceiver)
            wechatReceiver = null
        }
    }

    fun callWeChatLogin() {
        val req = SendAuth.Req().apply {
            scope = "snsapi_userinfo"
            state = "wechat_sdk_flat"
        }
        api.sendReq(req)
    }
}
