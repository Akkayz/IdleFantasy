package org.robolectric

import android.app.AlarmManager
import android.content.ContentResolver
import android.os.Looper
import org.robolectric.shadows.ShadowContentResolver

object Shadows {
    @JvmStatic
    fun shadowOf(alarmManager: AlarmManager): AlarmManager = alarmManager

    @JvmStatic
    fun shadowOf(contentResolver: ContentResolver): ShadowContentResolver =
        ShadowContentResolver(contentResolver)

    @JvmStatic
    fun shadowOf(looper: Looper): Looper = looper
}
