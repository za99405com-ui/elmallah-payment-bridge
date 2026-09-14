package com.example.paymentbridge.domain

object PaymentProvider {
    const val VODAFONE_CASH = "vodafone_cash"
    const val NBE_INCOMING_TRANSFER = "nbe_incoming_transfer"
    const val UNKNOWN = "unknown"
}

object PaymentChannel {
    const val VODAFONE_CASH = "vodafone_cash"
    const val INSTAPAY_OR_BANK_TRANSFER = "instapay_or_bank_transfer"
    const val OTHER = "other"
}

enum class SyncStatus {
    PENDING_UPLOAD,
    UPLOADING,
    UPLOADED,
    FAILED_RETRYABLE,
    REJECTED_BY_SERVER,
    DUPLICATE
}

enum class ServerMatchStatus {
    MATCHED,
    AMBIGUOUS,
    NO_MATCH,
    DUPLICATE,
    REJECTED,
    PENDING_REVIEW
}

data class RawNotificationMessage(
    val sourcePackage: String,
    val title: String,
    val text: String,
    val bigText: String? = null,
    val subText: String? = null,
    val postedAtMillis: Long = System.currentTimeMillis()
) {
    val fullText: String
        get() = buildString {
            if (bigText?.isNotBlank() == true) {
                append(bigText)
            } else {
                append(text)
            }
            if (!subText.isNullOrBlank()) {
                append("\n").append(subText)
            }
        }.trim()
}
