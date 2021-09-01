package io.agora.flat.common

class FlatErrorCode {
    companion object {
        /**
         * RTM
         */
        const val RTM_LOGIN_ERROR_START = 0x11000
        const val RTM_LOGOUT_ERROR_START = 0x12000

        /**
         * Flat Web 服务
         * https://github.com/netless-io/flat-server/blob/main/src/ErrorCode.ts
         */
        const val Web_ParamsCheckFailed = 100000 // parameter verification failed
        const val Web_ServerFail = 100001 // server fail (retry)
        const val Web_CurrentProcessFailed = 100002 // current processing failed
        const val Web_NotPermission = 100003 // insufficient permissions
        const val Web_NeedLoginAgain = 100004 // user need login in again
        const val Web_UnsupportedPlatform = 100005// Unsupported login platform
        const val Web_JWTSignFailed = 100006// jwt sign failed

        const val Web_RoomNotFound = 200000 // room not found
        const val Web_RoomIsEnded = 200001 // room has been ended
        const val Web_RoomIsRunning = 200002// room status is running
        const val Web_RoomNotIsRunning = 200003 // room not is running
        const val Web_RoomNotIsEnded = 200004 // room not is stopped
        const val Web_RoomNotIsIdle = 200005 // room not is idle

        const val Web_PeriodicNotFound = 300000 // room not found
        const val Web_PeriodicIsEnded = 300001 // room has been ended
        const val Web_PeriodicSubRoomHasRunning = 300002 // periodic sub room has running

        const val Web_UserNotFound = 400000 // user not found
        const val Web_RecordNotFound = 500000 // record info not found

        const val Web_UploadConcurrentLimit = 700000
        const val Web_NotEnoughTotalUsage = 700001 // not enough total usage
        const val Web_FileSizeTooBig = 700002 // single file size too big
        const val Web_FileNotFound = 700003 // file info not found
        const val Web_FileExists = 700004 // file already exists

        const val Web_FileIsConverted = 800000
        const val Web_FileConvertFailed = 800001// file convert failed
        const val Web_FileIsConverting = 800002 // file is converting
        const val Web_FileIsConvertWaiting = 800003 // file convert is in waiting status

        // https://docs.github.com/en/developers/apps/troubleshooting-authorization-request-errors
        const val Web_LoginGithubSuspended = 900000
        const val Web_LoginGithubURLMismatch = 900001
        const val Web_LoginGithubAccessDenied = 900002
    }
}