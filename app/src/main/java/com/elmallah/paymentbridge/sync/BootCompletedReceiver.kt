package com.elmallah.paymentbridge.sync

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.elmallah.paymentbridge.AlMallahBridgeApp

/**
 * BroadcastReceiver triggered on system reboot or app update.
 * Restores the background service and ensures WorkManager periodic sync is enqueued.
 */
class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        if (context == null || intent == null) return
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "Device reboot or app update received ($action). Recovering Payment Bridge background tasks.")

            try {
                BridgeSyncCoordinator.ensurePeriodicRetry(context)
                BridgeSyncCoordinator.enqueueImmediate(context)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule sync retry on boot", e)
            }

            val app = context.applicationContext as? AlMallahBridgeApp
            if (app?.apiProvider?.keyManager?.bridgeUploadEnabled == true) {
                try {
                    BridgeForegroundService.start(context)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start foreground service on boot", e)
                }
            }
        }
    }
}
