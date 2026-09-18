package com.elmallah.paymentbridge.capture

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class CapturedNotification(
    val id: String = UUID.randomUUID().toString(),
    val packageName: String,
    val appName: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * In-memory thread-safe cache of recent notifications captured locally.
 * Max 25 items kept locally for user convenience when adding samples or testing.
 * Strictly local-only: NEVER sent to any remote server.
 */
object RecentNotificationStore {
    private const val MAX_RECENT = 25
    private val lock = Any()
    private val buffer = ArrayDeque<CapturedNotification>(MAX_RECENT)

    private val _notificationsFlow = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val notificationsFlow: StateFlow<List<CapturedNotification>> = _notificationsFlow.asStateFlow()

    fun addNotification(
        packageName: String,
        appName: String,
        title: String,
        body: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        if (title.isBlank() && body.isBlank()) return
        // Ignore our own internal app notifications
        if (packageName == "com.elmallah.paymentbridge") return

        synchronized(lock) {
            // Avoid duplicate identical notifications within 2 seconds
            val last = buffer.firstOrNull()
            if (last != null &&
                last.packageName == packageName &&
                last.title == title &&
                last.body == body &&
                Math.abs(timestamp - last.timestamp) < 2000
            ) {
                return
            }

            if (buffer.size >= MAX_RECENT) {
                buffer.removeLast()
            }
            buffer.addFirst(
                CapturedNotification(
                    packageName = packageName,
                    appName = appName,
                    title = title,
                    body = body,
                    timestamp = timestamp
                )
            )
            _notificationsFlow.value = buffer.toList()
        }
    }

    fun getAll(): List<CapturedNotification> {
        synchronized(lock) {
            return buffer.toList()
        }
    }

    fun getLatest(): CapturedNotification? {
        synchronized(lock) {
            return buffer.firstOrNull()
        }
    }

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _notificationsFlow.value = emptyList()
        }
    }

    /**
     * Helper to format time relative to now in friendly Arabic
     */
    fun formatRelativeTime(timestamp: Long): String {
        val diffMs = System.currentTimeMillis() - timestamp
        val diffSec = diffMs / 1000
        val diffMin = diffSec / 60
        val diffHour = diffMin / 60
        val diffDays = diffHour / 24

        return when {
            diffSec < 45 -> "الآن"
            diffMin == 1L -> "منذ دقيقة"
            diffMin == 2L -> "منذ دقيقتين"
            diffMin in 3..10 -> "منذ $diffMin دقائق"
            diffMin < 60 -> "منذ $diffMin دقيقة"
            diffHour == 1L -> "منذ ساعة"
            diffHour == 2L -> "منذ ساعتين"
            diffHour in 3..10 -> "منذ $diffHour ساعات"
            diffHour < 24 -> "منذ $diffHour ساعة"
            diffDays == 1L -> "أمس"
            else -> "منذ $diffDays أيام"
        }
    }
}
