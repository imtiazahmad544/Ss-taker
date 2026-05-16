package com.floatingscreen.ui

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.floatingscreen.domain.model.*
import com.floatingscreen.domain.usecase.GetSettingsUseCase
import com.floatingscreen.domain.usecase.UpdateSettingsUseCase
import com.floatingscreen.permissions.PermissionManager
import com.floatingscreen.service.FloatingWindowService
import com.floatingscreen.service.ScreenRecordService
import com.floatingscreen.service.ScreenshotService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    val permissionManager: PermissionManager,
    private val getSettingsUseCase: GetSettingsUseCase,
    private val updateSettingsUseCase: UpdateSettingsUseCase,
) : ViewModel() {

    // Settings state
    val settings: StateFlow<AppSettings> = getSettingsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    // Permission state
    private val _permissionStatus = MutableStateFlow(PermissionStatus())
    val permissionStatus: StateFlow<PermissionStatus> = _permissionStatus.asStateFlow()

    // Recording state - communicated via Intent/Broadcast in real impl
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    // Screenshot state
    private val _screenshotState = MutableStateFlow<ScreenshotState>(ScreenshotState.Idle)
    val screenshotState: StateFlow<ScreenshotState> = _screenshotState.asStateFlow()

    // Floating window visible
    private val _isFloatingVisible = MutableStateFlow(false)
    val isFloatingVisible: StateFlow<Boolean> = _isFloatingVisible.asStateFlow()

    // MediaProjection result code & data stored from Activity result
    private var projectionResultCode: Int = -1
    private var projectionData: Intent? = null

    // Timer flow for recording
    private val _elapsedRecordingMs = MutableStateFlow(0L)
    val elapsedRecordingMs: StateFlow<Long> = _elapsedRecordingMs.asStateFlow()

    // UI events
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent.asSharedFlow()

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        _permissionStatus.value = permissionManager.getCurrentPermissionStatus()
    }

    fun onMediaProjectionResult(resultCode: Int, data: Intent?) {
        projectionResultCode = resultCode
        projectionData = data
        if (data != null) {
            _permissionStatus.update { it.copy(mediaProjectionGranted = true) }
        }
    }

    fun requestMediaProjection(activity: Activity) {
        val manager = context.getSystemService(MediaProjectionManager::class.java)
        val captureIntent = manager.createScreenCaptureIntent()
        activity.startActivityForResult(captureIntent, REQUEST_MEDIA_PROJECTION)
    }

    // ========== RECORDING ==========

    fun startRecording() {
        val data = projectionData ?: run {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.RequestMediaProjection)
            }
            return
        }

        val settings = settings.value
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_START_RECORDING
            putExtra(ScreenRecordService.EXTRA_RESULT_CODE, projectionResultCode)
            putExtra(ScreenRecordService.EXTRA_DATA, data)
            putExtra(ScreenRecordService.EXTRA_QUALITY, settings.recordingQuality)
            putExtra(ScreenRecordService.EXTRA_FRAME_RATE, settings.frameRate)
            putExtra(ScreenRecordService.EXTRA_AUDIO_SOURCE, settings.audioSource)
        }
        context.startForegroundService(intent)
        _recordingState.value = RecordingState.Preparing
    }

    fun stopRecording() {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_STOP_RECORDING
        }
        context.startService(intent)
        _recordingState.value = RecordingState.Stopping
    }

    fun pauseRecording() {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_PAUSE_RECORDING
        }
        context.startService(intent)
    }

    fun resumeRecording() {
        val intent = Intent(context, ScreenRecordService::class.java).apply {
            action = ScreenRecordService.ACTION_RESUME_RECORDING
        }
        context.startService(intent)
    }

    // ========== SCREENSHOT ==========

    fun takeScreenshot(delaySeconds: Int = 0) {
        val data = projectionData ?: run {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.RequestMediaProjection)
            }
            return
        }

        _screenshotState.value = if (delaySeconds > 0) {
            ScreenshotState.Countdown(delaySeconds)
        } else {
            ScreenshotState.Capturing
        }

        val intent = Intent(context, ScreenshotService::class.java).apply {
            action = ScreenshotService.ACTION_CAPTURE
            putExtra(ScreenshotService.EXTRA_RESULT_CODE, projectionResultCode)
            putExtra(ScreenshotService.EXTRA_DATA, data)
            putExtra(ScreenshotService.EXTRA_DELAY_SECONDS, delaySeconds)
        }
        context.startForegroundService(intent)
    }

    // ========== FLOATING WINDOW ==========

    fun showFloatingWindow() {
        if (!permissionManager.hasOverlayPermission()) {
            viewModelScope.launch {
                _uiEvent.emit(UiEvent.RequestOverlayPermission)
            }
            return
        }
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_SHOW
        }
        context.startForegroundService(intent)
        _isFloatingVisible.value = true
    }

    fun hideFloatingWindow() {
        val intent = Intent(context, FloatingWindowService::class.java).apply {
            action = FloatingWindowService.ACTION_HIDE
        }
        context.startService(intent)
        _isFloatingVisible.value = false
    }

    fun updateSettings(settings: AppSettings) {
        viewModelScope.launch {
            updateSettingsUseCase(settings)
        }
    }

    companion object {
        const val REQUEST_MEDIA_PROJECTION = 100
    }
}

sealed class UiEvent {
    object RequestMediaProjection : UiEvent()
    object RequestOverlayPermission : UiEvent()
    data class ShowMessage(val message: String) : UiEvent()
    data class NavigateTo(val route: String) : UiEvent()
}
