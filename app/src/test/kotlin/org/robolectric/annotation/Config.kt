package org.robolectric.annotation

@Retention(AnnotationRetention.RUNTIME)
annotation class Config(
    val manifest: String = "",
    val sdk: IntArray = [],
    val qualifiers: String = ""
) {
    companion object {
        const val NONE: String = "--none"
    }
}
