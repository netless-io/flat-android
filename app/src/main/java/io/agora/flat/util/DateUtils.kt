package io.agora.flat.util

import java.util.*

object DateUtils {
    fun isToday(utc: Long): Boolean {
        val c1 = Calendar.getInstance();

        val c2 = Calendar.getInstance();
        c2.time = Date(utc)

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isTomorrow(utc: Long): Boolean {
        val c1 = Calendar.getInstance();
        c1.add(Calendar.DAY_OF_YEAR, 1)

        val c2 = Calendar.getInstance();
        c2.time = Date(utc)

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isYesterday(utc: Long): Boolean {
        val c1 = Calendar.getInstance();
        c1.add(Calendar.DAY_OF_YEAR, -1)

        val c2 = Calendar.getInstance();
        c2.time = Date(utc)

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    fun isThisYear(utc: Long): Boolean {
        val c1 = Calendar.getInstance();

        val c2 = Calendar.getInstance();
        c2.time = Date(utc)

        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
    }
}