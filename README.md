<div align="center">
    <img width="200" height="200" style="display: block;" src="./assets/flat-logo.png">
</div>

<div align="center">
    <img alt="GitHub" src="https://img.shields.io/github/license/netless-io/flat?color=9cf&style=flat-square">
</div>

<div align="center">
    <h1>Agora Flat Android</h1>
    <p>Project flat is the Web, Windows and macOS client of <a href="https://flat.whiteboard.agora.io/en/">Agora Flat</a> open source classroom.</p>
    <p><a href="./README-zh.md">中文</a></p>
</div>


## Try it now

-   [Download artifact][flat-homepage]

## Features

-   Optimized teaching experience
    -   [ ] Big class
    -   [ ] Small class
    -   [ ] One on one
-   Real-time interaction
    -   [ ] Multifunctional interactive whiteboard
    -   [ ] Real-time video/audio chat(RTC)
    -   [x] Real-time messaging(RTM)
    -   [ ] Participant hand raising
-   Login via
    -   [x] Wechat
    -   [x] GitHub
    -   [ ] Google
-   Classroom management
    -   [x] Join and create classrooms
    -   [x] Support periodic rooms
    -   [x] View room history
    -   [ ] schedule classrooms
-   Classroom recording and replaying
    -   [ ] Whiteboard replaying
    -   [ ] Cloud recording for video and audio
    -   [ ] Messaging replaying
-   [ ] Cloud Storage for multi-media courseware
-   [ ] Device self-check
-   [ ] Auto Updater

## Development

### Prerequisites

* JDK 1.8
* Android SDK
  - Android Studio 4.2.0+



## Environment Variables Reference

| Variable                             | Description                                              | Note                                                                                |
| ------------------------------------ | -------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| NETLESS_APP_IDENTIFIER               | Whiteboard Access Key                                    | See [Projects and permissions][netless-auth]                                        |
| AGORA_APP_ID                         | Agora App ID                                             | For RTC and RTM. See [Use an App ID for authentication][agora-app-id-auth]          |
| WECHAT_APP_ID                        | [Wechat Open Platform][open-wechat] App ID               |                                                                                     |
| FLAT_SERVER_DOMAIN                   | Flat Server deployed address                             | e.g. `flat-api.whiteboard.agora.io`                                                 |
|                                                                                     |

[flat-homepage]: https://flat.whiteboard.agora.io/
[flat-server]: https://github.com/netless-io/flat-server
[flat-storybook]: https://netless-io.github.io/flat/storybook/
[open-wechat]: https://open.weixin.qq.com/
[netless-auth]: https://docs.agora.io/en/whiteboard/generate_whiteboard_token_at_app_server?platform=RESTful
[agora-app-id-auth]: https://docs.agora.io/en/Agora%20Platform/token#a-name--appidause-an-app-id-for-authentication
[cloud-recording]: https://docs.agora.io/en/cloud-recording/cloud_recording_api_rest?platform=RESTful#storageConfig
[cloud-recording-background]: https://docs.agora.io/en/cloud-recording/cloud_recording_layout?platform=RESTful#background
[electron-updater]: https://github.com/electron-userland/electron-builder/tree/master/packages/electron-updater
