package androidx.core.app

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager

object ActivityCompat {
    fun checkSelfPermission(context: Context, permission: String): Int =
        PackageManager.PERMISSION_GRANTED
}

class NotificationChannelCompat private constructor(
    val id: String,
    val importance: Int,
) {
    class Builder(private val id: String, private val importance: Int) {
        fun setName(name: CharSequence?): Builder = this
        fun setDescription(description: String?): Builder = this
        fun build(): NotificationChannelCompat = NotificationChannelCompat(id, importance)
    }
}

object NotificationCompat {
    const val PRIORITY_DEFAULT = 0

    class Builder(context: Context, channelId: String) {
        fun setSmallIcon(icon: Int): Builder = this
        fun setContentTitle(title: CharSequence?): Builder = this
        fun setContentText(text: CharSequence?): Builder = this
        fun setPriority(pri: Int): Builder = this
        fun setContentIntent(intent: PendingIntent?): Builder = this
        fun setAutoCancel(autoCancel: Boolean): Builder = this
        fun build(): Notification = Notification()
    }
}

class NotificationManagerCompat private constructor() {
    fun cancelAll() {}
    fun createNotificationChannel(channel: NotificationChannelCompat) {}
    fun notify(id: Int, notification: Notification) {}
    fun areNotificationsEnabled(): Boolean = true

    companion object {
        const val IMPORTANCE_DEFAULT = 3
        private val INSTANCE = NotificationManagerCompat()
        fun from(context: Context): NotificationManagerCompat = INSTANCE
    }
}
