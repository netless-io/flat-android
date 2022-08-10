package io.agora.flat.logger

import android.content.Context
import io.agora.flat.di.interfaces.Crashlytics
import io.agora.flat.di.interfaces.Logger
import io.agora.flat.di.interfaces.StartupInitializer
import javax.inject.Inject

class TimberInitializer @Inject constructor(
    private val logger: Logger,
    private val crashlytics: Crashlytics,
) : StartupInitializer {

    override fun init(context: Context) {
        crashlytics.init(context)
        logger.setup(BuildConfig.DEBUG)
    }
}
