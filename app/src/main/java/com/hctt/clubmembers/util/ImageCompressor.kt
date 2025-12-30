package com.hctt.clubmembers.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Compresses the given image URI to JPEG with a soft cap around 200 KB.
 */
fun Context.compressImage(
    uri: Uri,
    rotationDegrees: Float = 0f,
    quality: Int = 80,
    maxSizeKb: Int = 200
): InputStream? {
    val input = contentResolver.openInputStream(uri) ?: return null
    val bitmap = BitmapFactory.decodeStream(input) ?: return null
    val workingBitmap = bitmap.rotateIfNeeded(rotationDegrees)
    val stream = ByteArrayOutputStream()
    var currentQuality = quality
    workingBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, stream)
    while (stream.size() / 1024 > maxSizeKb && currentQuality > 40) {
        stream.reset()
        currentQuality -= 5
        workingBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, stream)
    }
    return ByteArrayInputStream(stream.toByteArray())
}

private fun Bitmap.rotateIfNeeded(degrees: Float): Bitmap {
    if (degrees % 360f == 0f) return this
    val matrix = Matrix().apply { postRotate(degrees) }
    return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
}
