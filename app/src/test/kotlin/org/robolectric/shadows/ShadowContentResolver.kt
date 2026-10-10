package org.robolectric.shadows

import android.content.ContentProvider
import android.content.ContentResolver
import android.net.Uri
import java.io.InputStream
import java.io.OutputStream

class ShadowContentResolver(private val resolver: ContentResolver) {
    fun registerInputStream(uri: Uri, stream: InputStream) {
        resolver.registerInputStream(uri, stream)
    }

    fun registerOutputStream(uri: Uri, stream: OutputStream) {
        resolver.registerOutputStream(uri, stream)
    }

    companion object {
        @JvmStatic
        fun registerProviderInternal(authority: String, provider: ContentProvider) {
            ContentResolver.registerProvider(authority, provider)
        }
    }
}
