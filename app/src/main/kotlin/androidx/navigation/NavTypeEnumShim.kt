package androidx.navigation

import androidx.core.bundle.Bundle

@Suppress("FunctionName")
fun <D : Enum<*>> NavType.Companion.EnumType(type: Class<D>): NavType<D> {
    return object : NavType<D>(isNullableAllowed = false) {
        override val name: String
            get() = type.name

        override fun get(bundle: Bundle, key: String): D? {
            val str = StringType[bundle, key] ?: return null
            return parseValue(str)
        }

        override fun parseValue(value: String): D {
            return type.enumConstants.first { it.name.equals(value, ignoreCase = true) }
        }

        override fun put(bundle: Bundle, key: String, value: D) {
            StringType.put(bundle, key, value.name)
        }
    }
}
