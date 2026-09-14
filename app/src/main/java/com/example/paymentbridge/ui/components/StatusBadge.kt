package com.example.paymentbridge.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.paymentbridge.domain.SyncStatus
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusInfo
import com.example.ui.theme.StatusNeutral
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning

@Composable
fun StatusBadge(
    syncStatus: String,
    serverMatchStatus: String? = null,
    modifier: Modifier = Modifier
) {
    val (label, bgColor, textColor, icon) = getStatusConfig(syncStatus, serverMatchStatus)

    Box(
        modifier = modifier
            .background(bgColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}

private data class BadgeConfig(
    val label: String,
    val bgColor: Color,
    val textColor: Color,
    val icon: ImageVector
)

private fun getStatusConfig(syncStatus: String, serverMatchStatus: String?): BadgeConfig {
    if (serverMatchStatus == "MATCHED") {
        return BadgeConfig(
            label = "تمت المطابقة ✅",
            bgColor = StatusSuccess.copy(alpha = 0.15f),
            textColor = StatusSuccess,
            icon = Icons.Default.CheckCircle
        )
    }

    if (serverMatchStatus == "AMBIGUOUS" || serverMatchStatus == "PENDING_REVIEW") {
        return BadgeConfig(
            label = "يحتاج مراجعة ⚠️",
            bgColor = StatusWarning.copy(alpha = 0.15f),
            textColor = StatusWarning,
            icon = Icons.Default.Warning
        )
    }

    return when (syncStatus) {
        SyncStatus.UPLOADED.name -> BadgeConfig(
            label = "تم الإرسال للسيرفر",
            bgColor = StatusInfo.copy(alpha = 0.15f),
            textColor = StatusInfo,
            icon = Icons.Default.CloudUpload
        )
        SyncStatus.PENDING_UPLOAD.name -> BadgeConfig(
            label = "بانتظار الإرسال",
            bgColor = StatusWarning.copy(alpha = 0.15f),
            textColor = StatusWarning,
            icon = Icons.Default.HourglassTop
        )
        SyncStatus.UPLOADING.name -> BadgeConfig(
            label = "جارٍ الإرسال...",
            bgColor = StatusInfo.copy(alpha = 0.15f),
            textColor = StatusInfo,
            icon = Icons.Default.CloudUpload
        )
        SyncStatus.DUPLICATE.name -> BadgeConfig(
            label = "مكرر ⛔",
            bgColor = StatusNeutral.copy(alpha = 0.15f),
            textColor = StatusNeutral,
            icon = Icons.Default.Block
        )
        SyncStatus.FAILED_RETRYABLE.name -> BadgeConfig(
            label = "فشل (سيُعاد)",
            bgColor = StatusError.copy(alpha = 0.15f),
            textColor = StatusError,
            icon = Icons.Default.Error
        )
        SyncStatus.REJECTED_BY_SERVER.name -> BadgeConfig(
            label = "مرفوض من السيرفر",
            bgColor = StatusError.copy(alpha = 0.15f),
            textColor = StatusError,
            icon = Icons.Default.Error
        )
        else -> BadgeConfig(
            label = syncStatus,
            bgColor = StatusNeutral.copy(alpha = 0.15f),
            textColor = StatusNeutral,
            icon = Icons.Default.HourglassTop
        )
    }
}
