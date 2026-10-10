package androidx.test.core.app

import android.app.AlarmManager
import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import java.nio.file.Files
import java.util.Locale

object ApplicationProvider {
    internal val currentQualifier = ThreadLocal<String?>()

    @Suppress("UNCHECKED_CAST")
    @JvmStatic
    fun <T : Context> getApplicationContext(): T {
        val tempDir = Files.createTempDirectory("idlefantasy_test_").toFile().apply {
            deleteOnExit()
        }
        val qualifier = currentQualifier.get()
        val customRes = if (!qualifier.isNullOrBlank()) {
            val config = Configuration().apply {
                setLocale(Locale.forLanguageTag(qualifier))
            }
            Resources(config)
        } else {
            null
        }
        val ctx = Context(customResources = customRes, customFilesDir = tempDir)
        (ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.scheduledAlarms?.clear()
        ctx.contentResolver.clearRegisteredStreams()
        return ctx as T
    }
}
