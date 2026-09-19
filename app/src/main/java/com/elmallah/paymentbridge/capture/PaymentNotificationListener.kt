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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

class PaymentNotificationListener : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var parser: CompositePaymentParser? = null
    private var heartbeatLoop: BridgeHeartbeatLoop? = null
    private val pendingRejectedNotifications = ConcurrentHashMap<String, RawNotificationMessage>()

    companion object {
        private const val TAG = "PaymentBridgeListener"
        private const val REJECTED_RETRY_WINDOW_MS = 5 * 60_000L
        private const val MAX_PENDING_REJECTED = 20

        @Volatile
        var isConnected: Boolean = false
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        isConnected = true
        Log.i(TAG, "Notification listener connected successfully.")

        val app = application as? AlMallahBridgeApp ?: return
        parser = CompositePaymentParser(ruleSupplier = { app.ruleStore.getActiveRules() })

        heartbeatLoop = BridgeHeartbeatLoop(
            context = applicationContext,
            scope = serviceScope,
            keyManager = app.apiProvider.keyManager,
            apiProvider = app.apiProvider,
            ruleStore = app.ruleStore
        ).also { loop -> loop.start { isConnected } }

        serviceScope.launch {
            app.ruleStore.rulesFlow.collectLatest {
                retryRecentlyRejectedNotifications(app)
            }
        }

        serviceScope.launch {
            delay(750)
            val now = System.currentTimeMillis()
            try {
                activeNotifications
                    ?.filter { now - it.postTime in 0..REJECTED_RETRY_WINDOW_MS }
                    ?.forEach { onNotificationPosted(it) }
            } catch (e: Exception) {
                Log.w(TAG, "Could not rescan recent active notifications.", e)
            }
        }

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

        val directText = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
            ?.filterNotNull()
            ?.map { it.toString().trim() }
            ?.filter { it.isNotEmpty() }
            ?: emptyList()

        val messagingCandidates = try {
            @Suppress("DEPRECATION")
            extras.getParcelableArray(Notification.EXTRA_MESSAGES)
                ?.mapIndexedNotNull { index, item ->
                    if (item !is Bundle) return@mapIndexedNotNull null
                    val body = item.getCharSequence("text")?.toString()?.trim().orEmpty()
                    if (body.isBlank()) return@mapIndexedNotNull null

                    val timestamp = item.getLong("time", 0L).takeIf { it > 0L }
                    NotificationMessageCandidate(
                        text = body,
                        timestamp = timestamp,
                        order = index
                    )
                }
                ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        val latestBody = NotificationTextResolver.resolveLatest(
            messagingCandidates = messagingCandidates,
            directText = directText,
            textLines = textLines,
            bigText = bigText
        )
        if (latestBody.isBlank()) return

        val rawMessage = RawNotificationMessage(
            sourcePackage = sourcePackage,
            title = title,
            text = latestBody,
            bigText = null,
            subText = subText,
            postedAtMillis = sbn.postTime
        )

        // Record locally for guided setup sample selection and message testing
        val friendlyAppName = try {
            val appInfo = packageManager.getApplicationInfo(sourcePackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (_: Exception) {
            title.ifBlank { sourcePackage }
        }
        RecentNotificationStore.addNotification(
            packageName = sourcePackage,
            appName = friendlyAppName,
            title = title,
            body = latestBody,
            timestamp = sbn.postTime
        )

        val app = application as? AlMallahBridgeApp
        val activeRules = app?.ruleStore?.getActiveRules() ?: emptyList()

        val sourceValidation = TrustedNotificationSourcePolicy.validateSource(
            sourcePackage,
            title,
            activeRules,
            rawMessage.fullText
        )
        if (sourceValidation is SourceValidationResult.Rejected) {
            rememberRejectedNotification(rawMessage)
            Log.d(TAG, "Notification held for rule retry: ${sourceValidation.reason}")
            return
        }

        pendingRejectedNotifications.remove(notificationKey(rawMessage))
        serviceScope.launch { processNotification(rawMessage) }
    }

    private fun notificationKey(message: RawNotificationMessage): String {
        return "${message.sourcePackage}|${message.postedAtMillis}|${message.title}|${message.text.hashCode()}"
    }

    private fun rememberRejectedNotification(message: RawNotificationMessage) {
        val now = System.currentTimeMillis()
        pendingRejectedNotifications.entries.removeIf {
            now - it.value.postedAtMillis > REJECTED_RETRY_WINDOW_MS
        }

        if (pendingRejectedNotifications.size >= MAX_PENDING_REJECTED) {
            val oldest = pendingRejectedNotifications.entries.minByOrNull { it.value.postedAtMillis }
            if (oldest != null) pendingRejectedNotifications.remove(oldest.key)
        }

        pendingRejectedNotifications[notificationKey(message)] = message
    }

    private suspend fun retryRecentlyRejectedNotifications(app: AlMallahBridgeApp) {
        if (pendingRejectedNotifications.isEmpty()) return

        val now = System.currentTimeMillis()
        val activeRules = app.ruleStore.getActiveRules()
        val snapshot = pendingRejectedNotifications.entries.toList()

        for ((key, message) in snapshot) {
            if (now - message.postedAtMillis > REJECTED_RETRY_WINDOW_MS) {
                pendingRejectedNotifications.remove(key)
                continue
            }

            val validation = TrustedNotificationSourcePolicy.validateSource(
                message.sourcePackage,
                message.title,
                activeRules,
                message.fullText
            )

            if (validation is SourceValidationResult.Accepted) {
                pendingRejectedNotifications.remove(key)
                Log.i(TAG, "Retrying notification after authoritative rule refresh.")
                processNotification(message)
            }
        }
    }

    private suspend fun processNotification(rawMessage: RawNotificationMessage) {
        val app = application as? AlMallahBridgeApp ?: return
        val repository = app.repository
        val keyManager = app.apiProvider.keyManager
        val activeParser = parser ?: CompositePaymentParser(ruleSupplier = { app.ruleStore.getActiveRules() })

        when (val parseResult = activeParser.parseLiveMessage(rawMessage, keyManager.deviceId)) {
            is PaymentParseResult.Ignored -> Log.d(TAG, "Notification ignored by policy: ${parseResult.reason}")
            is PaymentParseResult.Failed -> Log.d(TAG, "Notification did not match financial pattern: ${parseResult.reason}")
            is PaymentParseResult.Success -> {
                val event = parseResult.event

                // Verify that rule or provider is actively enabled
                val matchedRule = app.ruleStore.getActiveRules().firstOrNull { it.id == event.paymentSourceId }
                val isEnabled = matchedRule?.enabled ?: when (event.provider) {
                    PaymentProvider.VODAFONE_CASH -> keyManager.vfCashEnabled
                    PaymentProvider.NBE_INCOMING_TRANSFER -> keyManager.bankAlAhlyEnabled
                    else -> true
                }

                if (!isEnabled) {
                    Log.i(TAG, "Payment notification ignored because rule is disabled: ${event.paymentSourceId}")
                    return
                }

                // Update monitoring metrics
                keyManager.lastDetectedNotification = "${rawMessage.title}: ${rawMessage.text.take(60)}"
                keyManager.lastParsedAmountMinor = event.amountMinor
                keyManager.lastParsedTime = System.currentTimeMillis()

                Log.i(
                    TAG,
                    "Payment receipt parsed: Source=${event.paymentSourceId}, AmountMinor=${event.amountMinor}, Channel=${event.paymentChannel}"
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
        pendingRejectedNotifications.clear()
        isConnected = false
        serviceScope.cancel()
        super.onDestroy()
    }
}
