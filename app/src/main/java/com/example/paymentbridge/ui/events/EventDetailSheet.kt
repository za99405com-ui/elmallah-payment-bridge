package com.example.paymentbridge.ui.events

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.paymentbridge.data.PaymentEventEntity
import com.example.paymentbridge.domain.PaymentProvider
import com.example.paymentbridge.domain.SyncStatus
import com.example.paymentbridge.ui.components.StatusBadge
import com.example.ui.theme.BankAhlyTeal
import com.example.ui.theme.BankAhlyTealContainer
import com.example.ui.theme.VodafoneRed
import com.example.ui.theme.VodafoneRedContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventDetailSheet(
    event: PaymentEventEntity?,
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onRetryUpload: (String) -> Unit
) {
    if (event == null) return

    val context = LocalContext.current
    val isVodafone = event.provider == PaymentProvider.VODAFONE_CASH
    val providerName = if (isVodafone) "فودافون كاش" else "البنك الأهلي / InstaPay"
    val providerColor = if (isVodafone) VodafoneRed else BankAhlyTeal
    val providerBg = if (isVodafone) VodafoneRedContainer else BankAhlyTealContainer
    val icon = if (isVodafone) Icons.Default.PhoneAndroid else Icons.Default.AccountBalance

    val dateFormatter = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale("ar"))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = providerBg,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = providerColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = providerName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = event.paymentChannel,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                StatusBadge(
                    syncStatus = event.syncStatus,
                    serverMatchStatus = event.serverMatchStatus
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Big Amount Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "المبلغ المالي المستلم",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = String.format(Locale.US, "%.2f ج.م", event.amountInMajorUnits),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "(${event.amountMinor} قرش / minor units)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Details List
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    DetailRow(
                        label = "رقم العملية / المرجع",
                        value = event.transactionReference,
                        isCopyable = true,
                        onCopy = { copyToClipboard(context, "المرجع", event.transactionReference) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    if (!event.payerPhone.isNullOrBlank()) {
                        DetailRow(
                            label = "هاتف العميل المرسل",
                            value = event.payerPhone,
                            isCopyable = true,
                            onCopy = { copyToClipboard(context, "الهاتف", event.payerPhone) }
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    }

                    if (!event.accountLast4.isNullOrBlank()) {
                        DetailRow(
                            label = "آخر 4 أرقام من الحساب",
                            value = "**** " + event.accountLast4
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    }

                    if (!event.matchedOrderId.isNullOrBlank()) {
                        DetailRow(
                            label = "الطلب المرتبط بالسيرفر",
                            value = "#" + event.matchedOrderId,
                            isHighlight = true
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                    }

                    DetailRow(
                        label = "وقت الالتقاط",
                        value = dateFormatter.format(Date(event.receivedAt))
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    DetailRow(
                        label = "معرّف الحدث (EventId)",
                        value = event.eventId,
                        isMonospace = true,
                        isCopyable = true,
                        onCopy = { copyToClipboard(context, "Event ID", event.eventId) }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))

                    DetailRow(
                        label = "بصمة التكرار المحلية",
                        value = event.transactionFingerprint,
                        isMonospace = true
                    )

                    if (!event.lastErrorMessage.isNullOrBlank()) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
                        DetailRow(
                            label = "آخر استجابة / خطأ",
                            value = event.lastErrorMessage,
                            isError = true
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Resend Action: Enabled only when safe (failed or rejected, never duplicating)
            val canResend = event.syncStatus == SyncStatus.FAILED_RETRYABLE.name ||
                    event.syncStatus == SyncStatus.REJECTED_BY_SERVER.name ||
                    event.syncStatus == SyncStatus.PENDING_UPLOAD.name

            if (canResend) {
                Button(
                    onClick = {
                        onRetryUpload(event.eventId)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("إعادة الإرسال للسيرفر (آمن ومحتفظ بالمعرف)")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    isCopyable: Boolean = false,
    isMonospace: Boolean = false,
    isHighlight: Boolean = false,
    isError: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
                fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
                color = when {
                    isError -> MaterialTheme.colorScheme.error
                    isHighlight -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                }
            )
        }

        if (isCopyable && onCopy != null) {
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = "نسخ",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "تم نسخ $label", Toast.LENGTH_SHORT).show()
}
