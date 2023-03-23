package io.agora.flat

class Config {
    companion object {
        var forceBindPhone = !BuildConfig.DEBUG

        // three days in milliseconds
        const val INTERVAL_VERSION_CHECK = 259_200_000L

        // on hour in milliseconds
        var callVersionCheckInterval = 3_600_000L

        val cancelAccountCountTime = if (BuildConfig.DEBUG) 5_000L else 30_000L

        const val defaultBoardRatio = 9f / 16

        const val defaultWindowScale = 0.4f
        const val defaultMinWindowScale = 0.25f
    }
}