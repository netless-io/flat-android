package io.agora.flat.common

class FlatErrorCode {
    object Web {
        /**
         * Flat Web 服务
         * https://github.com/netless-io/flat-server/blob/main/src/ErrorCode.ts
         */
        const val ParamsCheckFailed = 100000 // parameter verification failed
        const val ServerFail = 100001 // server fail (retry)
        const val CurrentProcessFailed = 100002 // current processing failed
        const val NotPermission = 100003 // insufficient permissions
        const val NeedLoginAgain = 100004 // user need login in again
        const val UnsupportedPlatform = 100005// Unsupported login platform
        const val JWTSignFailed = 100006// jwt sign failed
        const val ExhaustiveAttack = 100007// exhaustive attack
        const val RequestSignatureIncorrect = 100008// exhaustive attack
        const val NonCompliant = 100009 // non compliant
        const val UnsupportedOperation = 100010 // operation not supported

        const val RoomNotFound = 200000 // room not found
        const val RoomIsEnded = 200001 // room has been ended
        const val RoomIsRunning = 200002// room status is running
        const val RoomNotIsRunning = 200003 // room not is running
        const val RoomNotIsEnded = 200004 // room not is stopped
        const val RoomNotIsIdle = 200005 // room not is idle
        const val RoomExists = 200006 // (pmi) room already exists, cannot create new room
        const val RoomNotFoundAndIsPmi = 200007 // room not found and the invite code is pmi

        const val BadRequest = 210000  // bad request
        const val JWTVerifyFailed = 210001  // jwt verify failed
        const val RoomLimit = 210002  // join room reach max user limit
        const val RoomExpired = 210003  // room expired
        const val RoomNotBegin = 210004  // join room before begin_time
        const val RoomCreateLimit = 210005  // create room reach max limit
        const val RoomNotBeginAndAddList = 210006 // join room before begin_time and joined

        const val InternalError = 220000 // unknown error
        const val ForwardFailed = 220001 // forward failed

        const val PeriodicNotFound = 300000 // room not found
        const val PeriodicIsEnded = 300001 // room has been ended
        const val PeriodicSubRoomHasRunning = 300002 // periodic sub room has running

        const val UserNotFound = 400000 // user not found
        const val UserRoomListNotEmpty = 400001 // user room list is not empty.
        const val UserAlreadyBinding = 400002 // user already binding.
        const val UserPasswordIncorrect = 400003 // user password (for update) incorrect
        const val UserOrPasswordIncorrect = 400004 // user or password (for login) incorrect

        const val RecordNotFound = 500000 // record info not found

        const val UploadConcurrentLimit = 700000
        const val NotEnoughTotalUsage = 700001 // not enough total usage
        const val FileSizeTooBig = 700002 // single file size too big
        const val FileNotFound = 700003 // file info not found
        const val FileExists = 700004 // file already exists
        const val DirectoryNotExists = 700005 // directory not exists
        const val DirectoryAlreadyExists = 700006 // directory already exists

        const val FileIsConverted = 800000
        const val FileConvertFailed = 800001// file convert failed
        const val FileIsConverting = 800002 // file is converting
        const val FileIsConvertWaiting = 800003 // file convert is in waiting status
        const val FileNotIsConvertNone = 800004 // file convert not is none
        const val FileNotIsConverting = 800005 // file convert is processing

        // https://docs.github.com/en/developers/apps/troubleshooting-authorization-request-errors
        const val LoginGithubSuspended = 900000
        const val LoginGithubURLMismatch = 900001
        const val LoginGithubAccessDenied = 900002

        const val SMSVerificationCodeInvalid = 110000// verification code invalid
        const val SMSAlreadyExist = 110001 // phone already exist by current user
        const val SMSAlreadyBinding = 110002 // phone are binding by other users
        const val SMSFailedToSendCode = 110003 // failed to send verification code

        const val EmailVerificationCodeInvalid = 115000  // verification code invalid
        const val EmailAlreadyExist = 115001  // email already exist by current user
        const val EmailAlreadyBinding = 115002  // email are binding by other users
        const val EmailFailedToSendCode = 115003 // failed to send verification code

        const val CensorshipFailed = 120000 // censorship failed

        const val OAuthUUIDNotFound = 130000 // oauth uuid not found
        const val OAuthClientIDNotFound = 130001 // oauth client id not found
        const val OAuthSecretUUIDNotFound = 130002 // oauth secret uuid not found
    }

    object RTM {
        /**
         * Agora RTM
         */
        const val RTM_LOGIN_ERROR_START = 0x11000

        const val RTM_LOGOUT_ERROR_START = 0x12000
    }

    object RTC {

    }
}