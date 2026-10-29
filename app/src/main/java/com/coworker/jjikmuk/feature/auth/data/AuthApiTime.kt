package com.coworker.jjikmuk.feature.auth.data

import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** API 24-compatible offset timestamp parser. Never assume a timezone for a local datetime. */
internal object AuthApiTime {
    private val offsetTimestamp = Regex("""(\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2})(?:\.(\d{1,9}))?(Z|[+-]\d{2}:\d{2})""")

    fun parseMillis(value: String): Long {
        val match = requireNotNull(offsetTimestamp.matchEntire(value)) { "Missing or invalid timestamp offset" }
        val zone = match.groupValues[3]
        if (zone != "Z") {
            val hours = zone.substring(1, 3).toInt()
            val minutes = zone.substring(4, 6).toInt()
            require(hours in 0..18 && minutes in 0..59 && (hours != 18 || minutes == 0))
        }
        val base = match.groupValues[1] + zone
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val position = ParsePosition(0)
        val date = requireNotNull(format.parse(base, position))
        require(position.index == base.length)
        val millis = match.groupValues[2].padEnd(3, '0').take(3).toInt()
        return Math.addExact(date.time, millis.toLong())
    }

    fun retryAfterSeconds(value: String?, receivedAtMillis: Long): Long? {
        if (value == null) return null
        val text = value.trim()
        if (text.matches(Regex("[0-9]+"))) return text.toLongOrNull()
        val format = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US).apply {
            isLenient = false
            timeZone = TimeZone.getTimeZone("GMT")
        }
        val position = ParsePosition(0)
        val date = format.parse(text, position) ?: return null
        if (position.index != text.length) return null
        val difference = date.time - receivedAtMillis
        return if (difference <= 0) 0 else difference / 1000 + if (difference % 1000 > 0) 1 else 0
    }
}
