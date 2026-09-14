package com.elmallah.paymentbridge.ui.home

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.elmallah.paymentbridge.ui.components.EventItemCard
import com.elmallah.paymentbridge.ui.components.MetricCard
import com.elmallah.paymentbridge.ui.events.EventDetailSheet
import com.elmallah.paymentbridge.ui.theme.AmberContainer
import com.elmallah.paymentbridge.ui.theme.AmberWarning
import com.elmallah.paymentbridge.ui.theme.BankAhlyTeal
import com.elmallah.paymentbridge.ui.theme.CoralContainer
import com.elmallah.paymentbridge.ui.theme.CoralError
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess
import com.elmallah.paymentbridge.ui.theme.NavyPrimary
import com.elmallah.paymentbridge.ui.theme.NeutralBorder
import com.elmallah.paymentbridge.ui.theme.TextMuted
import com.elmallah.paymentbridge.ui.theme.TextPrimary
import com.elmallah.paymentbridge.ui.theme.TextSecondary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onNavigateToParserTest: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val isNotificationAccessGranted by viewModel.isNotificationAccessGranted.collectAsState()
    val isCaptureOnlyMode by viewModel.isCaptureOnlyMode.collectAsState()
    val todayMetrics by viewModel.todayMetrics.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val events by viewModel.filteredEvents.collectAsState()
    val selectedEvent by viewModel.selectedEventForDetail.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshNotificationAccess()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "الملاح - مراقب المدفوعات",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = Color.White
                        )
                        Text(
                            text = "بوابة استلام إشعارات الدفع (تطبيق خاص)",
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToParserTest) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "اختبار قراءة الرسائل",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = NavyPrimary,
                    titleContentColor = Color.White
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
            // 1. Prominent Phase 1 Operating Mode Notice
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isCaptureOnlyMode) Color(0xFFF1F5F9) else EmeraldContainer
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isCaptureOnlyMode) NeutralBorder else EmeraldSuccess.copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (isCaptureOnlyMode) Icons.Default.CloudOff else Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = if (isCaptureOnlyMode) Color(0xFF475569) else EmeraldSuccess,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (isCaptureOnlyMode) "وضع التشغيل: التقاط ومراجعة فقط" else "وضع التشغيل: المزامنة السحابية مفعلة",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = TextPrimary
                            )
                            Text(
                                text = if (isCaptureOnlyMode) "الربط بالسيرفر غير مفعّل بعد (المرحلة الأولى - أرشفة محلية آمنة)" else "يتم إرسال العمليات المشفرة تلقائياً",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // 2. Notification Access Warning if not granted
            if (!isNotificationAccessGranted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = AmberContainer),
                        border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarning.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = AmberWarning,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تنبيه: إذن قراءة الإشعارات غير مفعل",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = AmberWarning
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "يحتاج التطبيق لإذن قراءة الإشعارات لالتقاط رسائل فودافون كاش والبنك الأهلي فور وصولها.",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Button(
                                onClick = {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                                    context.startActivity(intent)
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AmberWarning),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = "تفعيل إذن الإشعارات الآن",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            // 3. Metrics Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MetricCard(
                        title = "حالة الالتقاط",
                        value = if (isNotificationAccessGranted) "نشط ومفعل" else "متوقف",
                        subtitle = if (isNotificationAccessGranted) "يلتقط الرسائل المعتمدة" else "يتطلب منح الإذن",
                        icon = if (isNotificationAccessGranted) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                        accentColor = if (isNotificationAccessGranted) EmeraldSuccess else CoralError,
                        modifier = Modifier.weight(1f)
                    )

                    MetricCard(
                        title = "مدفوعات اليوم",
                        value = "${todayMetrics.eventCount} عملية",
                        subtitle = "${String.format(Locale.US, "%.2f", todayMetrics.totalAmountMajor)} ج.م",
                        icon = Icons.Default.Payments,
                        accentColor = BankAhlyTeal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // 4. Filter Chips
            item {
                Column {
                    Text(
                        text = "سجل الإشعارات الملتقطة",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(EventFilter.values()) { filter ->
                            FilterChip(
                                selected = selectedFilter == filter,
                                onClick = { viewModel.selectFilter(filter) },
                                label = { Text(filter.label) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = NavyPrimary,
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }

            // 5. Events List or Empty State
            if (events.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = androidx.compose.foundation.BorderStroke(1.dp, NeutralBorder)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(36.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد عمليات ملتقطة حالياً",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "سيتم تسجيل إشعارات التحويل الصالحة فور استلامها من VF-Cash أو Bank-AlAhly",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            } else {
                items(events, key = { it.eventId }) { event ->
                    EventItemCard(
                        entity = event,
                        onClick = { viewModel.selectEvent(event) }
                    )
                }
            }
        }
    }

    // Modal BottomSheet for Event Details
    selectedEvent?.let { eventEntity ->
        ModalBottomSheet(
            onDismissRequest = { viewModel.selectEvent(null) },
            sheetState = sheetState
        ) {
            EventDetailSheet(entity = eventEntity)
        }
    }
}
