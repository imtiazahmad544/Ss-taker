package com.floatingscreen.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.floatingscreen.FloatingScreenApp
import com.floatingscreen.R
import com.floatingscreen.domain.usecase.InsertMediaUseCase
import com.floatingscreen.media.ScreenshotCapture
import com.floatingscreen.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ScreenshotService : Service() {

    @Inject
    lateinit var insertMediaUseCase: InsertMediaUseCase

    private val binder = ScreenshotBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaProjection: MediaProjection? = null
    private var screenshotCapture: ScreenshotCapture? = null

    private val _captureResult = MutableSharedFlow<Result<String>>(replay = 0)
    val captureResult: SharedFlow<Result<String>> = _captureResult.asSharedFlow()

    inner class ScreenshotBinder : Binder() {
        fun getService(): ScreenshotService = this@ScreenshotService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_CAPTURE -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                val delaySeconds = intent.getIntExtra(EXTRA_DELAY_SECONDS, 0)

                if (resultCode != -1 && data != null) {
                    startForeground(
                        FloatingScreenApp.NOTIFICATION_ID_RECORDING,
                        buildCapturingNotification(delaySeconds)
                    )
                    captureScreenshot(resultCode, data, delaySeconds)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun captureScreenshot(resultCode: Int, data: Intent, delaySeconds: Int) {
        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        screenshotCapture = ScreenshotCapture(this, mediaProjection!!)

        serviceScope.launch {
            try {
                val record = withContext(Dispatchers.IO) {
                    screenshotCapture?.captureScreenshot(delaySeconds)
                }

                if (record != null) {
                    insertMediaUseCase(record)
                    _captureResult.emit(Result.success(record.filePath))
                    showCapturedNotification(record.fileName)
                } else {
                    _captureResult.emit(Result.failure(Exception("Screenshot capture returned null")))
                }
            } catch (e: Exception) {
                Timber.e(e, "Screenshot capture error")
                _captureResult.emit(Result.failure(e))
            } finally {
                cleanup()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun buildCapturingNotification(delaySeconds: Int): Notification {
        val text = if (delaySeconds > 0) "Taking screenshot in ${delaySeconds}s..." else "Capturing screenshot..."
        val mainIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, FloatingScreenApp.CHANNEL_RECORDING)
            .setContentTitle("Floating Screen Utility")
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_screenshot)
            .setContentIntent(mainIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun showCapturedNotification(fileName: String) {
        val notification = NotificationCompat.Builder(this, FloatingScreenApp.CHANNEL_SCREENSHOT)
            .setContentTitle("Screenshot Saved")
            .setContentText(fileName)
            .setSmallIcon(R.drawable.ic_check)
            .setAutoCancel(true)
            .build()
        androidx.core.app.NotificationManagerCompat.from(this)
            .notify(FloatingScreenApp.NOTIFICATION_ID_SCREENSHOT, notification)
    }

    private fun cleanup() {
        screenshotCapture?.release()
        screenshotCapture = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        serviceScope.cancel()
        cleanup()
        super.onDestroy()
    }

    companion object {
        const val ACTION_CAPTURE = "action.capture_screenshot"
        const val EXTRA_RESULT_CODE = "extra.result_code"
        const val EXTRA_DATA = "extra.data"
        const val EXTRA_DELAY_SECONDS = "extra.delay_seconds"
    }
}
