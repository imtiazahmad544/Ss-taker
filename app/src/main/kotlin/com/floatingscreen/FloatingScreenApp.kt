package com.floatingscreen

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class FloatingScreenApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize Timber logging
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Recording channel
        val recordingChannel = NotificationChannel(
            CHANNEL_RECORDING,
            "Screen Recording",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active screen recording notification"
            setShowBadge(false)
        }

        // Screenshot channel
        val screenshotChannel = NotificationChannel(
            CHANNEL_SCREENSHOT,
            "Screenshot",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Screenshot capture notifications"
            setShowBadge(true)
        }

        // Floating window channel
        val floatingChannel = NotificationChannel(
            CHANNEL_FLOATING,
            "Floating Window",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Floating overlay window service"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(
            listOf(recordingChannel, screenshotChannel, floatingChannel)
        )
    }

    companion object {
        const val CHANNEL_RECORDING = "channel_screen_recording"
        const val CHANNEL_SCREENSHOT = "channel_screenshot"
        const val CHANNEL_FLOATING = "channel_floating_window"

        const val NOTIFICATION_ID_RECORDING = 1001
        const val NOTIFICATION_ID_SCREENSHOT = 1002
        const val NOTIFICATION_ID_FLOATING = 1003
    }
}
