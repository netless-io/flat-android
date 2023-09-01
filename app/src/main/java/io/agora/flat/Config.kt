package io.agora.flat

class Config {
    companion object {
        // three days in milliseconds
        const val INTERVAL_VERSION_CHECK = 259_200_000L

        // ten minutes in milliseconds
        var callVersionCheckInterval = 10 * 60_000L

        val cancelAccountCountTime = if (BuildConfig.DEBUG) 3_000L else 30_000L

        const val defaultBoardRatio = 9f / 16

        const val defaultWindowScale = 0.4f

        const val defaultMinWindowScale = 0.25f
    }
}