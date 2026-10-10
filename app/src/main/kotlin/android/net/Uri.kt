package android.net

import java.io.File
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder

data class Uri(private val raw: String) {
    val scheme: String?
        get() = raw.substringBefore("://", "").takeIf { it.isNotEmpty() }

    val authority: String?
        get() {
            val afterScheme = raw.substringAfter("://", "")
            if (afterScheme.isEmpty()) return null
            return afterScheme.substringBefore('/').takeIf { it.isNotEmpty() }
        }

    val path: String?
        get() {
            if (raw.startsWith("file://")) {
                return try {
                    File(URI(raw)).absolutePath
                } catch (_: Exception) {
                    URLDecoder.decode(raw.removePrefix("file://"), Charsets.UTF_8)
                }
            }
            val afterScheme = raw.substringAfter("://", "")
            if (afterScheme.isNotEmpty()) {
                val p = afterScheme.substringAfter('/', "")
                return if (p.isEmpty()) "/" else "/$p"
            }
            return raw
        }

    fun toFileOrNull(): File? {
        return when {
            raw.startsWith("file://") -> try {
                File(URI(raw))
            } catch (_: Exception) {
                File(raw.removePrefix("file://"))
            }
            raw.startsWith("content://") -> {
                val docId = android.provider.DocumentsContract.extractDocIdOrNull(this) ?: return null
                File(docId).takeIf { it.isAbsolute }
            }
            else -> File(raw)
        }
    }

    override fun toString(): String = raw

    companion object {
        fun parse(uriString: String): Uri = Uri(uriString)

        fun fromFile(file: File): Uri = Uri(file.toURI().toString())

        internal fun encodeSegment(s: String): String =
            URLEncoder.encode(s, Charsets.UTF_8).replace("+", "%20")

        internal fun decodeSegment(s: String): String =
            URLDecoder.decode(s, Charsets.UTF_8)
    }
}
