package com.example.data.generator

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.InputStream

object ImageProcessor {

    /**
     * Safely loads and scales a bitmap from a URI within memory limits
     */
    fun loadScaledBitmap(context: Context, uri: Uri, targetWidth: Int, targetHeight: Int): Bitmap? {
        return try {
            var input: InputStream? = context.contentResolver.openInputStream(uri)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(input, null, options)
            input?.close()

            if (options.outWidth <= 0 || options.outHeight <= 0) return null

            var inSampleSize = 1
            while (options.outWidth / (inSampleSize * 2) >= targetWidth &&
                options.outHeight / (inSampleSize * 2) >= targetHeight
            ) {
                inSampleSize *= 2
            }

            val decodeOptions = BitmapFactory.Options().apply {
                this.inSampleSize = inSampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }

            input = context.contentResolver.openInputStream(uri)
            val decoded = BitmapFactory.decodeStream(input, null, decodeOptions)
            input?.close()

            decoded?.let { bmp ->
                scaleCenterCrop(bmp, targetWidth, targetHeight)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Crops and scales the bitmap to exactly match the target aspect ratio and dimensions
     */
    fun scaleCenterCrop(source: Bitmap, targetWidth: Int, targetHeight: Int): Bitmap {
        val srcWidth = source.width
        val srcHeight = source.height

        val scale = maxOf(
            targetWidth.toFloat() / srcWidth,
            targetHeight.toFloat() / srcHeight
        )

        val scaledWidth = scale * srcWidth
        val scaledHeight = scale * srcHeight

        val left = (targetWidth - scaledWidth) / 2f
        val top = (targetHeight - scaledHeight) / 2f

        val output = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(output)
        val matrix = Matrix().apply {
            postScale(scale, scale)
            postTranslate(left, top)
        }
        val paint = android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(source, matrix, paint)
        return output
    }
}
