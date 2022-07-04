package io.agora.flat

class Config {
    companion object {
        var forceBindPhone = !BuildConfig.DEBUG

        // three days in milliseconds
        const val INTERVAL_VERSION_CHECK = 259_200_000L
    }
}