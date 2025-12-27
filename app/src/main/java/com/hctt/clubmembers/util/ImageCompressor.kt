package com.hctt.clubmembers.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Compresses the given image URI to JPEG with a soft cap around 200 KB.
 */
fun Context.compressImage(uri: Uri, quality: Int = 80, maxSizeKb: Int = 200): InputStream? {
    val input = contentResolver.openInputStream(uri) ?: return null
    val bitmap = BitmapFactory.decodeStream(input) ?: return null
    val stream = ByteArrayOutputStream()
    var currentQuality = quality
    bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, stream)
    while (stream.size() / 1024 > maxSizeKb && currentQuality > 40) {
        stream.reset()
        currentQuality -= 5
        bitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, stream)
    }
    return ByteArrayInputStream(stream.toByteArray())
}
