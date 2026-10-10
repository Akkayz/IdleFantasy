package androidx.hilt.navigation.compose

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import com.fantasyidler.data.db.AppDatabase
import com.fantasyidler.data.db.dao.ArenaRecordDao
import com.fantasyidler.data.db.dao.CustomThemeDao
import com.fantasyidler.data.db.dao.FarmingPatchDao
import com.fantasyidler.data.db.dao.GlobalStateDao
import com.fantasyidler.data.db.dao.PlayerDao
import com.fantasyidler.data.db.dao.QuestProgressDao
import com.fantasyidler.data.db.dao.SkillSessionDao
import com.fantasyidler.di.AppModule
import com.fantasyidler.di.DatabaseModule
import kotlinx.serialization.json.Json
import java.lang.reflect.ParameterizedType
import javax.inject.Inject
import javax.inject.Provider
import javax.inject.Singleton

object DesktopInjector {
    val context: Context get() = Context.DesktopInstance
    val json: Json by lazy { AppModule.provideJson() }
    val database: AppDatabase by lazy { DatabaseModule.provideDatabase(context) }

    private val singletons = mutableMapOf<Class<*>, Any>()

    @Suppress("UNCHECKED_CAST")
    @Synchronized
    fun <T : Any> get(clazz: Class<T>): T {
        when (clazz) {
            Context::class.java -> return context as T
            Json::class.java -> return json as T
            AppDatabase::class.java -> return database as T
            PlayerDao::class.java -> return DatabaseModule.providePlayerDao(database) as T
            SkillSessionDao::class.java -> return DatabaseModule.provideSkillSessionDao(database) as T
            QuestProgressDao::class.java -> return DatabaseModule.provideQuestProgressDao(database) as T
            FarmingPatchDao::class.java -> return DatabaseModule.provideFarmingPatchDao(database) as T
            GlobalStateDao::class.java -> return DatabaseModule.provideGlobalStateDao(database) as T
            ArenaRecordDao::class.java -> return DatabaseModule.provideArenaRecordDao(database) as T
            CustomThemeDao::class.java -> return DatabaseModule.provideCustomThemeDao(database) as T
        }

        singletons[clazz]?.let { return it as T }

        val instance = createInstance(clazz, null)
        if (clazz.isAnnotationPresent(Singleton::class.java)) {
            singletons[clazz] = instance
        }
        return instance
    }

    fun <VM : ViewModel> createViewModel(clazz: Class<VM>, savedStateHandle: SavedStateHandle?): VM {
        return createInstance(clazz, savedStateHandle ?: SavedStateHandle())
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T : Any> createInstance(clazz: Class<T>, savedStateHandle: SavedStateHandle?): T {
        val constructors = clazz.declaredConstructors
        val constructor = constructors.firstOrNull { it.isAnnotationPresent(Inject::class.java) }
            ?: constructors.firstOrNull()
            ?: throw IllegalStateException("No constructor found for ${clazz.name}")
        constructor.isAccessible = true

        val paramTypes = constructor.parameterTypes
        val genericTypes = constructor.genericParameterTypes
        val args = Array<Any?>(paramTypes.size) { i ->
            val pType = paramTypes[i]
            when {
                pType == SavedStateHandle::class.java -> savedStateHandle ?: SavedStateHandle()
                pType == Provider::class.java -> {
                    val gType = genericTypes[i] as ParameterizedType
                    val targetClass = gType.actualTypeArguments[0] as Class<Any>
                    Provider { get(targetClass) }
                }
                else -> get(pType as Class<Any>)
            }
        }
        return constructor.newInstance(*args) as T
    }
}

@Composable
inline fun <reified VM : ViewModel> hiltViewModel(): VM {
    val owner = LocalViewModelStoreOwner.current
    val navEntry = owner as? NavBackStackEntry
    return if (owner != null) {
        viewModel<VM>(
            viewModelStoreOwner = owner,
            initializer = {
                val handle = try {
                    navEntry?.savedStateHandle ?: SavedStateHandle()
                } catch (_: Exception) {
                    SavedStateHandle()
                }
                val args = navEntry?.arguments
                if (args != null) {
                    for (key in args.keySet()) {
                        if (key == null) continue
                        @Suppress("DEPRECATION")
                        val v = args.get(key)
                        if (v != null && handle.get<Any>(key) == null) {
                            handle[key] = v
                        }
                    }
                }
                DesktopInjector.createViewModel(VM::class.java, handle)
            }
        )
    } else {
        remember { DesktopInjector.createViewModel(VM::class.java, SavedStateHandle()) }
    }
}
