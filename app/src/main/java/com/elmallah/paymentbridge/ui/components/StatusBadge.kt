package com.elmallah.paymentbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.domain.SyncStatus
import com.elmallah.paymentbridge.ui.theme.AmberContainer
import com.elmallah.paymentbridge.ui.theme.AmberWarning
import com.elmallah.paymentbridge.ui.theme.CoralContainer
import com.elmallah.paymentbridge.ui.theme.CoralError
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess
import com.elmallah.paymentbridge.ui.theme.NavySecondary

@Composable
fun StatusBadge(
    syncStatus: String,
    serverMatchStatus: String? = null,
    modifier: Modifier = Modifier
) {
    val (label, bg, fg) = when {
        serverMatchStatus == "MATCHED" -> Triple("مطابق للطلب", EmeraldContainer, EmeraldSuccess)
        serverMatchStatus == "PENDING_REVIEW" -> Triple("قيد المراجعة", AmberContainer, AmberWarning)
        serverMatchStatus == "NO_MATCH" -> Triple("غير مطابق", CoralContainer, CoralError)
        syncStatus == SyncStatus.CAPTURE_ONLY.name -> Triple("محفوظ محلياً", Color(0xFFE2E8F0), NavySecondary)
        syncStatus == SyncStatus.UPLOADED.name -> Triple("تم الإرسال", EmeraldContainer, EmeraldSuccess)
        syncStatus == SyncStatus.PENDING_UPLOAD.name -> Triple("بانتظار الإرسال", AmberContainer, AmberWarning)
        syncStatus == SyncStatus.DUPLICATE.name -> Triple("عملية مكررة", CoralContainer, CoralError)
        syncStatus == SyncStatus.FAILED_RETRYABLE.name -> Triple("إعادة محاولة", CoralContainer, CoralError)
        else -> Triple("قيد المعالجة", Color(0xFFF1F5F9), Color(0xFF64748B))
    }

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
