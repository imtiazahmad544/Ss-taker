package com.floatingscreen.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.*
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.floatingscreen.FloatingScreenApp
import com.floatingscreen.R
import com.floatingscreen.ui.MainActivity
import com.floatingscreen.ui.components.FloatingBubbleContent
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import kotlin.math.abs

@AndroidEntryPoint
class FloatingWindowService : Service() {

    private lateinit var windowManager: WindowManager
    private var floatingView: View? = null
    private var isExpanded = false

    // Last saved position
    private var savedX = 0
    private var savedY = 300

    // Drag tracking
    private var dragStartX = 0f
    private var dragStartY = 0f
    private var viewStartX = 0
    private var viewStartY = 0
    private var isDragging = false

    // Callbacks to main activity / VM
    var onStartRecording: (() -> Unit)? = null
    var onStopRecording: (() -> Unit)? = null
    var onTakeScreenshot: (() -> Unit)? = null
    var onOpenHistory: (() -> Unit)? = null
    var onOpenSettings: (() -> Unit)? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        startForeground(
            FloatingScreenApp.NOTIFICATION_ID_FLOATING,
            buildFloatingNotification()
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_SHOW -> showFloatingWindow()
            ACTION_HIDE -> hideFloatingWindow()
            ACTION_STOP -> stopSelf()
        }
        return START_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun showFloatingWindow() {
        if (floatingView != null) return

        val params = createWindowParams()

        // We use a FrameLayout that hosts a ComposeView
        // For simplicity, we'll use an XML inflated view approach here
        // In a full Compose setup, ComposeView requires lifecycle owner injection
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null)

        floatingView!!.setOnTouchListener(createDragTouchListener(params))

        // Click listener on bubble icon to expand/collapse
        floatingView!!.findViewById<View>(R.id.bubble_icon)?.setOnClickListener {
            toggleExpand()
        }

        // Control buttons
        floatingView!!.findViewById<View>(R.id.btn_record)?.setOnClickListener {
            vibrate()
            onStartRecording?.invoke()
        }
        floatingView!!.findViewById<View>(R.id.btn_stop)?.setOnClickListener {
            vibrate()
            onStopRecording?.invoke()
        }
        floatingView!!.findViewById<View>(R.id.btn_screenshot)?.setOnClickListener {
            vibrate()
            onTakeScreenshot?.invoke()
            collapse()
        }
        floatingView!!.findViewById<View>(R.id.btn_history)?.setOnClickListener {
            vibrate()
            onOpenHistory?.invoke()
        }
        floatingView!!.findViewById<View>(R.id.btn_settings)?.setOnClickListener {
            vibrate()
            onOpenSettings?.invoke()
        }
        floatingView!!.findViewById<View>(R.id.btn_close)?.setOnClickListener {
            collapse()
            stopSelf()
        }

        try {
            windowManager.addView(floatingView, params)
        } catch (e: Exception) {
            Timber.e(e, "Failed to add floating view")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun createDragTouchListener(params: WindowManager.LayoutParams): View.OnTouchListener {
        return View.OnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    dragStartX = event.rawX
                    dragStartY = event.rawY
                    viewStartX = params.x
                    viewStartY = params.y
                    isDragging = false
                    false
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - dragStartX
                    val dy = event.rawY - dragStartY
                    if (!isDragging && (abs(dx) > 10 || abs(dy) > 10)) {
                        isDragging = true
                    }
                    if (isDragging) {
                        params.x = (viewStartX + dx).toInt()
                        params.y = (viewStartY + dy).toInt()
                        windowManager.updateViewLayout(floatingView, params)
                    }
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (!isDragging) {
                        // It's a tap, not drag
                        view.performClick()
                    } else {
                        // Save position
                        savedX = params.x
                        savedY = params.y
                        // Snap to edge
                        snapToEdge(params)
                    }
                    isDragging = false
                    false
                }
                else -> false
            }
        }
    }

    private fun snapToEdge(params: WindowManager.LayoutParams) {
        val display = windowManager.defaultDisplay
        val size = android.graphics.Point()
        display.getSize(size)
        val screenWidth = size.x

        val bubbleWidth = floatingView?.width ?: 150
        val targetX = if (params.x + bubbleWidth / 2 < screenWidth / 2) {
            -20 // left edge with slight overhang
        } else {
            screenWidth - bubbleWidth + 20 // right edge
        }

        // Animate to edge
        val startX = params.x
        val animator = android.animation.ValueAnimator.ofInt(startX, targetX)
        animator.duration = 200
        animator.addUpdateListener { anim ->
            params.x = anim.animatedValue as Int
            try { windowManager.updateViewLayout(floatingView, params) } catch (e: Exception) { }
        }
        animator.start()
        savedX = targetX
    }

    private fun toggleExpand() {
        if (isExpanded) collapse() else expand()
    }

    private fun expand() {
        isExpanded = true
        floatingView?.findViewById<View>(R.id.controls_panel)?.visibility = View.VISIBLE
        floatingView?.animate()?.scaleX(1f)?.scaleY(1f)?.setDuration(200)?.start()
    }

    private fun collapse() {
        isExpanded = false
        floatingView?.findViewById<View>(R.id.controls_panel)?.visibility = View.GONE
    }

    private fun hideFloatingWindow() {
        floatingView?.let {
            try {
                windowManager.removeView(it)
            } catch (e: Exception) {
                Timber.e(e, "Error removing floating view")
            }
        }
        floatingView = null
    }

    private fun createWindowParams(): WindowManager.LayoutParams {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        return WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            savedX,
            savedY,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
        }
    }

    private fun buildFloatingNotification(): Notification {
        val stopIntent = PendingIntent.getService(
            this, 0,
            Intent(this, FloatingWindowService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_IMMUTABLE
        )
        val mainIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, FloatingScreenApp.CHANNEL_FLOATING)
            .setContentTitle("Floating Screen Utility")
            .setContentText("Floating controls active")
            .setSmallIcon(R.drawable.ic_floating)
            .setContentIntent(mainIntent)
            .addAction(R.drawable.ic_close, "Stop", stopIntent)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }

    private fun vibrate() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(VibratorManager::class.java)
                vibratorManager.defaultVibrator.vibrate(
                    VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (e: Exception) {
            Timber.e(e, "Vibration error")
        }
    }

    override fun onDestroy() {
        hideFloatingWindow()
        super.onDestroy()
    }

    companion object {
        const val ACTION_SHOW = "action.show_floating"
        const val ACTION_HIDE = "action.hide_floating"
        const val ACTION_STOP = "action.stop_floating"
    }
}
