package android.graphics

import android.content.res.ResourceRepository
import android.content.res.Resources
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.fantasyidler.R
import org.jetbrains.skia.Image
import java.io.InputStream
import java.io.OutputStream
import javax.imageio.ImageIO

class Bitmap(val imageBitmap: ImageBitmap) {
    enum class CompressFormat {
        PNG,
        JPEG,
        WEBP,
    }

    fun compress(format: CompressFormat, quality: Int, stream: OutputStream): Boolean {
        return try {
            val awtImage = imageBitmap.toAwtImage()
            val fmtName = when (format) {
                CompressFormat.PNG -> "png"
                CompressFormat.JPEG -> "jpg"
                CompressFormat.WEBP -> "png"
            }
            ImageIO.write(awtImage, fmtName, stream)
        } catch (_: Exception) {
            false
        }
    }
}

object BitmapFactory {
    fun decodeStream(stream: InputStream?): Bitmap? {
        if (stream == null) return null
        return try {
            val bytes = stream.readBytes()
            val skiaImage = Image.makeFromEncoded(bytes)
            Bitmap(skiaImage.toComposeImageBitmap())
        } catch (_: Exception) {
            null
        }
    }

    fun decodeResource(res: Resources, id: Int): Bitmap? {
        val name = R.drawableIdToName[id] ?: return null
        return try {
            ResourceRepository.openDrawableStream(name)?.use { decodeStream(it) }
        } catch (_: Exception) {
            null
        }
    }
}

object Color {
    @JvmStatic
    fun colorToHSV(color: Int, hsv: FloatArray) {
        val r = (color shr 16) and 0xFF
        val g = (color shr 8) and 0xFF
        val b = color and 0xFF
        val hsb = java.awt.Color.RGBtoHSB(r, g, b, null)
        hsv[0] = hsb[0] * 360f
        hsv[1] = hsb[1]
        hsv[2] = hsb[2]
    }
}
