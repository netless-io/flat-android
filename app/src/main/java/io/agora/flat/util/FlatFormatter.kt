package io.agora.flat.util

import android.content.Context
import io.agora.flat.R
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object FlatFormatter {
    private val timeFormatter = SimpleDateFormat("HH:mm")
    private val timeMsFormatter = SimpleDateFormat("mm:ss")
    private val dateFormat = SimpleDateFormat("MM/dd")
    private val dateDashFormat = SimpleDateFormat("MM-dd")
    private val dateMiscFormat = SimpleDateFormat("MM月dd日")
    private val longDateFormat = SimpleDateFormat("yyyy/MM/dd")
    private val longDateWithWeekFormat = SimpleDateFormat("yyyy/MM/dd EE", Locale.CHINA)

    fun time(utcMs: Long): String {
        return timeFormatter.format(utcMs)
    }

    fun timeMS(utcMs: Long): String {
        return timeMsFormatter.format(utcMs)
    }

    fun date(utcMs: Long): String {
        return dateFormat.format(utcMs)
    }

    fun dateDash(utcMs: Long): String {
        return dateDashFormat.format(utcMs)
    }

    // TODO to Support locale
    fun dateMisc(utcMs: Long): String {
        return dateMiscFormat.format(utcMs)
    }

    fun formatLongDate(utcMs: Long): String {
        return longDateFormat.format(utcMs)
    }

    fun longDateWithWeek(utcMs: Long): String {
        return longDateWithWeekFormat.format(utcMs)
    }

    fun timeDuring(begin: Long, end: Long): String {
        return "${timeFormatter.format(begin)}~${timeFormatter.format(end)}"
    }

    // TODO 国际化支持
    fun diffTime(context: Context, begin: Long, end: Long): String {
        val diff = end - begin
        if (diff < 0) {
            return context.getString(R.string.relative_time_mm, 0)
        }
        if (diff < 3600_000) {
            return context.getString(R.string.relative_time_mm, TimeUnit.MILLISECONDS.toMinutes(diff))
        }
        if (diff < 86400_000) {
            return context.getString(R.string.relative_time_hh, TimeUnit.MILLISECONDS.toHours(diff))
        }
        return context.getString(R.string.relative_time_dd, TimeUnit.MILLISECONDS.toDays(diff))
    }

    fun size(size: Long): String {
        return if (size < 1024 * 1024) {
            String.format("%.1f K", (size / 1024.0))
        } else {
            String.format("%.1f M", (size / 1024.0 / 1024.0))
        }
    }
}