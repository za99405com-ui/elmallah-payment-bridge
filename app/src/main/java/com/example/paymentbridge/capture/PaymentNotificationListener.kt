package com.example.paymentbridge.capture

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.paymentbridge.data.PaymentProcessOutcome
import com.example.paymentbridge.data.PaymentRepository
import com.example.paymentbridge.domain.RawNotificationMessage
import com.example.paymentbridge.parser.CompositePaymentParser
import com.example.paymentbridge.parser.PaymentParseResult
import com.example.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = CompositePaymentParser()
    private lateinit var repository: PaymentRepository
    private lateinit var keyManager: DeviceKeyManager

    override fun onCreate() {
        super.onCreate()
        repository = PaymentRepository(applicationContext)
        keyManager = DeviceKeyManager(applicationContext)
        Log.i(TAG, "AlMallah Payment Notification Listener started")
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        Log.i(TAG, "AlMallah Payment Notification Listener stopped")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()
        val postedTime = sbn.postTime

        if (title.isBlank() && text.isBlank() && bigText.isNullOrBlank()) {
            return
        }

        val rawMessage = RawNotificationMessage(
            sourcePackage = packageName,
            title = title,
            text = text,
            bigText = bigText,
            subText = subText,
            postedAtMillis = postedTime
        )

        serviceScope.launch {
            try {
                when (val result = parser.parse(rawMessage, keyManager.deviceId)) {
                    is PaymentParseResult.Success -> {
                        Log.i(TAG, "Payment message detected: ${result.event.provider}, ref=${result.event.transactionReference}, amount=${result.event.amountMinor}")
                        val rawSnippet = if (keyManager.isRawDiagnosticsEnabled) rawMessage.fullText else null
                        when (val outcome = repository.ingestPaymentEvent(result.event, rawSnippet)) {
                            is PaymentProcessOutcome.NewPaymentEnqueued -> {
                                Log.i(TAG, "New payment enqueued: ${outcome.event.transactionReference}")
                            }
                            is PaymentProcessOutcome.DuplicateDetected -> {
                                Log.w(TAG, "Duplicate transaction ignored: ${outcome.existingReference}")
                            }
                            is PaymentProcessOutcome.SaveFailed -> {
                                Log.e(TAG, "Failed to save payment: ${outcome.error}")
                            }
                        }
                    }
                    is PaymentParseResult.Ignored -> {
                        Log.d(TAG, "Notification ignored: ${result.reason} - ${result.message}")
                    }
                    is PaymentParseResult.Failed -> {
                        Log.w(TAG, "Payment parse failed: ${result.reason} - ${result.message}")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing incoming notification", e)
            }
        }
    }

    companion object {
        private const val TAG = "PaymentNotificationListener"
    }
}
