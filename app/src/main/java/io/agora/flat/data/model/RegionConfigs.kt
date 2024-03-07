package io.agora.flat.data.model

import com.google.gson.annotations.SerializedName

data class RegionConfigs(
    @SerializedName("hash") var hash: String? = null,
    @SerializedName("login") var login: Login? = Login(),
    @SerializedName("server") var server: Server? = Server(),
    @SerializedName("whiteboard") var whiteboard: Whiteboard? = Whiteboard(),
    @SerializedName("agora") var agora: Agora? = Agora(),
    @SerializedName("github") var github: Github? = Github(),
    @SerializedName("wechat") var wechat: Wechat? = Wechat(),
    @SerializedName("google") var google: Google? = Google(),
    @SerializedName("cloudStorage") var cloudStorage: CloudStorage? = CloudStorage(),
    @SerializedName("censorship") var censorship: Censorship? = Censorship()
)

data class Login(
    @SerializedName("wechatWeb") var wechatWeb: Boolean? = null,
    @SerializedName("wechatMobile") var wechatMobile: Boolean? = null,
    @SerializedName("github") var github: Boolean? = null,
    @SerializedName("google") var google: Boolean? = null,
    @SerializedName("apple") var apple: Boolean? = null,
    @SerializedName("agora") var agora: Boolean? = null,
    @SerializedName("sms") var sms: Boolean? = null,
    @SerializedName("smsForce") var smsForce: Boolean? = null
)

data class Server(
    @SerializedName("region") var region: String? = null,
    @SerializedName("regionCode") var regionCode: Int? = null,
    @SerializedName("env") var env: String? = null,
    @SerializedName("joinEarly") var joinEarly: Int? = null,
)

data class Whiteboard(
    @SerializedName("appId") var appId: String? = null,
    @SerializedName("convertRegion") var convertRegion: String? = null
)

data class Agora(
    @SerializedName("clientId") var clientId: String? = null,
    @SerializedName("appId") var appId: String? = null,
    @SerializedName("screenshot") var screenshot: Boolean? = null,
    @SerializedName("messageNotification") var messageNotification: Boolean? = null
)

data class Github(
    @SerializedName("clientId") var clientId: String? = null
)

data class Wechat(
    @SerializedName("webAppId") var webAppId: String? = null,
    @SerializedName("mobileAppId") var mobileAppId: String? = null
)

data class Google(
    @SerializedName("clientId") var clientId: String? = null
)

data class CloudStorage(
    @SerializedName("singleFileSize") var singleFileSize: Long? = null,
    @SerializedName("totalSize") var totalSize: Long? = null,
    @SerializedName("allowFileSuffix") var allowFileSuffix: ArrayList<String> = arrayListOf(),
    @SerializedName("accessKey") var accessKey: String? = null
)

data class Censorship(

    @SerializedName("video") var video: Boolean? = null,
    @SerializedName("voice") var voice: Boolean? = null,
    @SerializedName("text") var text: Boolean? = null

)