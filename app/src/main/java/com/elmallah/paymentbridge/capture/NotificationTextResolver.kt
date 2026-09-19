package com.elmallah.paymentbridge.capture

data class NotificationMessageCandidate(
    val text: String,
    val timestamp: Long? = null,
    val order: Int = 0
)

/**
 * Chooses the newest actual message body from an Android messaging notification.
 *
 * Samsung Messages can expose stale EXTRA_BIG_TEXT while EXTRA_TEXT / EXTRA_MESSAGES
 * already contain the newest SMS. Payment parsing must therefore prefer structured
 * message payloads and the direct notification text, using BIG_TEXT only as fallback.
 */
object NotificationTextResolver {

    fun resolveLatest(
        messagingCandidates: List<NotificationMessageCandidate>,
        directText: String?,
        textLines: List<String>,
        bigText: String?
    ): String {
        val structured = messagingCandidates
            .filter { it.text.isNotBlank() }
            .maxWithOrNull(
                compareBy<NotificationMessageCandidate> { it.timestamp ?: Long.MIN_VALUE }
                    .thenBy { it.order }
            )
            ?.text
            ?.trim()
            .orEmpty()

        if (structured.isNotBlank()) return structured

        val direct = directText?.trim().orEmpty()
        if (direct.isNotBlank() && !looksLikeNotificationSummary(direct)) return direct

        val latestLine = textLines.lastOrNull { it.isNotBlank() }?.trim().orEmpty()
        if (latestLine.isNotBlank()) return latestLine

        return bigText?.trim().orEmpty()
    }

    private fun looksLikeNotificationSummary(text: String): Boolean {
        val normalized = text.trim().lowercase()
        return Regex("""^\d+\s+(?:new\s+)?messages?$""").matches(normalized) ||
            Regex("""^\d+\s+رسائل?$""").matches(normalized)
    }
}
