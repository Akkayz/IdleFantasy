package android.provider

import android.content.ContentResolver
import android.net.Uri
import android.os.Bundle
import java.io.File

object Settings {
    const val ACTION_APP_NOTIFICATION_SETTINGS = "android.settings.APP_NOTIFICATION_SETTINGS"
    const val EXTRA_APP_PACKAGE = "android.provider.extra.APP_PACKAGE"

    object Global {
        const val BOOT_COUNT = "boot_count"

        private val values = mutableMapOf<String, Int>(BOOT_COUNT to 1)

        fun getInt(cr: ContentResolver, name: String): Int {
            return values[name] ?: throw RuntimeException("Setting not found: $name")
        }

        fun putInt(cr: ContentResolver, name: String, value: Int): Boolean {
            values[name] = value
            return true
        }
    }
}

object DocumentsContract {
    object Document {
        const val COLUMN_DOCUMENT_ID = "document_id"
        const val COLUMN_DISPLAY_NAME = "_display_name"
    }

    private const val METHOD_CREATE = "android:createDocument"
    private const val METHOD_DELETE = "android:deleteDocument"
    private const val METHOD_RENAME = "android:renameDocument"
    private const val KEY_URI = "uri"
    private const val KEY_DISPLAY_NAME = "_display_name"

    fun buildTreeDocumentUri(authority: String, treeDocId: String): Uri {
        val encoded = Uri.encodeSegment(treeDocId)
        return Uri.parse("content://$authority/tree/$encoded")
    }

    fun buildDocumentUri(authority: String, documentId: String): Uri {
        val encoded = Uri.encodeSegment(documentId)
        return Uri.parse("content://$authority/document/$encoded")
    }

    fun buildDocumentUriUsingTree(treeUri: Uri, documentId: String): Uri {
        val auth = treeUri.authority ?: "local.filesystem"
        val treeId = Uri.encodeSegment(getTreeDocumentId(treeUri))
        val docId = Uri.encodeSegment(documentId)
        return Uri.parse("content://$auth/tree/$treeId/document/$docId")
    }

    fun buildChildDocumentsUriUsingTree(treeUri: Uri, parentDocumentId: String): Uri {
        val auth = treeUri.authority ?: "local.filesystem"
        val treeId = Uri.encodeSegment(getTreeDocumentId(treeUri))
        val parentId = Uri.encodeSegment(parentDocumentId)
        return Uri.parse("content://$auth/tree/$treeId/document/$parentId/children")
    }

    fun getTreeDocumentId(documentUri: Uri): String {
        val s = documentUri.toString()
        if (s.contains("/tree/")) {
            val part = s.substringAfter("/tree/").substringBefore('/')
            return Uri.decodeSegment(part)
        }
        return documentUri.toFileOrNull()?.absolutePath ?: s
    }

    fun getDocumentId(documentUri: Uri): String {
        extractDocIdOrNull(documentUri)?.let { return it }
        return documentUri.toFileOrNull()?.absolutePath ?: documentUri.toString()
    }

    internal fun extractDocIdOrNull(uri: Uri): String? {
        val s = uri.toString()
        if (s.contains("/document/")) {
            val part = s.substringAfter("/document/").substringBefore('/')
            return Uri.decodeSegment(part)
        }
        if (s.contains("/tree/")) {
            val part = s.substringAfter("/tree/").substringBefore('/')
            return Uri.decodeSegment(part)
        }
        return null
    }

    fun createDocument(
        content: ContentResolver,
        parentDocumentUri: Uri,
        mimeType: String,
        displayName: String,
    ): Uri? {
        val auth = parentDocumentUri.authority
        if (auth != null && ContentResolver.registeredProviders.containsKey(auth)) {
            val extras = Bundle().apply {
                putParcelable(KEY_URI, parentDocumentUri)
                putString(KEY_DISPLAY_NAME, displayName)
            }
            val out = content.call(auth, METHOD_CREATE, null, extras)
            return out?.getParcelable(KEY_URI)
        }
        val parentDir = parentDocumentUri.toFileOrNull() ?: return null
        parentDir.mkdirs()
        val safeName = File(displayName).name
        if (safeName.isEmpty() || safeName == "." || safeName == ".." || safeName != displayName) return null
        val fileName = if (mimeType == "application/json" && !safeName.endsWith(".json") && !safeName.endsWith(".tmp")) {
            "$safeName.json"
        } else {
            safeName
        }
        val target = File(parentDir, fileName)
        try {
            if (!target.canonicalFile.toPath().startsWith(parentDir.canonicalFile.toPath())) return null
            if (!target.exists()) target.createNewFile()
        } catch (_: Exception) {
            return null
        }
        return buildDocumentUriUsingTree(parentDocumentUri, target.absolutePath)
    }

    fun renameDocument(
        content: ContentResolver,
        documentUri: Uri,
        displayName: String,
    ): Uri? {
        val auth = documentUri.authority
        if (auth != null && ContentResolver.registeredProviders.containsKey(auth)) {
            val extras = Bundle().apply {
                putParcelable(KEY_URI, documentUri)
                putString(KEY_DISPLAY_NAME, displayName)
            }
            val out = content.call(auth, METHOD_RENAME, displayName, extras)
            return out?.getParcelable(KEY_URI)
        }
        val srcFile = documentUri.toFileOrNull() ?: return null
        val parentDir = srcFile.parentFile ?: return null
        val safeName = File(displayName).name
        if (safeName.isEmpty() || safeName == "." || safeName == ".." || safeName != displayName) return null
        val dstName = if (!safeName.endsWith(".json")) "$safeName.json" else safeName
        val dstFile = File(parentDir, dstName)
        try {
            if (!dstFile.canonicalFile.toPath().startsWith(parentDir.canonicalFile.toPath())) return null
        } catch (_: Exception) {
            return null
        }
        if (dstFile.exists()) dstFile.delete()
        if (!srcFile.renameTo(dstFile)) return null
        return buildDocumentUriUsingTree(documentUri, dstFile.absolutePath)
    }

    fun deleteDocument(content: ContentResolver, documentUri: Uri): Boolean {
        val auth = documentUri.authority
        if (auth != null && ContentResolver.registeredProviders.containsKey(auth)) {
            val extras = Bundle().apply {
                putParcelable(KEY_URI, documentUri)
            }
            content.call(auth, METHOD_DELETE, null, extras)
            return true
        }
        val file = documentUri.toFileOrNull() ?: return false
        return !file.exists() || file.delete()
    }
}
