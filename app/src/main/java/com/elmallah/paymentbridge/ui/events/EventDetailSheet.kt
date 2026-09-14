package com.elmallah.paymentbridge.ui.events

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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.data.PaymentEventEntity
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.ui.components.StatusBadge
import com.elmallah.paymentbridge.ui.theme.BankAhlyTeal
import com.elmallah.paymentbridge.ui.theme.BankAhlyTealContainer
import com.elmallah.paymentbridge.ui.theme.TextMuted
import com.elmallah.paymentbridge.ui.theme.TextPrimary
import com.elmallah.paymentbridge.ui.theme.TextSecondary
import com.elmallah.paymentbridge.ui.theme.VodafoneRed
import com.elmallah.paymentbridge.ui.theme.VodafoneRedContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventDetailSheet(
    entity: PaymentEventEntity,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isVodafone = entity.provider == PaymentProvider.VODAFONE_CASH

    val providerName = if (isVodafone) "فودافون كاش" else "البنك الأهلي / إنستاباي"
    val providerColor = if (isVodafone) VodafoneRed else BankAhlyTeal
    val providerContainer = if (isVodafone) VodafoneRedContainer else BankAhlyTealContainer
    val icon = if (isVodafone) Icons.Default.PhoneAndroid else Icons.Default.AccountBalance

    val amountFormatted = String.format(Locale.US, "%.2f", entity.amountMinor / 100.0)
    val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale("ar"))

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = providerContainer,
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
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "القناة: ${entity.paymentChannel}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            StatusBadge(
                syncStatus = entity.syncStatus,
                serverMatchStatus = entity.serverMatchStatus
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Amount Block
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$amountFormatted ج.م",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = providerColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entity.amountMinor} قرش (وحدة نقدية دقيقة)",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // Transaction Reference Row with Copy
        DetailRowWithCopy(
            label = "رقم العملية المرجعي",
            value = entity.transactionReference,
            onCopy = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Transaction Reference", entity.transactionReference))
                Toast.makeText(context, "تم نسخ الرقم المرجعي", Toast.LENGTH_SHORT).show()
            }
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

        // Payer Phone or Account
        if (!entity.payerPhone.isNullOrBlank()) {
            DetailRowWithCopy(
                label = "رقم هاتف الراسل",
                value = entity.payerPhone,
                onCopy = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Payer Phone", entity.payerPhone))
                    Toast.makeText(context, "تم نسخ رقم الهاتف", Toast.LENGTH_SHORT).show()
                }
            )
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))
        }

        if (!entity.accountLast4.isNullOrBlank()) {
            DetailRow(label = "آخر 4 أرقام من الحساب", value = "****${entity.accountLast4}")
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))
        }

        if (!entity.walletPhone.isNullOrBlank()) {
            DetailRow(label = "محفظة الاستلام", value = entity.walletPhone)
            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))
        }

        // Parse Confidence (explicitly distinguish from payment match confidence)
        DetailRow(
            label = "دقة قراءة الإشعار (Parse Confidence)",
            value = if (entity.parseConfidence == "high") "عالية (مطابقة كاملة للنص)" else "متوسطة (بعض الحقول غير متوفرة)"
        )
        Text(
            text = "ملاحظة: دقة قراءة الإشعار تعني صحة استخراج البيانات من الرسالة، ولا تعني مطابقة الطلب بالسيرفر بعد.",
            fontSize = 11.sp,
            color = TextMuted,
            modifier = Modifier.padding(top = 2.dp)
        )

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

        // Timestamps
        DetailRow(label = "تاريخ وصول الإشعار", value = dateTimeFormat.format(Date(entity.notificationPostedAt)))
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "تاريخ الحفظ في التطبيق", value = dateTimeFormat.format(Date(entity.capturedAt)))

        HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = Color(0xFFE2E8F0))

        // Source Package & Sender
        DetailRow(label = "مرسل الإشعار", value = entity.sourceSender)
        Spacer(modifier = Modifier.height(8.dp))
        DetailRow(label = "حزمة التطبيق المصدر", value = entity.sourcePackage)

        // Raw diagnostic snippet (if available)
        if (!entity.rawSnippet.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "نص الرسالة الملتقطة (تشخيص محلي فقط)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color(0xFFF8FAFC),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = entity.rawSnippet,
                    fontSize = 12.sp,
                    color = TextPrimary,
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun DetailRowWithCopy(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 13.sp, color = TextSecondary)
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
        IconButton(onClick = onCopy) {
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "نسخ",
                tint = TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
