package io.agora.flat

class Constants {
    companion object {
        var useDev = true

        // dev 环境
        const val FLAT_SERVER_URL_DEV = "https://flat-api-dev.whiteboard.agora.io/"
        const val GITHUB_CLIENT_ID_DEV = "9821657775fbc74773f1"

        // prod 环境
        const val FLAT_SERVER_URL_PROD = "https://flat-api.whiteboard.agora.io/"
        const val GITHUB_CLIENT_ID_PROD = "71a29285a437998bdfe0"

        val FLAT_SERVICE_URL = if (useDev) FLAT_SERVER_URL_DEV else FLAT_SERVER_URL_PROD

        val GITHUB_CALLBACK = "${FLAT_SERVICE_URL}v1/login/github/callback"
        val GITHUB_CLIENT_ID = if (useDev) GITHUB_CLIENT_ID_DEV else GITHUB_CLIENT_ID_PROD

        const val WX_APP_ID = "wx09437693798bc108"

        const val NETLESS_APP_IDENTIFIER = "cFjxAJjiEeuUQ0211QCRBw/mO9uJB_DiCIqug"

        const val AGORA_APP_ID = "931b86d6781e49a2a255db4ce6e8e804"
    }

    object IntentKey {
        const val ROOM_UUID = "room_uuid"
        const val PERIODIC_UUID = "periodic_uuid"
    }
}