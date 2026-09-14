package com.elmallah.paymentbridge.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.data.PaymentEventEntity
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.ui.theme.BankAhlyTeal
import com.elmallah.paymentbridge.ui.theme.BankAhlyTealContainer
import com.elmallah.paymentbridge.ui.theme.NeutralBorder
import com.elmallah.paymentbridge.ui.theme.TextMuted
import com.elmallah.paymentbridge.ui.theme.TextPrimary
import com.elmallah.paymentbridge.ui.theme.TextSecondary
import com.elmallah.paymentbridge.ui.theme.VodafoneRed
import com.elmallah.paymentbridge.ui.theme.VodafoneRedContainer
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun EventItemCard(
    entity: PaymentEventEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isVodafone = entity.provider == PaymentProvider.VODAFONE_CASH

    val providerName = if (isVodafone) "فودافون كاش" else "البنك الأهلي / إنستاباي"
    val providerColor = if (isVodafone) VodafoneRed else BankAhlyTeal
    val providerContainer = if (isVodafone) VodafoneRedContainer else BankAhlyTealContainer
    val icon = if (isVodafone) Icons.Default.PhoneAndroid else Icons.Default.AccountBalance

    val amountFormatted = String.format(Locale.US, "%.2f", entity.amountMinor / 100.0)
    val timeFormatted = SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(entity.capturedAt))

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NeutralBorder),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = providerContainer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = providerColor,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = providerName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Text(
                            text = timeFormatted,
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$amountFormatted ج.م",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = providerColor
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StatusBadge(
                        syncStatus = entity.syncStatus,
                        serverMatchStatus = entity.serverMatchStatus
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val detailLabel = when {
                    !entity.payerPhone.isNullOrBlank() -> "المرسل: ${entity.payerPhone}"
                    !entity.accountLast4.isNullOrBlank() -> "الحساب: ****${entity.accountLast4}"
                    else -> "المرسل: غير محدد بالرسالة"
                }

                Text(
                    text = detailLabel,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Text(
                    text = "مرجع: ${entity.transactionReference}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted
                )
            }
        }
    }
}
