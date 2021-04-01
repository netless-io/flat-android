package com.agora.netless.flat.ui.activity

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.agora.netless.flat.Constants
import com.agora.netless.flat.R
import com.tencent.mm.opensdk.constants.ConstantsAPI
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.WXAPIFactory

class WeChatLoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_wechat_login)

        regToWx()

        findViewById<View>(R.id.wxLogin).setOnClickListener {
            val req = SendAuth.Req()
            req.scope = "snsapi_userinfo"
            req.state = "wechat_sdk_flat";
            api.sendReq(req)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (wxReceiver != null) {
            unregisterReceiver(wxReceiver)
        }
    }

    private lateinit var api: IWXAPI
    private var wxReceiver: BroadcastReceiver? = null

    private fun regToWx() {
        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, true)
        api.registerApp(Constants.WX_APP_ID)

        wxReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                api.registerApp(Constants.WX_APP_ID)
            }
        }
        registerReceiver(wxReceiver, IntentFilter(ConstantsAPI.ACTION_REFRESH_WXAPP))
    }
}