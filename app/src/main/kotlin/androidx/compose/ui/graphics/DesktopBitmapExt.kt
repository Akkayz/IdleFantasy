package androidx.compose.ui.graphics

import android.graphics.Bitmap

fun Bitmap.asImageBitmap(): ImageBitmap = this.imageBitmap

fun ImageBitmap.asAndroidBitmap(): Bitmap = Bitmap(this)
