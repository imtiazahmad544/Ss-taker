package com.floatingscreen.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Binder
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.floatingscreen.FloatingScreenApp
import com.floatingscreen.R
import com.floatingscreen.domain.model.*
import com.floatingscreen.media.ScreenRecorder
import com.floatingscreen.ui.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ScreenRecordService : Service() {

    @Inject
    lateinit var insertMediaUseCase: com.floatingscreen.domain.usecase.InsertMediaUseCase

    private val binder = RecordBinder()
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private var mediaProjection: MediaProjection? = null
    private var screenRecorder: ScreenRecorder? = null

    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // Timer job
    private var timerJob: Job? = null

    inner class RecordBinder : Binder() {
        fun getService(): ScreenRecordService = this@ScreenRecordService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        Timber.d("ScreenRecordService created")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_RECORDING -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, -1)
                val data = intent.getParcelableExtra<Intent>(EXTRA_DATA)
                val quality = intent.getSerializableExtra(EXTRA_QUALITY) as? RecordingQuality
                    ?: RecordingQuality.FHD
                val frameRate = intent.getSerializableExtra(EXTRA_FRAME_RATE) as? FrameRate
                    ?: FrameRate.FPS_30
                val audioSource = intent.getSerializableExtra(EXTRA_AUDIO_SOURCE) as? AudioSource
                    ?: AudioSource.MICROPHONE

                if (resultCode != -1 && data != null) {
                    startForeground(
                        FloatingScreenApp.NOTIFICATION_ID_RECORDING,
                        buildRecordingNotification("Recording...")
                    )
                    startRecording(resultCode, data, quality, frameRate, audioSource)
                }
            }
            ACTION_STOP_RECORDING -> stopRecording()
            ACTION_PAUSE_RECORDING -> pauseRecording()
            ACTION_RESUME_RECORDING -> resumeRecording()
        }
        return START_STICKY
    }

    private fun startRecording(
        resultCode: Int,
        data: Intent,
        quality: RecordingQuality,
        frameRate: FrameRate,
        audioSource: AudioSource
    ) {
        _recordingState.value = RecordingState.Preparing

        val projectionManager = getSystemService(MediaProjectionManager::class.java)
        mediaProjection = projectionManager.getMediaProjection(resultCode, data)

        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                Timber.d("MediaProjection stopped externally")
                serviceScope.launch { stopRecording() }
            }
        }, null)

        screenRecorder = ScreenRecorder(this, mediaProjection!!)

        serviceScope.launch(Dispatchers.IO) {
            val file = screenRecorder?.startRecording(quality, frameRate, audioSource) { error ->
                _recordingState.value = RecordingState.Error(error)
            }

            if (file != null) {
                val startTime = System.currentTimeMillis()
                _recordingState.value = RecordingState.Recording(startTime)
                startTimer(startTime)
            } else {
                _recordingState.value = RecordingState.Error("Could not start recording")
                stopSelf()
            }
        }
    }

    private fun stopRecording() {
        if (_recordingState.value is RecordingState.Idle) return

        _recordingState.value = RecordingState.Stopping
        timerJob?.cancel()

        serviceScope.launch(Dispatchers.IO) {
            val record = screenRecorder?.stopRecording()
            screenRecorder?.release()
            screenRecorder = null

            mediaProjection?.stop()
            mediaProjection = null

            if (record != null) {
                val id = insertMediaUseCase(record)
                val savedRecord = record.copy(id = id)
                _recordingState.value = RecordingState.Completed(savedRecord)
                showCompletedNotification(savedRecord)
            } else {
                _recordingState.value = RecordingState.Idle
            }

            withContext(Dispatchers.Main) {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
    }

    private fun pauseRecording() {
        val current = _recordingState.value
        if (current is RecordingState.Recording) {
            screenRecorder?.pauseRecording()
            timerJob?.cancel()
            val elapsed = System.currentTimeMillis() - current.startTimeMs
            _recordingState.value = RecordingState.Paused(elapsed)
            updateNotification("Recording Paused")
        }
    }

    private fun resumeRecording() {
        val current = _recordingState.value
        if (current is RecordingState.Paused) {
            screenRecorder?.resumeRecording()
            val newStart = System.currentTimeMillis() - current.elapsedMs
            _recordingState.value = RecordingState.Recording(newStart)
            startTimer(newStart)
            updateNotification("Recording...")
        }
    }

    private fun startTimer(startTimeMs: Long) {
        timerJob?.cancel()
        timerJob = serviceScope.launch {
            while (isActive) {
                val elapsed = System.currentTimeMillis() - startTimeMs
                val formatted = formatDuration(elapsed)
                updateNotification("Recording: $formatted")
                delay(1000)
            }
        }
    }

    private fun buildRecordingNotification(contentText: String): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ScreenRecordService::class.java).apply {
                action = ACTION_STOP_RECORDING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseIntent = PendingIntent.getService(
            this, 1,
            Intent(this, ScreenRecordService::class.java).apply {
                action = ACTION_PAUSE_RECORDING
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val mainIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, FloatingScreenApp.CHANNEL_RECORDING)
            .setContentTitle("Floating Screen Utility")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_record)
            .setContentIntent(mainIntent)
            .addAction(R.drawable.ic_pause, "Pause", pauseIntent)
            .addAction(R.drawable.ic_stop, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = buildRecordingNotification(text)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(FloatingScreenApp.NOTIFICATION_ID_RECORDING, notification)
    }

    private fun showCompletedNotification(record: MediaRecord) {
        val notification = NotificationCompat.Builder(this, FloatingScreenApp.CHANNEL_SCREENSHOT)
            .setContentTitle("Recording Saved")
            .setContentText(record.fileName)
            .setSmallIcon(R.drawable.ic_check)
            .setAutoCancel(true)
            .build()
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(FloatingScreenApp.NOTIFICATION_ID_SCREENSHOT, notification)
    }

    private fun formatDuration(ms: Long): String {
        val seconds = (ms / 1000) % 60
        val minutes = (ms / (1000 * 60)) % 60
        val hours = ms / (1000 * 60 * 60)
        return if (hours > 0) "%02d:%02d:%02d".format(hours, minutes, seconds)
        else "%02d:%02d".format(minutes, seconds)
    }

    override fun onDestroy() {
        serviceScope.cancel()
        screenRecorder?.release()
        mediaProjection?.stop()
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_RECORDING = "action.start_recording"
        const val ACTION_STOP_RECORDING = "action.stop_recording"
        const val ACTION_PAUSE_RECORDING = "action.pause_recording"
        const val ACTION_RESUME_RECORDING = "action.resume_recording"
        const val EXTRA_RESULT_CODE = "extra.result_code"
        const val EXTRA_DATA = "extra.data"
        const val EXTRA_QUALITY = "extra.quality"
        const val EXTRA_FRAME_RATE = "extra.frame_rate"
        const val EXTRA_AUDIO_SOURCE = "extra.audio_source"
    }
}
