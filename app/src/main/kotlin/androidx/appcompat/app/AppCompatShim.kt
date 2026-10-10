package androidx.appcompat.app

import android.content.Context
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultLauncher
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.Locale

open class AppCompatActivity : Context(), LifecycleOwner {
    private val lifecycleRegistry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = lifecycleRegistry

    var intent: android.content.Intent = android.content.Intent()
        private set

    fun setIntent(newIntent: android.content.Intent) {
        intent = newIntent
    }

    fun <I, O> registerForActivityResult(
        contract: ActivityResultContract<I, O>,
        callback: (O) -> Unit
    ): ActivityResultLauncher<I> {
        return ActivityResultLauncher { input ->
            callback(contract.invokeDesktop(input))
        }
    }

    protected open fun onCreate(savedInstanceState: Bundle?) {}
    protected open fun onStart() {}
    protected open fun onStop() {}
    protected open fun onNewIntent(intent: android.content.Intent) {}
}

object AppCompatDelegate {
    private val localeFile: File
        get() = File(Context.DesktopInstance.filesDir, "locale.txt")

    private val _localeFlow = MutableStateFlow(loadInitialLocaleList())
    val localeFlow: StateFlow<LocaleListCompat> = _localeFlow.asStateFlow()

    private fun loadInitialLocaleList(): LocaleListCompat {
        return try {
            if (localeFile.isFile) {
                val tag = localeFile.readText().trim()
                if (tag.isNotEmpty() && tag != "system") {
                    val list = LocaleListCompat.forLanguageTags(tag)
                    list[0]?.let { Context.DesktopInstance.resources.configuration.setLocale(it) }
                    list
                } else {
                    LocaleListCompat.getEmptyLocaleList()
                }
            } else {
                LocaleListCompat.getEmptyLocaleList()
            }
        } catch (_: Exception) {
            LocaleListCompat.getEmptyLocaleList()
        }
    }

    fun getApplicationLocales(): LocaleListCompat = _localeFlow.value

    fun setApplicationLocales(locales: LocaleListCompat) {
        _localeFlow.value = locales
        try {
            val tag = locales.toLanguageTags()
            localeFile.parentFile?.mkdirs()
            localeFile.writeText(if (tag.isEmpty()) "system" else tag)
            val resolved = locales[0] ?: Locale.getDefault()
            Context.DesktopInstance.resources.configuration.setLocale(resolved)
        } catch (_: Exception) {
        }
    }
}
