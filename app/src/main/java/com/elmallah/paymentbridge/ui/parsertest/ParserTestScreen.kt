package com.elmallah.paymentbridge.ui.parsertest

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.capture.CapturedNotification
import com.elmallah.paymentbridge.capture.RecentNotificationStore
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.parser.PaymentParseResult
import com.elmallah.paymentbridge.ui.components.AppIconView
import com.elmallah.paymentbridge.ui.components.RecentNotificationPickerSheet
import com.elmallah.paymentbridge.ui.theme.AmberContainer
import com.elmallah.paymentbridge.ui.theme.AmberWarning
import com.elmallah.paymentbridge.ui.theme.CoralContainer
import com.elmallah.paymentbridge.ui.theme.CoralError
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess
import com.elmallah.paymentbridge.ui.theme.NavyPrimary
import com.elmallah.paymentbridge.ui.theme.TextMuted
import com.elmallah.paymentbridge.ui.theme.TextPrimary
import com.elmallah.paymentbridge.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParserTestScreen(
    viewModel: ParserTestViewModel,
    onBack: () -> Unit
) {
    val rules by viewModel.rules.collectAsState()
    val selectedRule by viewModel.selectedRule.collectAsState()
    val recentNotifications by viewModel.recentNotifications.collectAsState()

    val senderTitle by viewModel.senderTitleInput.collectAsState()
    val messageText by viewModel.messageTextInput.collectAsState()
    val parseResult by viewModel.parseResultState.collectAsState()
    val saveResult by viewModel.saveResultState.collectAsState()
    val feedbackMessage by viewModel.addSampleMessageFeedback.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRecentSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اختبار قراءة إشعار",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Source Selector
            item {
                Column {
                    Text(
                        text = "مصدر الدفع:",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (rules.isEmpty()) {
                        Text(
                            text = "لا توجد مصادر دفع مسجلة. يمكنك إضافتها من الإعدادات.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(rules, key = { it.id }) { rule ->
                                val isSelected = rule.id == selectedRule?.id
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.selectRule(rule.id) },
                                    label = { Text(rule.name, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                                    leadingIcon = {
                                        AppIconView(
                                            packageName = rule.packageNames.firstOrNull(),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Display selected app summary
                    selectedRule?.let { rule ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AppIconView(
                                    packageName = rule.packageNames.firstOrNull(),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "التطبيق المرتبط: ${rule.friendlyDisplayAppName}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = rule.packageNames.firstOrNull() ?: "غير محدد",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick Samples Row
            item {
                Column {
                    Text(
                        text = "نماذج تجريبية سريعة:",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(viewModel.samplePresets) { preset ->
                            SuggestionChip(
                                onClick = { viewModel.loadPreset(preset) },
                                label = { Text(preset.title, fontSize = 12.sp) }
                            )
                        }
                    }
                }
            }

            // Notification Title / Sender
            item {
                OutlinedTextField(
                    value = senderTitle,
                    onValueChange = { viewModel.senderTitleInput.value = it },
                    label = { Text("عنوان الإشعار / اسم المرسل (Notification Title)") },
                    placeholder = { Text("مثال: VF-Cash أو Bank-AlAhly") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            // Message Text
            item {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { viewModel.messageTextInput.value = it },
                    label = { Text("نص الإشعار (Notification Text)") },
                    placeholder = { Text("الصق نص الإشعار هنا للاختبار...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(130.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Action Buttons (Recent notification + Test)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRecentSheet = true },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("استخدام آخر إشعار", fontSize = 12.sp)
                    }

                    Button(
                        onClick = { viewModel.executeTestParse() },
                        enabled = messageText.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("اختبار الرسالة", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Feedback / Save Messages
            feedbackMessage?.let { fb ->
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = EmeraldContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = fb,
                            color = EmeraldSuccess,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            saveResult?.let { msg ->
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (msg.startsWith("تم")) EmeraldContainer else AmberContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (msg.startsWith("تم")) EmeraldSuccess else AmberWarning,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }
            }

            // Parse Result Section
            parseResult?.let { result ->
                item {
                    when (result) {
                        is PaymentParseResult.Success -> {
                            val event = result.event
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                                border = BorderStroke(1.dp, EmeraldSuccess.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = EmeraldSuccess,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تم التعرف على عملية دفع",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = EmeraldSuccess
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    ResultDetailRow("المصدر", selectedRule?.name ?: event.provider)
                                    ResultDetailRow("المبلغ", "${String.format(Locale.US, "%.2f", event.amountInMajorUnits)} جنيه")
                                    ResultDetailRow("رقم المحول", event.payerPhone ?: "غير متوفر بالرسالة")
                                    ResultDetailRow("نوع العملية", "استلام أموال")
                                    if (!event.transactionReference.isNullOrBlank()) {
                                        ResultDetailRow("الرقم المرجعي", event.transactionReference)
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    Button(
                                        onClick = { viewModel.saveParsedEventLocally(event) },
                                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldSuccess),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("حفظ هذه العملية محلياً في السجل", fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        is PaymentParseResult.Ignored, is PaymentParseResult.Failed -> {
                            val reasonText = when (result) {
                                is PaymentParseResult.Ignored -> result.reason
                                is PaymentParseResult.Failed -> result.message
                                else -> "لم يتم العثور على مبلغ"
                            }

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = CoralContainer),
                                border = BorderStroke(1.dp, CoralError.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = CoralError,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "لم يتم التعرف على الرسالة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 16.sp,
                                            color = CoralError
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = "السبب: $reasonText",
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )

                                    if (selectedRule != null) {
                                        Spacer(modifier = Modifier.height(12.dp))
                                        Button(
                                            onClick = { viewModel.addCurrentMessageAsSampleToSelectedRule() },
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text("إضافة هذه الرسالة كنموذج لمصدر \"${selectedRule?.name}\"")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section: Recent Notifications (Local history)
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "آخر الإشعارات الملتقطة محلياً",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Text(
                        text = "محفوظة على الهاتف فقط",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (recentNotifications.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "لم يتم التقاط أي إشعارات جديدة بعد. ستظهر الإشعارات الواردة هنا تلقائياً عند تشغيل خدمة المراقبة.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            } else {
                items(recentNotifications.take(8), key = { it.id }) { notif ->
                    RecentNotificationItemRow(
                        notification = notif,
                        onTest = { viewModel.applyCapturedNotification(notif) }
                    )
                }
            }
        }
    }

    if (showRecentSheet) {
        RecentNotificationPickerSheet(
            sheetState = sheetState,
            onDismiss = { showRecentSheet = false },
            onSelectNotification = { picked ->
                viewModel.applyCapturedNotification(picked)
            }
        )
    }
}

@Composable
private fun RecentNotificationItemRow(
    notification: CapturedNotification,
    onTest: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AppIconView(
                packageName = notification.packageName,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title.ifBlank { notification.appName },
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = RecentNotificationStore.formatRelativeTime(notification.timestamp),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedButton(
                onClick = onTest,
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text("اختبار", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun ResultDetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, fontSize = 13.sp, color = TextSecondary)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
