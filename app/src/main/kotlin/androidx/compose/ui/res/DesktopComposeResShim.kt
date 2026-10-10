package androidx.compose.ui.res

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.fantasyidler.R
import org.jetbrains.skia.Image
import java.io.File
import java.util.concurrent.ConcurrentHashMap

private val drawableBitmapCache = ConcurrentHashMap<Int, ImageBitmap>()

private val resDirCandidates by lazy {
    listOf(
        File("app/src/main/res"),
        File("src/main/res"),
        File("../app/src/main/res")
    )
}

private val drawableSubdirs = listOf(
    "drawable",
    "drawable-xxxhdpi",
    "drawable-xxhdpi",
    "drawable-xhdpi",
    "drawable-hdpi",
    "drawable-mdpi"
)

private fun loadDrawableBytes(name: String): ByteArray? {
    for (base in resDirCandidates) {
        if (!base.isDirectory) continue
        for (sub in drawableSubdirs) {
            val f = File(File(base, sub), "$name.png")
            if (f.isFile) return f.readBytes()
        }
    }
    for (sub in drawableSubdirs) {
        val stream = R::class.java.classLoader?.getResourceAsStream("res/$sub/$name.png")
            ?: R::class.java.classLoader?.getResourceAsStream("$sub/$name.png")
        if (stream != null) return stream.use { it.readBytes() }
    }
    return null
}

@Composable
fun stringResource(id: Int): String {
    val _locale by AppCompatDelegate.localeFlow.collectAsState()
    val context = LocalContext.current
    return context.getString(id)
}

@Composable
fun stringResource(id: Int, vararg formatArgs: Any): String {
    val _locale by AppCompatDelegate.localeFlow.collectAsState()
    val context = LocalContext.current
    return context.getString(id, *formatArgs)
}

@Composable
fun pluralStringResource(id: Int, count: Int): String {
    val _locale by AppCompatDelegate.localeFlow.collectAsState()
    val context = LocalContext.current
    return context.resources.getQuantityString(id, count)
}

@Composable
fun pluralStringResource(id: Int, count: Int, vararg formatArgs: Any): String {
    val _locale by AppCompatDelegate.localeFlow.collectAsState()
    val context = LocalContext.current
    return context.resources.getQuantityString(id, count, *formatArgs)
}

@Composable
fun painterResource(id: Int): Painter {
    val bitmap = remember(id) {
        drawableBitmapCache.getOrPut(id) {
            val name = R.drawableIdToName[id]
            val bytes = if (name != null) loadDrawableBytes(name) else null
            if (bytes != null) {
                try {
                    Image.makeFromEncoded(bytes).toComposeImageBitmap()
                } catch (_: Exception) {
                    ImageBitmap(1, 1)
                }
            } else {
                ImageBitmap(1, 1)
            }
        }
    }
    return remember(bitmap) { BitmapPainter(bitmap, filterQuality = FilterQuality.None) }
}
