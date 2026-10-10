package android.content

import android.content.pm.ProviderInfo
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.ConcurrentHashMap

class ContentValues

abstract class ContentProvider {
    var context: Context? = null
        private set

    open fun attachInfo(context: Context?, info: ProviderInfo?) {
        this.context = context
        onCreate()
    }

    abstract fun onCreate(): Boolean

    open fun call(method: String, arg: String?, extras: Bundle?): Bundle? = null

    abstract fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?,
    ): Cursor?

    abstract fun getType(uri: Uri): String?
    abstract fun insert(uri: Uri, values: ContentValues?): Uri?
    abstract fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int
    abstract fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?): Int
}

class ContentResolver {
    internal val registeredInputStreams = ConcurrentHashMap<Uri, InputStream>()
    internal val registeredOutputStreams = ConcurrentHashMap<Uri, OutputStream>()

    fun takePersistableUriPermission(uri: Uri, modeFlags: Int) {}

    fun releasePersistableUriPermission(uri: Uri, modeFlags: Int) {}

    fun registerInputStream(uri: Uri, stream: InputStream) {
        registeredInputStreams[uri] = stream
    }

    fun registerOutputStream(uri: Uri, stream: OutputStream) {
        registeredOutputStreams[uri] = stream
    }

    fun clearRegisteredStreams() {
        registeredInputStreams.clear()
        registeredOutputStreams.clear()
    }

    fun openInputStream(uri: Uri): InputStream? {
        registeredInputStreams[uri]?.let { return it }
        val file = uri.toFileOrNull() ?: return null
        return if (file.isFile) FileInputStream(file) else null
    }

    fun openOutputStream(uri: Uri, mode: String = "w"): OutputStream? {
        registeredOutputStreams[uri]?.let { return it }
        val file = uri.toFileOrNull() ?: return null
        file.parentFile?.mkdirs()
        return FileOutputStream(file, false)
    }

    fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? {
        val provider = registeredProviders[uri.authority]
        if (provider != null) {
            return provider.query(uri, projection, selection, selectionArgs, sortOrder)
        }
        val dir = uri.toFileOrNull() ?: return null
        val cols = projection ?: arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
        )
        val cursor = MatrixCursor(cols)
        if (dir.isDirectory) {
            dir.listFiles()?.forEach { child ->
                cursor.addRow(
                    cols.map { col ->
                        when (col) {
                            DocumentsContract.Document.COLUMN_DOCUMENT_ID -> child.absolutePath
                            DocumentsContract.Document.COLUMN_DISPLAY_NAME -> child.name
                            else -> null
                        }
                    }
                )
            }
        }
        return cursor
    }

    fun call(authority: String, method: String, arg: String?, extras: Bundle?): Bundle? {
        val provider = registeredProviders[authority]
        return provider?.call(method, arg, extras)
    }

    companion object {
        internal val registeredProviders = ConcurrentHashMap<String, ContentProvider>()

        fun registerProvider(authority: String, provider: ContentProvider) {
            registeredProviders[authority] = provider
        }
    }
}
