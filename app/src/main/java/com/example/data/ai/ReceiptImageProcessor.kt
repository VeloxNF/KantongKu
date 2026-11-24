package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import kotlin.math.max
import kotlin.math.roundToInt

object ReceiptImageProcessor {

    suspend fun loadPreparedReceiptBitmap(
        context: Context,
        uri: Uri,
        maxDimensionPx: Int = 2048,
    ): Bitmap? = withContext(Dispatchers.IO) {
        val resolver = context.applicationContext.contentResolver

        val orientation = resolver.openInputStream(uri)?.use { stream ->
            runCatching {
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        } ?: ExifInterface.ORIENTATION_NORMAL

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return@withContext null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = boundsOptions.outWidth,
                height = boundsOptions.outHeight,
                maxDim = maxDimensionPx,
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = resolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return@withContext null

        normalizeOrientationAndScale(decoded, orientation, maxDimensionPx)
    }

    suspend fun loadPreparedReceiptBitmapFromFile(
        file: File,
        maxDimensionPx: Int = 2048,
    ): Bitmap? = withContext(Dispatchers.IO) {
        if (!file.exists()) return@withContext null

        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        FileInputStream(file).use { stream ->
            BitmapFactory.decodeStream(stream, null, boundsOptions)
        }
        if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
            return@withContext null
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(
                width = boundsOptions.outWidth,
                height = boundsOptions.outHeight,
                maxDim = maxDimensionPx,
            )
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }

        val decoded = FileInputStream(file).use { stream ->
            BitmapFactory.decodeStream(stream, null, decodeOptions)
        } ?: return@withContext null

        normalizeOrientationAndScale(decoded, orientation, maxDimensionPx)
    }

    private fun calculateInSampleSize(width: Int, height: Int, maxDim: Int): Int {
        var inSampleSize = 1
        var longest = max(width, height)
        while (longest / inSampleSize > maxDim * 2) {
            inSampleSize *= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun normalizeOrientationAndScale(
        bitmap: Bitmap,
        exifOrientation: Int,
        maxDimensionPx: Int,
    ): Bitmap {
        val matrix = Matrix()
        when (exifOrientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.postRotate(90f)
                matrix.preScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.postRotate(270f)
                matrix.preScale(-1f, 1f)
            }
        }

        val oriented = if (!matrix.isIdentity) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
                if (it !== bitmap) bitmap.recycle()
            }
        } else {
            bitmap
        }

        val longest = max(oriented.width, oriented.height)
        if (longest <= maxDimensionPx) return oriented

        val scale = maxDimensionPx.toFloat() / longest.toFloat()
        val targetW = (oriented.width * scale).roundToInt().coerceAtLeast(1)
        val targetH = (oriented.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(oriented, targetW, targetH, true).also {
            if (it !== oriented) oriented.recycle()
        }
    }
}
