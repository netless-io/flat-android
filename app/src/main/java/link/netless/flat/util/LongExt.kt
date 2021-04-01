package link.netless.flat.util

import java.text.SimpleDateFormat

fun Long.formatToMMDD(): String {
    val simpleDateFormat = SimpleDateFormat("MM-dd")
    return simpleDateFormat.format(this)
}

fun Long.formatToMMDDWeek(): String {
    val simpleDateFormat = SimpleDateFormat("MM月dd日")
    return simpleDateFormat.format(this)
}

fun Long.formatToHHmm(): String {
    val simpleDateFormat = SimpleDateFormat("HH:mm")
    return simpleDateFormat.format(this)
}