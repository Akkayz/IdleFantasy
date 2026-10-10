package androidx.core.os

import java.util.Locale

class LocaleListCompat private constructor(private val locales: List<Locale>) {
    val isEmpty: Boolean get() = locales.isEmpty()

    operator fun get(index: Int): Locale? = locales.getOrNull(index)

    fun toLanguageTags(): String = locales.joinToString(",") { it.toLanguageTag() }

    companion object {
        private val EMPTY = LocaleListCompat(emptyList())

        fun getEmptyLocaleList(): LocaleListCompat = EMPTY

        fun forLanguageTags(list: String?): LocaleListCompat {
            if (list.isNullOrBlank()) return EMPTY
            val parsed = list.split(',')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { Locale.forLanguageTag(it) }
            return if (parsed.isEmpty()) EMPTY else LocaleListCompat(parsed)
        }
    }
}
