package io.agora.flat.logger

import android.os.Build
import android.util.Log
import io.agora.flat.di.interfaces.Crashlytics
import io.agora.flat.di.interfaces.LogReporter
import io.agora.flat.di.interfaces.Logger
import timber.log.Timber
import java.util.regex.Pattern
import javax.inject.Inject

internal class FlatLogger @Inject constructor(
    private val crashlytics: Crashlytics,
    private val logReporter: LogReporter,
) : Logger {
    override fun setup(debugMode: Boolean) {
        if (debugMode) {
            Timber.plant(FlatDebugTree())
        }
        try {
            Timber.plant(CrashlyticsTree(crashlytics))
            Timber.plant(LogReporterTree(logReporter))
        } catch (e: IllegalStateException) {
            // Crashlytics is likely not setup in this project. Ignore the exception
        }
    }

    override fun setUserId(id: String) {
        try {
            crashlytics.setUserId(id)
            logReporter.setUserId(id)
        } catch (e: IllegalStateException) {
            // Crashlytics is likely not setup in this project. Ignore the exception
        }
    }

    override fun v(message: String, vararg args: Any?) {
        Timber.v(message, *args)
    }

    override fun v(t: Throwable, message: String, vararg args: Any?) {
        Timber.v(t, message, *args)
    }

    override fun v(t: Throwable) {
        Timber.v(t)
    }

    override fun d(message: String, vararg args: Any?) {
        Timber.d(message, *args)
    }

    override fun d(t: Throwable, message: String, vararg args: Any?) {
        Timber.d(t, message, *args)
    }

    override fun d(t: Throwable) {
        Timber.d(t)
    }

    override fun i(message: String, vararg args: Any?) {
        Timber.i(message, *args)
    }

    override fun i(t: Throwable, message: String, vararg args: Any?) {
        Timber.i(t, message, *args)
    }

    override fun i(t: Throwable) {
        Timber.i(t)
    }

    override fun w(message: String, vararg args: Any?) {
        Timber.w(message, *args)
    }

    override fun w(t: Throwable, message: String, vararg args: Any?) {
        Timber.w(t, message, *args)
    }

    override fun w(t: Throwable) {
        Timber.w(t)
    }

    override fun e(message: String, vararg args: Any?) {
        Timber.e(message, *args)
    }

    override fun e(t: Throwable, message: String, vararg args: Any?) {
        Timber.e(t, message, *args)
    }

    override fun e(t: Throwable) {
        Timber.e(t)
    }

    override fun wtf(message: String, vararg args: Any?) {
        Timber.wtf(message, *args)
    }

    override fun wtf(t: Throwable, message: String, vararg args: Any?) {
        Timber.wtf(t, message, *args)
    }

    override fun wtf(t: Throwable) {
        Timber.wtf(t)
    }
}

/**
 * Special version of [Timber.DebugTree] which is tailored for Timber being wrapped
 * within another class.
 */
private class FlatDebugTree : Timber.DebugTree() {
    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        super.log(priority, createClassTag(), message, t)
    }

    private fun createClassTag(): String {
        val stackTrace = Throwable().stackTrace
        if (stackTrace.size <= CALL_STACK_INDEX) {
            throw IllegalStateException("Synthetic stacktrace didn't have enough elements: are you using proguard?")
        }
        var tag = stackTrace[CALL_STACK_INDEX].className
        val m = ANONYMOUS_CLASS.matcher(tag)
        if (m.find()) {
            tag = m.replaceAll("")
        }
        tag = tag.substring(tag.lastIndexOf('.') + 1)
        // Tag length limit was removed in API 24.
        return when {
            Build.VERSION.SDK_INT >= 24 || tag.length <= MAX_TAG_LENGTH -> tag
            else -> tag.substring(0, MAX_TAG_LENGTH)
        }
    }

    companion object {
        private const val MAX_TAG_LENGTH = 23
        private const val CALL_STACK_INDEX = 7
        private val ANONYMOUS_CLASS by lazy { Pattern.compile("(\\$\\d+)+$") }
    }
}

private class CrashlyticsTree(
    private val crashlytics: Crashlytics,
) : Timber.Tree() {
    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.WARN
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        crashlytics.log(priority, tag, message, t)
    }
}

private class LogReporterTree(
    private val logReporter: LogReporter,
) : Timber.Tree() {

    companion object {
        val priorityToLogLevel = mapOf(
            Log.VERBOSE to "VERBOSE",
            Log.DEBUG to "DEBUG",
            Log.INFO to "INFO",
            Log.WARN to "WARN",
            Log.ERROR to "ERROR",
            Log.ASSERT to "ASSERT"
        )
    }

    override fun isLoggable(tag: String?, priority: Int): Boolean {
        return priority >= Log.INFO
    }

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        val logLevel = priorityToLogLevel[priority]?.lowercase() ?: "UNKNOWN"
        val logMap = mutableMapOf(
            "message" to message,
            "priority" to logLevel,
        )
        tag?.let {
            logMap["tag"] = tag
        }
        t?.let {
            logMap["t"] = t.stackTraceToString()
        }
        logReporter.report(logMap)
    }


}
