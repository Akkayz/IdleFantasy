package dagger.hilt

import kotlin.reflect.KClass

@Retention(AnnotationRetention.RUNTIME)
annotation class InstallIn(vararg val value: KClass<*>)
