package io.agora.flat.util

import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

object FlatDataTimeFormatter {
    private val timeFormatter = SimpleDateFormat("HH:mm")

    private val dateFormatter = SimpleDateFormat("MM/dd")
    private val longDateFormatter = SimpleDateFormat("yyyy/MM/dd")

    private val longDateWithWeekFormatter = SimpleDateFormat("yyyy/MM/dd EE", Locale.CHINA)

    fun time(utcMs: Long): String {
        return timeFormatter.format(utcMs)
    }

    fun date(utcMs: Long): String {
        return dateFormatter.format(utcMs)
    }

    fun formatLongDate(utcMs: Long): String {
        return longDateFormatter.format(utcMs)
    }

    fun longDateWithWeek(utcMs: Long): String {
        return longDateWithWeekFormatter.format(utcMs)
    }

    fun timeDuring(begin: Long, end: Long): String {
        return "${timeFormatter.format(begin)}~${timeFormatter.format(end)}"
    }

    // TODO 国际化支持
    fun diffTime(begin: Long, end: Long): String {
        val diff = end - begin
        if (diff < 0) {
            throw RuntimeException("end need large than begin")
        }
        // 显示分钟
        if (diff < 3600_000) {
            return "${TimeUnit.MILLISECONDS.toMinutes(diff)}分"
        }
        if (diff < 86400_000) {
            return "${TimeUnit.MILLISECONDS.toHours(diff)}小时"
        }
        return "${TimeUnit.MILLISECONDS.toDays(diff)}天"
    }
}