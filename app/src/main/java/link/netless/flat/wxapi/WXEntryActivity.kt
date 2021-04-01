package link.netless.flat.wxapi

import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.agora.netless.flat.Constants
import com.tencent.mm.opensdk.modelbase.BaseReq
import com.tencent.mm.opensdk.modelbase.BaseResp
import com.tencent.mm.opensdk.modelmsg.SendAuth
import com.tencent.mm.opensdk.openapi.IWXAPI
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler
import com.tencent.mm.opensdk.openapi.WXAPIFactory
import okhttp3.*
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException

class WXEntryActivity : Activity(), IWXAPIEventHandler {
    val TAG = WXEntryActivity::class.simpleName

    private lateinit var api: IWXAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        api = WXAPIFactory.createWXAPI(this, Constants.WX_APP_ID, false);
        api.handleIntent(intent, this);
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    override fun onReq(p0: BaseReq?) {

    }

    override fun onResp(baseResp: BaseResp?) {
        when (baseResp?.errCode) {
            BaseResp.ErrCode.ERR_OK -> {
                val code: String = (baseResp as SendAuth.Resp).code
                Log.d(TAG, "Login Code $code")

                getAccessToken(code)
            }
            BaseResp.ErrCode.ERR_AUTH_DENIED -> finish()
            BaseResp.ErrCode.ERR_USER_CANCEL -> finish()
            else -> finish()
        }
    }

    private fun getAccessToken(code: String) {
        createProgressDialog()
        // 获取授权
        val loginUrl = StringBuffer()
        loginUrl.append("https://api.weixin.qq.com/sns/oauth2/access_token")
            .append("?appid=")
            .append(Constants.WX_APP_ID)
            .append("&secret=")
            //.append(Constants.WX_APP_SECRET)
            .append("&code=")
            .append(code)
            .append("&grant_type=authorization_code")
        Log.d(TAG, "URL: $loginUrl")

        val okHttpClient = OkHttpClient()
        val request: Request = Request.Builder()
            .url(loginUrl.toString())
            .build()
        val call: Call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "onFailure: ")
                mProgressDialog.dismiss()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseInfo: String = response.body?.string() ?: ""
                Log.d(TAG, "onResponse: $responseInfo")
                mProgressDialog.dismiss()
                var access: String? = null
                var openId: String? = null
                try {
                    val jsonObject = JSONObject(responseInfo)
                    access = jsonObject.getString("access_token")
                    openId = jsonObject.getString("openid")
                } catch (e: JSONException) {
                    e.printStackTrace()
                }
                access?.let {
                    if (openId != null) {
                        getUserInfo(it, openId)
                    }
                }
            }
        })
    }

    private fun getUserInfo(access: String, openid: String) {
        val getUserInfoUrl =
            "https://api.weixin.qq.com/sns/userinfo?access_token=$access&openid=$openid"
        val okHttpClient = OkHttpClient()
        val request: Request = Request.Builder()
            .url(getUserInfoUrl)
            .get() //默认就是GET请求，可以不写
            .build()
        val call = okHttpClient.newCall(request)
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "onFailure: ")
                mProgressDialog.dismiss()
            }

            override fun onResponse(call: Call, response: Response) {
                val responseInfo = response.body?.string()
                Log.d(TAG, "onResponse: $responseInfo")
                finish()
                mProgressDialog.dismiss()
            }
        })
    }

    private lateinit var mProgressDialog: ProgressDialog

    private fun createProgressDialog() {
        mProgressDialog = ProgressDialog(this)
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER) //转盘
        mProgressDialog.setCancelable(false)
        mProgressDialog.setCanceledOnTouchOutside(false)
        mProgressDialog.setTitle("提示")
        mProgressDialog.setMessage("登录中，请稍后")
        mProgressDialog.show()
    }
}