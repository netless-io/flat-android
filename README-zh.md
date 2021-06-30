<div align="center">
    <img width="200" height="200" style="display: block;" src="./assets/flat-logo.png">
</div>

<div align="center">
    <img alt="GitHub" src="https://img.shields.io/github/license/netless-io/flat?color=9cf&style=flat-square">
</div>

<div align="center">
    <h1>Agora Flat</h1>
    <p>项目 flat 是 <a href="https://flat.whiteboard.agora.io/">Agora Flat</a> 开源教室的 Web 端、Android 端、Windows 客户端与 macOS 客户端。</p>
</div>

## 产品体验

-   [下载地址][flat-homepage]

## 特性

-   多场景课堂
    -   [ ] 大班课
    -   [ ] 小班课
    -   [ ] 一对一
-   实时交互
    -   [ ] 多功能互动白板
    -   [ ] 实时音视频（RTC）通讯
    -   [ ] 即时消息（RTM）聊天
    -   [ ] 举手上麦发言
-   帐户系统
    -   [x] 微信登陆
    -   [x] GitHub 登陆
    -   [ ] 谷歌登陆
-   房间管理
    -   [x] 加入、创建
    -   [ ] 预定房间
    -   [x] 支持周期性房间
    -   [x] 查看历史房间
-   课堂录制回放
    -   [ ] 白板信令回放
    -   [ ] 音视频云录制回放
    -   [ ] 群聊信令回放
-   [ ] 多媒体课件云盘
-   [ ] 设备检测
-   [ ] 自动检查更新

## 本地开发

### 开发环境

* JDK 1.8
* Android SDK
  - Android Studio 4.2.0+


## 环境变量值参考

| 变量名                               | 描述                                               | 备注                                                             |
| ------------------------------------ | -------------------------------------------------- | ---------------------------------------------------------------- |
| NETLESS_APP_IDENTIFIER               | 互动白板 Access Key                                | 见: [在 app 服务端生成 Token][netless-auth]                      |
| AGORA_APP_ID                         | Agora 声网 App ID                                  | 用于 RTC 与 RTM。见: [校验用户权限][agora-app-id-auth]           |
| WECHAT_APP_ID                        | [微信开放平台][open-wechat] App ID                 | 见 `网站应用` 里 `AppID`                                         |
| FLAT_SERVER_DOMAIN                   | Flat Server 部署的域名地址                         | 如: `flat-api.whiteboard.agora.io`                               |
|                                                                  |

[flat-homepage]: https://flat.whiteboard.agora.io/
[flat-server]: https://github.com/netless-io/flat-server
[flat-storybook]: https://netless-io.github.io/flat/storybook/
[open-wechat]: https://open.weixin.qq.com/
[netless-auth]: https://docs.agora.io/cn/whiteboard/generate_whiteboard_token_at_app_server?platform=RESTful
[agora-app-id-auth]: https://docs.agora.io/cn/Agora%20Platform/token#a-name--appidause-an-app-id-for-authentication
[cloud-recording]: https://docs.agora.io/cn/cloud-recording/cloud_recording_api_rest?platform=RESTful#storageConfig
[cloud-recording-background]: https://docs.agora.io/cn/cloud-recording/cloud_recording_layout?platform=RESTful#background
[electron-updater]: https://github.com/electron-userland/electron-builder/tree/master/packages/electron-updater
