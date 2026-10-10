package android.content

import android.app.AlarmManager
import android.content.res.AssetManager
import android.content.res.Configuration
import android.content.res.Resources
import android.net.Uri
import android.os.PowerManager
import java.awt.Desktop
import java.io.File
import java.net.URI

open class Context(
    private val customResources: Resources? = null,
    private val customFilesDir: File? = null,
) {
    open val packageName: String = "com.fantasyidler"

    open val filesDir: File
        get() {
            if (customFilesDir != null) {
                if (!customFilesDir.exists()) customFilesDir.mkdirs()
                return customFilesDir
            }
            return resolveDefaultFilesDir()
        }

    open val cacheDir: File
        get() = File(filesDir, "cache").apply { if (!exists()) mkdirs() }

    open val assets: AssetManager = AssetManagerInstance

    open val resources: Resources
        get() = customResources ?: DefaultResources

    open val contentResolver: ContentResolver = ContentResolverInstance

    fun getString(resId: Int): String = resources.getString(resId)

    fun getString(resId: Int, vararg formatArgs: Any?): String =
        resources.getString(resId, *formatArgs)

    fun createConfigurationContext(overrideConfiguration: Configuration): Context {
        return Context(Resources(overrideConfiguration), customFilesDir)
    }

    open fun getSystemService(name: String): Any? = when (name) {
        ALARM_SERVICE -> AlarmManagerInstance
        POWER_SERVICE -> PowerManagerInstance
        else -> null
    }

    open fun <T> getSystemService(serviceClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return when (serviceClass) {
            AlarmManager::class.java -> AlarmManagerInstance as T
            PowerManager::class.java -> PowerManagerInstance as T
            else -> throw IllegalArgumentException("Unsupported system service: $serviceClass")
        }
    }

    open fun startActivity(intent: Intent) {
        try {
            when (intent.action) {
                Intent.ACTION_VIEW -> {
                    val url = intent.data?.toString() ?: return
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().browse(URI(url))
                    }
                }
                Intent.ACTION_SEND, Intent.ACTION_CHOOSER -> {
                    val target = (intent.extras[Intent.EXTRA_INTENT] as? Intent) ?: intent
                    val streamUri = target.extras[Intent.EXTRA_STREAM] as? Uri
                    val file = streamUri?.toFileOrNull()
                    if (file != null && file.exists() && Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    companion object {
        const val ALARM_SERVICE = "alarm"
        const val POWER_SERVICE = "power"

        private val AssetManagerInstance = AssetManager()
        private val DefaultResources = Resources()
        private val ContentResolverInstance = ContentResolver()
        private val AlarmManagerInstance = AlarmManager()
        private val PowerManagerInstance = PowerManager()

        val DesktopInstance: Context = Context()

        internal fun resolveDefaultFilesDir(): File {
            val customDir = System.getProperty("idlefantasy.dataDir")
            if (!customDir.isNullOrBlank()) {
                return File(customDir).apply { if (!exists()) mkdirs() }
            }
            val os = System.getProperty("os.name").orEmpty().lowercase()
            val userHome = System.getProperty("user.home").orEmpty()
            val baseDir = when {
                os.contains("win") -> {
                    val appData = System.getenv("APPDATA")
                    if (!appData.isNullOrBlank()) File(appData, "IdleFantasy")
                    else File(userHome, "AppData/Roaming/IdleFantasy")
                }
                os.contains("mac") -> File(userHome, "Library/Application Support/IdleFantasy")
                else -> {
                    val xdg = System.getenv("XDG_DATA_HOME")
                    if (!xdg.isNullOrBlank()) File(xdg, "IdleFantasy")
                    else File(userHome, ".local/share/IdleFantasy")
                }
            }
            val dir = File(baseDir, "data")
            if (!dir.exists()) dir.mkdirs()
            return dir
        }
    }
}

class Intent {
    var action: String? = null
    var data: Uri? = null
    var type: String? = null
    var flags: Int = 0
    val extras = mutableMapOf<String, Any?>()

    constructor()
    constructor(action: String) {
        this.action = action
    }
    constructor(action: String, uri: Uri?) {
        this.action = action
        this.data = uri
    }
    constructor(packageContext: Context, cls: Class<*>)

    fun putExtra(name: String, value: Any?): Intent {
        extras[name] = value
        return this
    }

    fun getStringExtra(name: String): String? = extras[name] as? String

    fun getIntExtra(name: String, defaultValue: Int): Int =
        (extras[name] as? Number)?.toInt() ?: defaultValue

    fun addFlags(flags: Int): Intent {
        this.flags = this.flags or flags
        return this
    }

    companion object {
        const val ACTION_VIEW = "android.intent.action.VIEW"
        const val ACTION_SEND = "android.intent.action.SEND"
        const val ACTION_CHOOSER = "android.intent.action.CHOOSER"
        const val ACTION_BOOT_COMPLETED = "android.intent.action.BOOT_COMPLETED"
        const val ACTION_MY_PACKAGE_REPLACED = "android.intent.action.MY_PACKAGE_REPLACED"

        const val EXTRA_STREAM = "android.intent.extra.STREAM"
        const val EXTRA_INTENT = "android.intent.extra.INTENT"

        const val FLAG_ACTIVITY_NEW_TASK = 0x10000000
        const val FLAG_ACTIVITY_CLEAR_TASK = 0x00008000
        const val FLAG_GRANT_READ_URI_PERMISSION = 0x00000001
        const val FLAG_GRANT_WRITE_URI_PERMISSION = 0x00000002

        fun createChooser(target: Intent, title: CharSequence?): Intent {
            return Intent(ACTION_CHOOSER).apply {
                putExtra(EXTRA_INTENT, target)
            }
        }
    }
}

abstract class BroadcastReceiver {
    class PendingResult {
        fun finish() {}
    }

    fun goAsync(): PendingResult = PendingResult()

    abstract fun onReceive(context: Context, intent: Intent)
}
