package com.elmallah.paymentbridge.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.elmallah.paymentbridge.AlMallahBridgeApp
import com.elmallah.paymentbridge.data.RecordResult
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.CompositePaymentParser
import com.elmallah.paymentbridge.parser.PaymentParseResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = CompositePaymentParser()

    companion object {
        private const val TAG = "PaymentBridgeListener"
        @Volatile
        var isConnected: Boolean = false
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.i(TAG, "Notification listener connected successfully.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        isConnected = false
        Log.w(TAG, "Notification listener disconnected.")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val sourcePackage = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val rawMessage = RawNotificationMessage(
            sourcePackage = sourcePackage,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            postedAtMillis = sbn.postTime
        )

        // Asynchronously process incoming message with strict security validation
        serviceScope.launch {
            processNotification(rawMessage)
        }
    }

    private suspend fun processNotification(rawMessage: RawNotificationMessage) {
        val app = application as? AlMallahBridgeApp ?: return
        val repository = app.repository
        val keyManager = app.apiProvider.keyManager

        // 1. Strict Live Validation:
        // Rejects non-trusted messaging packages and unapproved sender titles.
        when (val parseResult = parser.parseLiveMessage(rawMessage, keyManager.deviceId)) {
            is PaymentParseResult.Ignored -> {
                // Ignore without logging sensitive details
                Log.d(TAG, "Notification ignored by policy: ${parseResult.reason}")
            }
            is PaymentParseResult.Failed -> {
                Log.d(TAG, "Notification did not match financial receipt pattern: ${parseResult.reason}")
            }
            is PaymentParseResult.Success -> {
                val event = parseResult.event
                // Sanitized log: strictly never log payer phone, reference, or raw SMS
                Log.i(TAG, "Payment receipt parsed successfully. Provider: ${event.provider}, AmountMinor: ${event.amountMinor}, Channel: ${event.paymentChannel}")

                val rawSnippetToSave = if (keyManager.rawDiagnosticsEnabled) rawMessage.fullText else null
                when (val recordResult = repository.recordPaymentEvent(event, rawSnippetToSave)) {
                    is RecordResult.Success -> {
                        Log.i(TAG, "Payment event stored locally. Mode: ${if (keyManager.bridgeUploadEnabled) "BRIDGE_UPLOAD" else "CAPTURE_ONLY"}")
                        // In Phase 1 (CAPTURE_ONLY mode, bridgeUploadEnabled == false), STOP HERE!
                        // Do not upload and do not retry against non-existent endpoints.
                    }
                    is RecordResult.Duplicate -> {
                        Log.w(TAG, "Duplicate payment event detected locally. Skipped duplicate insertion.")
                    }
                    is RecordResult.Failure -> {
                        Log.e(TAG, "Failed to persist payment event into Room database.")
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
