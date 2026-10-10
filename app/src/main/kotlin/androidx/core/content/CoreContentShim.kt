package androidx.core.content

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import java.io.File

object ContextCompat {
    fun checkSelfPermission(context: Context, permission: String): Int =
        PackageManager.PERMISSION_GRANTED
}

object FileProvider {
    fun getUriForFile(context: Context, authority: String, file: File): Uri =
        Uri.fromFile(file)
}
