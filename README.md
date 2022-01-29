<div align="center">
    <img width="200" height="200" style="display: block;" src="art/flat-logo.png">
</div>

<!-- 
<div align="center">
    <img alt="GitHub" src="https://img.shields.io/github/license/netless-io/flat?color=9cf&style=flat-square">
</div> 
-->

<div align="center">
    <h1>Agora Flat Android</h1>
    <p>Project Flat Android is the Android client of <a href="https://flat.whiteboard.agora.io/en/">Agora Flat</a> open source classroom.</p>
    <img src="art/flat-showcase.png">
    <p><a href="./README-zh.md">中文</a></p>
</div>


# Try it now

-   [Download artifact][flat-homepage]
-   [Start using Flat Web][flat-web]

# Features
-   Open sourced front-end and back-end
    -   [x] [Flat Web][flat-web]
    -   [x] Flat Desktop ([Windows][flat-homepage] and [macOS][flat-homepage])
    -   [x] [Flat Android][flat-android]
    -   [x] [Flat Server][flat-server]
-   Optimized teaching experience
    -   [x] Big class
    -   [x] Small class
    -   [x] One on one
-   Real-time interaction
    -   [x] Multifunctional interactive whiteboard
    -   [x] Real-time video/audio chat(RTC)
    -   [x] Real-time messaging(RTM)
    -   [x] Participant hand raising
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
    -   [x] Whiteboard replaying
    -   [x] Cloud recording for video and audio
    -   [x] Messaging replaying
-   [x] Cloud Storage for multi-media courseware
-   [ ] Device self-check
-   [ ] Auto Updater

# Development

## Android Code
Flat is an app that attempts to use the latest cutting edge libraries and tools. As a summary:

* Entirely written in Kotlin.
* UI Mostly written in Jetpack Compose (see below).
* Uses Kotlin Coroutines throughout.
* Uses many of the Architecture Components, including: Room, Lifecycle, ViewModel.
* Uses Hilt for dependency injection

## Development Tool
Flat require the latest Android Studio Arctic Fox release to be able to build the app. This is because the project is written in Jetpack Compose (more on that below).


## Environment Variables Reference

| Variable                             | Description                                              | Note                                                                                |
| ------------------------------------ | -------------------------------------------------------- | ----------------------------------------------------------------------------------- |
| NETLESS_APP_IDENTIFIER               | Whiteboard Access Key                                    | See [Projects and permissions][netless-auth]                                        |
| AGORA_APP_ID                         | Agora App ID                                             | For RTC and RTM. See [Use an App ID for authentication][agora-app-id-auth]          |
| WECHAT_APP_ID                        | [Wechat Open Platform][open-wechat] App ID               |                                                                                     |
| FLAT_SERVER_DOMAIN                   | Flat Server deployed address                             | e.g. `flat-api.whiteboard.agora.io`                                                 |
|                                                                                     |

## Disclaimers
You may use Flat for commercial purposes but please note that we do not accept customizational commercial requirements and deployment supports. Nor do we offer customer supports for commercial usage. Please head to [agora-flexible-classroom](https://www.agora.io/en/products/flexible-classroom) for such requirements.

[flat-homepage]: https://flat.whiteboard.agora.io/en/#download
[flat-web]: https://flat-web.whiteboard.agora.io/
[flat-server]: https://github.com/netless-io/flat-server
[flat-android]: https://github.com/netless-io/flat-android
[flat-storybook]: https://netless-io.github.io/flat/storybook/
[open-wechat]: https://open.weixin.qq.com/
[netless-auth]: https://docs.agora.io/en/whiteboard/generate_whiteboard_token_at_app_server?platform=RESTful
[agora-app-id-auth]: https://docs.agora.io/en/Agora%20Platform/token#a-name--appidause-an-app-id-for-authentication
[cloud-recording]: https://docs.agora.io/en/cloud-recording/cloud_recording_api_rest?platform=RESTful#storageConfig
[cloud-recording-background]: https://docs.agora.io/en/cloud-recording/cloud_recording_layout?platform=RESTful#background
[electron-updater]: https://github.com/electron-userland/electron-builder/tree/master/packages/electron-updater
