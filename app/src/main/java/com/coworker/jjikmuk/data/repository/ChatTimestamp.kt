package com.coworker.jjikmuk.data.repository

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** Server chat dates without an offset are UTC. Uses APIs available on minSdk 24. */
internal fun parseChatTimestamp(value: String): Long {
    val match = Regex("""^(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?Z?$""")
        .matchEntire(value) ?: error("Invalid chat timestamp")
    val date = match.groupValues[1]
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.ROOT).apply {
        timeZone = TimeZone.getTimeZone("UTC")
        isLenient = false
    }
    val position = ParsePosition(0)
    val seconds = requireNotNull(parser.parse(date, position)).time
    require(position.index == date.length)
    val millis = match.groupValues[2].padEnd(3, '0').take(3).toLong()
    return seconds + millis
}
