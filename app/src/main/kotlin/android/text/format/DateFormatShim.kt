package android.text.format

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object DateFormat {
    fun getTimeFormat(context: Context): java.text.DateFormat {
        val locale = context.resources.configuration.getLocale()
        return java.text.DateFormat.getTimeInstance(java.text.DateFormat.SHORT, locale)
    }

    fun getDateFormat(context: Context): java.text.DateFormat {
        val locale = context.resources.configuration.getLocale()
        return java.text.DateFormat.getDateInstance(java.text.DateFormat.SHORT, locale)
    }
}

object DateUtils {
    const val MINUTE_IN_MILLIS = 60_000L
    const val HOUR_IN_MILLIS = 3_600_000L
    const val DAY_IN_MILLIS = 86_400_000L

    fun getRelativeTimeSpanString(
        time: Long,
        now: Long = System.currentTimeMillis(),
        minResolution: Long = MINUTE_IN_MILLIS,
    ): CharSequence {
        val diff = now - time
        if (diff < MINUTE_IN_MILLIS) return "Just now"
        val mins = diff / MINUTE_IN_MILLIS
        if (mins < 60) return "${mins}m ago"
        val hours = mins / 60
        if (hours < 24) return "${hours}h ago"
        val days = hours / 24
        return if (days < 7) "${days}d ago" else SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(time))
    }
}
