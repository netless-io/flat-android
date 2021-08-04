package io.agora.flat

class Constants {
    companion object {
        const val WX_APP_ID = "wx09437693798bc108"

        const val NETLESS_APP_IDENTIFIER = "cFjxAJjiEeuUQ0211QCRBw/mO9uJB_DiCIqug"

        const val AGORA_APP_ID = "931b86d6781e49a2a255db4ce6e8e804"

        const val OSS_ACCESS_KEY_ID = "LTAI5t9Gb6tzQzzLmB6cTVf7"
    }

    object IntentKey {
        const val ROOM_UUID = "room_uuid"
        const val PERIODIC_UUID = "periodic_uuid"
    }

    object Login {
        const val AUTH_SUCCESS = 0;
        const val AUTH_DENIED = 1;
        const val AUTH_CANCEL = 2;
        const val AUTH_ERROR = 3;

        const val KEY_LOGIN_STATE = "login_state";
        const val KEY_LOGIN_RESP = "login_resp";
        const val KEY_ERROR_CODE = "error_code";
        const val KEY_ERROR_MESSAGE = "error_message";
    }
}