package io.agora.flat.common.login

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import io.agora.flat.Constants

class LoginHelper(val context: Context) {
    private lateinit var api: IWXAPI
    private var wxReceiver: BroadcastReceiver? = null

    fun register() {
        api = WXAPIFactory.createWXAPI(context, Constants.WX_APP_ID, true)
        api.registerApp(Constants.WX_APP_ID)

        wxReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                api.registerApp(Constants.WX_APP_ID)
            }
        }
        context.registerReceiver(wxReceiver, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
    }

    fun unregister() {
        wxReceiver?.also { context.unregisterReceiver(it) }
    }
}