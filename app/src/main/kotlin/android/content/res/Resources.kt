package android.content.res

import android.util.Log
import com.fantasyidler.R
import java.io.File
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.JarURLConnection
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.xml.parsers.DocumentBuilderFactory

class LocaleList(private val list: List<Locale>) {
    val isEmpty: Boolean get() = list.isEmpty()
    operator fun get(index: Int): Locale? = list.getOrNull(index)
    fun toLanguageTags(): String = list.joinToString(",") { it.toLanguageTag() }
}

class Configuration(other: Configuration? = null) {
    var uiMode: Int = other?.uiMode ?: UI_MODE_NIGHT_YES
    private var currentLocale: Locale = other?.currentLocale ?: Locale.getDefault()

    val locales: LocaleList
        get() = LocaleList(listOf(currentLocale))

    fun setLocale(locale: Locale) {
        currentLocale = locale
    }

    fun getLocale(): Locale = currentLocale

    companion object {
        const val UI_MODE_NIGHT_MASK = 0x30
        const val UI_MODE_NIGHT_NO = 0x10
        const val UI_MODE_NIGHT_YES = 0x20
    }
}

class AssetManager {
    private val devAssetsDirs = listOf(
        File("app/src/main/assets"),
        File("src/main/assets"),
        File("assets"),
    )

    private fun resolveDevDir(): File? = devAssetsDirs.firstOrNull { it.isDirectory }

    fun open(fileName: String): InputStream {
        val clean = fileName.trimStart('/')
        resolveDevDir()?.resolve(clean)?.takeIf { it.isFile }?.let {
            return it.inputStream()
        }
        val cl = Thread.currentThread().contextClassLoader ?: AssetManager::class.java.classLoader
        return cl.getResourceAsStream(clean)
            ?: throw FileNotFoundException("Asset not found: $fileName")
    }

    fun list(path: String): Array<String>? {
        val clean = path.trim('/')
        resolveDevDir()?.resolve(clean)?.takeIf { it.isDirectory }?.let { dir ->
            return dir.list() ?: emptyArray()
        }
        val cl = Thread.currentThread().contextClassLoader ?: AssetManager::class.java.classLoader
        val url = cl.getResource(clean) ?: return emptyArray()
        return when (url.protocol) {
            "file" -> File(url.toURI()).list() ?: emptyArray()
            "jar" -> {
                val conn = url.openConnection() as? JarURLConnection ?: return emptyArray()
                val prefix = "$clean/"
                conn.jarFile.entries().asSequence()
                    .map { it.name }
                    .filter { it.startsWith(prefix) && it.length > prefix.length }
                    .map { it.removePrefix(prefix).substringBefore('/') }
                    .filter { it.isNotEmpty() }
                    .distinct()
                    .toList()
                    .toTypedArray()
            }
            else -> emptyArray()
        }
    }
}

class Resources(
    val configuration: Configuration = Configuration(),
) {
    fun getIdentifier(name: String, defType: String?, defPackage: String?): Int {
        return when (defType) {
            "string" -> R.stringNameToId[name] ?: 0
            "drawable" -> R.drawableNameToId[name] ?: 0
            else -> 0
        }
    }

    fun getString(id: Int): String {
        if (id == android.R.string.cancel) {
            val cancelId = R.stringNameToId["cancel"]
            if (cancelId != null) return getString(cancelId)
            return "Cancel"
        }
        val key = R.stringIdToName[id] ?: return ""
        val bundle = ResourceRepository.getBundle(configuration.getLocale())
        val fallback = ResourceRepository.getBundle(Locale.ENGLISH)
        return bundle.strings[key] ?: fallback.strings[key] ?: key
    }

    fun getString(id: Int, vararg formatArgs: Any?): String {
        val raw = getString(id)
        if (formatArgs.isEmpty()) return raw
        return try {
            String.format(configuration.getLocale(), raw, *formatArgs)
        } catch (_: Exception) {
            try {
                String.format(Locale.US, raw, *formatArgs)
            } catch (_: Exception) {
                raw
            }
        }
    }

    fun getQuantityString(id: Int, quantity: Int, vararg formatArgs: Any?): String {
        val key = R.pluralsIdToName[id] ?: return ""
        val bundle = ResourceRepository.getBundle(configuration.getLocale())
        val fallback = ResourceRepository.getBundle(Locale.ENGLISH)
        val quantities = bundle.plurals[key] ?: fallback.plurals[key] ?: return ""
        val rule = if (quantity == 1) "one" else "other"
        val template = quantities[rule] ?: quantities["other"] ?: quantities.values.firstOrNull() ?: ""
        return if (formatArgs.isEmpty()) template else try {
            String.format(configuration.getLocale(), template, *formatArgs)
        } catch (_: Exception) {
            template
        }
    }

    fun getStringArray(id: Int): Array<String> {
        val key = R.arrayIdToName[id] ?: return emptyArray()
        val bundle = ResourceRepository.getBundle(configuration.getLocale())
        val fallback = ResourceRepository.getBundle(Locale.ENGLISH)
        return bundle.arrays[key] ?: fallback.arrays[key] ?: emptyArray()
    }
}

internal class ParsedXmlBundle(
    val strings: Map<String, String>,
    val plurals: Map<String, Map<String, String>>,
    val arrays: Map<String, Array<String>>,
    val colors: Map<String, String>,
)

internal object ResourceRepository {
    private val cache = ConcurrentHashMap<String, ParsedXmlBundle>()

    private val devResDirs = listOf(
        File("app/src/main/res"),
        File("src/main/res"),
        File("../app/src/main/res"),
        File("res"),
    )

    private val drawableDirs = listOf(
        "drawable",
        "drawable-xxxhdpi",
        "drawable-xxhdpi",
        "drawable-xhdpi",
        "drawable-hdpi",
        "drawable-mdpi",
    )

    private val knownXmlFiles = listOf(
        "colors.xml",
        "strings.xml",
        "strings_enemies.xml",
        "strings_game.xml",
        "strings_guild_quests.xml",
        "strings_items.xml",
        "strings_notifications.xml",
        "strings_quests.xml",
        "strings_skills.xml",
        "strings_weekly_quests.xml",
        "themes.xml",
    )

    fun resolveResRoot(): File? = devResDirs.firstOrNull { it.isDirectory }

    fun getBundle(locale: Locale): ParsedXmlBundle {
        val folderCandidates = candidateFoldersFor(locale)
        val cacheKey = folderCandidates.firstOrNull() ?: "values"
        return cache.getOrPut(cacheKey) {
            loadBundle(folderCandidates)
        }
    }

    fun getColorHex(name: String): String? {
        return getBundle(Locale.ENGLISH).colors[name]
    }

    fun findDrawableFile(name: String): File? {
        val root = resolveResRoot() ?: return null
        for (dirName in drawableDirs) {
            val dir = root.resolve(dirName)
            if (!dir.isDirectory) continue
            val png = dir.resolve("$name.png")
            if (png.isFile) return png
            val xml = dir.resolve("$name.xml")
            if (xml.isFile) return xml
        }
        return null
    }

    fun openDrawableStream(name: String): InputStream? {
        val file = findDrawableFile(name)
        if (file != null && file.name.endsWith(".png", ignoreCase = true)) {
            return file.inputStream()
        }
        val cl = Thread.currentThread().contextClassLoader ?: ResourceRepository::class.java.classLoader
        for (dirName in drawableDirs) {
            val stream = cl.getResourceAsStream("res/$dirName/$name.png")
                ?: cl.getResourceAsStream("$dirName/$name.png")
            if (stream != null) return stream
        }
        return null
    }

    private fun candidateFoldersFor(locale: Locale): List<String> {
        val lang = when (val l = locale.language.lowercase()) {
            "id" -> "in"
            "iw" -> "he"
            else -> l
        }
        if (lang.isEmpty() || lang == "en") return listOf("values")
        val country = locale.country.uppercase()
        return buildList {
            if (country.isNotEmpty()) add("values-$lang-r$country")
            add("values-$lang")
            add("values")
        }
    }

    private fun loadBundle(folderCandidates: List<String>): ParsedXmlBundle {
        val root = resolveResRoot()
        val strings = mutableMapOf<String, String>()
        val plurals = mutableMapOf<String, Map<String, String>>()
        val arrays = mutableMapOf<String, Array<String>>()
        val colors = mutableMapOf<String, String>()
        val dbf = DocumentBuilderFactory.newInstance()

        fun parseStream(stream: InputStream, sourceName: String) {
            try {
                val doc = dbf.newDocumentBuilder().parse(stream)
                val children = doc.documentElement.childNodes
                for (i in 0 until children.length) {
                    val node = children.item(i)
                    val name = node.attributes?.getNamedItem("name")?.nodeValue ?: continue
                    when (node.nodeName) {
                        "string" -> strings[name] = unescapeAndroidString(node.textContent ?: "")
                        "color" -> colors[name] = (node.textContent ?: "").trim()
                        "plurals" -> {
                            val map = mutableMapOf<String, String>()
                            val items = node.childNodes
                            for (j in 0 until items.length) {
                                val item = items.item(j)
                                if (item.nodeName == "item") {
                                    val q = item.attributes?.getNamedItem("quantity")?.nodeValue ?: continue
                                    map[q] = unescapeAndroidString(item.textContent ?: "")
                                }
                            }
                            plurals[name] = map
                        }
                        "string-array" -> {
                            val list = mutableListOf<String>()
                            val items = node.childNodes
                            for (j in 0 until items.length) {
                                val item = items.item(j)
                                if (item.nodeName == "item") {
                                    list.add(unescapeAndroidString(item.textContent ?: ""))
                                }
                            }
                            arrays[name] = list.toTypedArray()
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("ResourceRepository", "Failed to parse XML resource: $sourceName", e)
            }
        }

        if (root != null) {
            val folder = folderCandidates
                .map { root.resolve(it) }
                .firstOrNull { it.isDirectory }
                ?: root.resolve("values")

            folder.listFiles { f -> f.isFile && f.extension == "xml" }?.forEach { xmlFile ->
                xmlFile.inputStream().use { parseStream(it, xmlFile.path) }
            }
        } else {
            val cl = Thread.currentThread().contextClassLoader ?: ResourceRepository::class.java.classLoader
            val folderName = folderCandidates.firstOrNull { candidate ->
                cl.getResource("res/$candidate/strings.xml") != null
            } ?: "values"
            for (xmlName in knownXmlFiles) {
                val resPath = "res/$folderName/$xmlName"
                cl.getResourceAsStream(resPath)?.use { parseStream(it, resPath) }
            }
        }
        return ParsedXmlBundle(strings, plurals, arrays, colors)
    }

    internal fun unescapeAndroidString(raw: String): String {
        var s = raw.trim()
        if (s.length >= 2 && s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length - 1)
        }
        val sb = StringBuilder(s.length)
        var i = 0
        while (i < s.length) {
            val c = s[i]
            if (c == '\\' && i + 1 < s.length) {
                when (val next = s[i + 1]) {
                    '\'' -> { sb.append('\''); i += 2 }
                    '"' -> { sb.append('"'); i += 2 }
                    'n' -> { sb.append('\n'); i += 2 }
                    't' -> { sb.append('\t'); i += 2 }
                    '\\' -> { sb.append('\\'); i += 2 }
                    '@' -> { sb.append('@'); i += 2 }
                    '?' -> { sb.append('?'); i += 2 }
                    'u' -> {
                        if (i + 5 < s.length) {
                            val hex = s.substring(i + 2, i + 6)
                            val code = hex.toIntOrNull(16)
                            if (code != null) {
                                sb.append(code.toChar())
                                i += 6
                                continue
                            }
                        }
                        sb.append(c)
                        i++
                    }
                    else -> {
                        sb.append(next)
                        i += 2
                    }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }
}
