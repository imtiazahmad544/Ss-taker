package com.floatingscreen.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Environment
import android.util.DisplayMetrics
import android.view.WindowManager
import com.floatingscreen.domain.model.MediaRecord
import com.floatingscreen.domain.model.MediaType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class ScreenshotCapture(
    private val context: Context,
    private val mediaProjection: MediaProjection
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val displayMetrics = DisplayMetrics().also {
        windowManager.defaultDisplay.getRealMetrics(it)
    }

    suspend fun captureScreenshot(delaySeconds: Int = 0): MediaRecord? = withContext(Dispatchers.IO) {
        try {
            if (delaySeconds > 0) {
                delay(delaySeconds * 1000L)
            }

            val width = displayMetrics.widthPixels
            val height = displayMetrics.heightPixels
            val density = displayMetrics.densityDpi

            var imageReader: ImageReader? = null
            var virtualDisplay: VirtualDisplay? = null

            try {
                imageReader = ImageReader.newInstance(
                    width, height,
                    PixelFormat.RGBA_8888,
                    2
                )

                virtualDisplay = mediaProjection.createVirtualDisplay(
                    "ScreenshotCapture",
                    width, height, density,
                    DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                    imageReader.surface,
                    null, null
                )

                // Wait for the frame
                delay(200)

                val image: Image? = imageReader.acquireLatestImage()
                val bitmap = image?.let { convertImageToBitmap(it, width, height) }
                image?.close()

                bitmap?.let { saveBitmap(it) }
            } finally {
                virtualDisplay?.release()
                imageReader?.close()
            }
        } catch (e: Exception) {
            Timber.e(e, "Screenshot capture failed")
            null
        }
    }

    private fun convertImageToBitmap(image: Image, width: Int, height: Int): Bitmap {
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        val bitmap = Bitmap.createBitmap(
            width + rowPadding / pixelStride,
            height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)

        // Crop to exact screen size
        return Bitmap.createBitmap(bitmap, 0, 0, width, height).also {
            if (it != bitmap) bitmap.recycle()
        }
    }

    private fun saveBitmap(bitmap: Bitmap): MediaRecord? {
        return try {
            val timestamp = System.currentTimeMillis()
            val fileName = "SCR_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))}.png"
            val dir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                "FloatingScreenUtility"
            )
            dir.mkdirs()
            val file = File(dir, fileName)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            bitmap.recycle()

            MediaRecord(
                fileName = fileName,
                filePath = file.absolutePath,
                mediaType = MediaType.SCREENSHOT,
                timestamp = timestamp,
                width = bitmap.width,
                height = bitmap.height,
                fileSizeBytes = file.length()
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to save bitmap")
            null
        }
    }

    fun release() {
        // MediaProjection is released by the service
    }
}
