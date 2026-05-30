package com.chat.advanced.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import com.chat.advanced.entity.ChatBgKeys
import com.chat.base.utils.AndroidUtilities
import com.chat.base.views.blurview.impl.AndroidStockBlurImpl
import com.chat.base.views.blurview.impl.AndroidXBlurImpl
import com.chat.base.views.blurview.impl.BlurImpl
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/**
 * Matches PreviewChatBgActivity ShapeBlurView wallpaper blur (blur_radius=3dp, down_sample=4).
 */
object ChatBgBlurHelper {

    private const val BLUR_RADIUS_DP = 3f
    private const val DOWN_SAMPLE_FACTOR = 4f

    fun captureWallpaperBitmap(imageView: ImageView, blurView: View): Bitmap? {
        val width = imageView.width
        val height = imageView.height
        if (width <= 0 || height <= 0) return null
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        imageView.draw(canvas)
        if (blurView.visibility == View.VISIBLE) {
            blurView.draw(canvas)
        }
        return bitmap
    }

    fun saveBitmap(path: String, bitmap: Bitmap): Boolean {
        val file = File(path)
        file.parentFile?.mkdirs()
        return try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            file.exists() && file.length() > 0
        } catch (ignored: Exception) {
            false
        }
    }

    fun deleteBlurredCache(url: String) {
        val path = ChatBgKeys.blurredCacheFilePath(url)
        if (!path.isEmpty()) {
            File(path).delete()
        }
    }

    fun displayPath(context: Context, originalCachePath: String): String {
        val blurredPath = ChatBgKeys.blurredCachePath(originalCachePath)
        if (File(blurredPath).exists()) {
            return blurredPath
        }
        if (File(originalCachePath).exists()) {
            createBlurredCacheFromFile(context, originalCachePath, blurredPath)
            if (File(blurredPath).exists()) {
                return blurredPath
            }
        }
        return originalCachePath
    }

    fun createBlurredCacheFromFile(
        context: Context,
        sourcePath: String,
        destPath: String
    ): Boolean {
        val source = decodeWallpaperBitmap(sourcePath) ?: return false
        val blurred = blurBitmapLikeShapeBlurView(context, source) ?: return false
        val saved = saveBitmap(destPath, blurred)
        if (blurred != source) {
            blurred.recycle()
        }
        source.recycle()
        return saved
    }

    private fun decodeWallpaperBitmap(path: String): Bitmap? {
        val file = File(path)
        if (!file.exists()) return null
        val targetWidth = AndroidUtilities.getScreenWidth()
        val targetHeight = AndroidUtilities.getScreenHeight()
        if (targetWidth <= 0 || targetHeight <= 0) return null
        val options = BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(path, options)
        options.inSampleSize = calculateInSampleSize(options, targetWidth, targetHeight)
        options.inJustDecodeBounds = false
        val decoded = BitmapFactory.decodeFile(path, options) ?: return null
        val scaled = Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
        if (scaled != decoded) {
            decoded.recycle()
        }
        return scaled
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            var halfHeight = height / 2
            var halfWidth = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun blurBitmapLikeShapeBlurView(context: Context, source: Bitmap): Bitmap? {
        val blurRadiusPx = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            BLUR_RADIUS_DP,
            context.resources.displayMetrics
        )
        var downSampleFactor = DOWN_SAMPLE_FACTOR
        var radius = blurRadiusPx / downSampleFactor
        if (radius > 25) {
            downSampleFactor = downSampleFactor * radius / 25
            radius = 25f
        }
        val scaledWidth = max(1, (source.width / downSampleFactor).toInt())
        val scaledHeight = max(1, (source.height / downSampleFactor).toInt())
        val bitmapToBlur = Bitmap.createScaledBitmap(source, scaledWidth, scaledHeight, true)
        val blurredBitmap = Bitmap.createBitmap(scaledWidth, scaledHeight, Bitmap.Config.ARGB_8888)
        val impl = createBlurImpl()
        return try {
            if (impl.prepare(context, bitmapToBlur, radius)) {
                impl.blur(bitmapToBlur, blurredBitmap)
                Bitmap.createScaledBitmap(blurredBitmap, source.width, source.height, true)
            } else {
                null
            }
        } catch (ignored: Exception) {
            null
        } finally {
            impl.release()
            if (bitmapToBlur != source) {
                bitmapToBlur.recycle()
            }
            blurredBitmap.recycle()
        }
    }

    private fun createBlurImpl(): BlurImpl {
        return try {
            Class.forName("androidx.renderscript.RenderScript")
            AndroidXBlurImpl()
        } catch (ignored: Throwable) {
            AndroidStockBlurImpl()
        }
    }
}
