package com.floatingscreen.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings
import com.floatingscreen.service.FloatingWindowService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            Timber.d("Boot completed / package replaced - checking auto-start setting")
            // Auto-start is handled via DataStore in a coroutine scope
            // We read settings and conditionally start the floating service
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // Only start if overlay permission is granted
                    if (Settings.canDrawOverlays(context)) {
                        // In production, read DataStore settings here
                        // For now, we just start the floating service
                        val serviceIntent = Intent(context, FloatingWindowService::class.java).apply {
                            action = FloatingWindowService.ACTION_SHOW
                        }
                        context.startForegroundService(serviceIntent)
                    }
                } catch (e: Exception) {
                    Timber.e(e, "Boot receiver error")
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
