package com.elmallah.paymentbridge.capture

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.elmallah.paymentbridge.AlMallahBridgeApp
import com.elmallah.paymentbridge.data.RecordResult
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.CompositePaymentParser
import com.elmallah.paymentbridge.parser.PaymentParseResult
import com.elmallah.paymentbridge.sync.BridgeHeartbeatLoop
import com.elmallah.paymentbridge.sync.BridgeSyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PaymentNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val parser = CompositePaymentParser()
    private var heartbeatLoop: BridgeHeartbeatLoop? = null

    companion object {
        private const val TAG = "PaymentBridgeListener"
        @Volatile
        var isConnected: Boolean = false
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.i(TAG, "Notification listener connected successfully.")

        val app = application as? AlMallahBridgeApp ?: return
        heartbeatLoop = BridgeHeartbeatLoop(
            context = applicationContext,
            scope = serviceScope,
            keyManager = app.apiProvider.keyManager,
            apiProvider = app.apiProvider
        ).also { loop -> loop.start { isConnected } }

        BridgeSyncCoordinator.enqueueImmediate(applicationContext)
    }

    override fun onListenerDisconnected() {
        isConnected = false
        Log.w(TAG, "Notification listener disconnected.")

        val loop = heartbeatLoop
        if (loop != null) {
            serviceScope.launch {
                loop.sendOnce(listenerEnabled = false)
                loop.stop()
            }
        }
        super.onListenerDisconnected()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return

        val sourcePackage = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim() ?: ""

        val sourceValidation = TrustedNotificationSourcePolicy.validateSource(sourcePackage, title)
        if (sourceValidation is SourceValidationResult.Rejected) return

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.filterNotNull()
            ?.map { it.toString().trim() }
            ?.filter { it.isNotEmpty() }
            ?.joinToString("\n")

        val messagingStyleText = try {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)?.mapNotNull { item ->
                if (item is Bundle) item.getCharSequence("text")?.toString()?.trim() else null
            }?.filter { it.isNotEmpty() }?.joinToString("\n")
        } catch (_: Exception) {
            null
        }

        val resolvedBigText = when {
            !bigText.isNullOrBlank() -> bigText
            !messagingStyleText.isNullOrBlank() -> messagingStyleText
            !textLines.isNullOrBlank() -> textLines
            else -> null
        }

        val rawMessage = RawNotificationMessage(
            sourcePackage = sourcePackage,
            title = title,
            text = text,
            bigText = resolvedBigText,
            subText = subText,
            postedAtMillis = sbn.postTime
        )

        serviceScope.launch { processNotification(rawMessage) }
    }

    private suspend fun processNotification(rawMessage: RawNotificationMessage) {
        val app = application as? AlMallahBridgeApp ?: return
        val repository = app.repository
        val keyManager = app.apiProvider.keyManager

        when (val parseResult = parser.parseLiveMessage(rawMessage, keyManager.deviceId)) {
            is PaymentParseResult.Ignored -> Log.d(TAG, "Notification ignored by policy: ${parseResult.reason}")
            is PaymentParseResult.Failed -> Log.d(TAG, "Notification did not match financial receipt pattern: ${parseResult.reason}")
            is PaymentParseResult.Success -> {
                val event = parseResult.event

                val providerEnabled = when (event.provider) {
                    PaymentProvider.VODAFONE_CASH -> keyManager.vfCashEnabled
                    PaymentProvider.NBE_INCOMING_TRANSFER -> keyManager.bankAlAhlyEnabled
                    else -> false
                }
                if (!providerEnabled) {
                    Log.i(TAG, "Payment notification ignored because provider is disabled for this device.")
                    return
                }

                Log.i(
                    TAG,
                    "Payment receipt parsed. Provider=${event.provider}, AmountMinor=${event.amountMinor}, Channel=${event.paymentChannel}"
                )

                val rawSnippetToSave = if (keyManager.rawDiagnosticsEnabled) rawMessage.fullText else null
                when (repository.recordPaymentEvent(event, rawSnippetToSave)) {
                    is RecordResult.Success -> {
                        Log.i(TAG, "Payment event stored locally. UploadEnabled=${keyManager.bridgeUploadEnabled}")
                        if (keyManager.bridgeUploadEnabled) {
                            BridgeSyncCoordinator.enqueueImmediate(applicationContext)
                        }
                    }
                    is RecordResult.Duplicate -> Log.w(TAG, "Duplicate payment event detected locally; upload skipped.")
                    is RecordResult.Failure -> Log.e(TAG, "Failed to persist payment event into Room database.")
                }
            }
        }
    }

    override fun onDestroy() {
        heartbeatLoop?.stop()
        heartbeatLoop = null
        isConnected = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
