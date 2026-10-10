package android.app

import android.content.Context
import android.content.Intent

open class Application : Context() {
    open fun onCreate() {}
}

class Notification

class PendingIntent private constructor(
    val requestCode: Int,
    val intent: Intent,
) {
    fun cancel() {
        remove(this)
    }

    override fun equals(other: Any?): Boolean =
        other is PendingIntent && other.requestCode == requestCode

    override fun hashCode(): Int = requestCode

    companion object {
        const val FLAG_UPDATE_CURRENT = 0x08000000
        const val FLAG_NO_CREATE = 0x20000000
        const val FLAG_IMMUTABLE = 0x04000000

        private val activeBroadcasts = mutableMapOf<Int, PendingIntent>()

        fun getActivity(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent {
            return PendingIntent(requestCode, intent)
        }

        fun getBroadcast(context: Context, requestCode: Int, intent: Intent, flags: Int): PendingIntent {
            if ((flags and FLAG_NO_CREATE) != 0) {
                return activeBroadcasts[requestCode] ?: PendingIntent(requestCode, intent)
            }
            val pi = PendingIntent(requestCode, intent)
            activeBroadcasts[requestCode] = pi
            return pi
        }

        internal fun remove(pi: PendingIntent) {
            activeBroadcasts.remove(pi.requestCode)
        }
    }
}

class AlarmManager {
    val scheduledAlarms = mutableListOf<PendingIntent>()

    fun setExactAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent?) {
        if (operation != null) {
            scheduledAlarms.removeAll { it.requestCode == operation.requestCode }
            scheduledAlarms.add(operation)
        }
    }

    fun setAndAllowWhileIdle(type: Int, triggerAtMillis: Long, operation: PendingIntent?) {
        if (operation != null) {
            scheduledAlarms.removeAll { it.requestCode == operation.requestCode }
            scheduledAlarms.add(operation)
        }
    }

    fun cancel(operation: PendingIntent?) {
        if (operation != null) {
            scheduledAlarms.removeAll { it.requestCode == operation.requestCode }
            PendingIntent.remove(operation)
        }
    }

    companion object {
        const val RTC_WAKEUP = 0
        const val INTERVAL_HOUR = 3_600_000L
        const val INTERVAL_DAY = 86_400_000L
    }
}
