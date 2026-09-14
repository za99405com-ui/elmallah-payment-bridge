package com.example.paymentbridge.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.paymentbridge.capture.NotificationPermissionHelper
import com.example.paymentbridge.ui.components.EventItemCard
import com.example.paymentbridge.ui.components.MetricCard
import com.example.paymentbridge.ui.events.EventDetailSheet
import com.example.ui.theme.BankAhlyTeal
import com.example.ui.theme.Emerald600
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusNeutral
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.StatusWarning
import java.text.SimpleDateFormat
import java.util.Date
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
    val serverStatus by viewModel.serverConnectionState.collectAsState()
    val todayMetrics by viewModel.todayMetrics.collectAsState()
    val lastSyncTime by viewModel.lastSyncTime.collectAsState()
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val events by viewModel.filteredEvents.collectAsState()
    val selectedEvent by viewModel.selectedEventForDetail.collectAsState()

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Check notification access each time app returns to foreground
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
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "الجسر الآمن مع elmallah-admin3",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToParserTest) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "اختبار قراءة الرسائل"
                        )
                    }
                    IconButton(onClick = { viewModel.syncNow() }) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "مزامنة الآن"
                        )
                    }
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 4 Status Indicator Cards
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 1. Notification Access Card
                        val (notifValue, notifSub, notifColor, notifIcon) = if (isNotificationAccessGranted) {
                            Quad("تعمل بنجاح", "التقاط فوري للإشعارات", StatusSuccess, Icons.Default.Notifications)
                        } else {
                            Quad("غير مفعلة ⚠️", "اضغط للتفعيل بالنظام", StatusError, Icons.Default.NotificationsOff)
                        }

                        MetricCard(
                            title = "خدمة مراقبة الإشعارات",
                            value = notifValue,
                            subtitle = notifSub,
                            icon = notifIcon,
                            accentColor = notifColor,
                            onClick = {
                                if (!isNotificationAccessGranted) {
                                    NotificationPermissionHelper.openNotificationAccessSettings(context)
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )

                        // 2. Server Connectivity Card
                        val (serverValue, serverSub, serverColor, serverIcon) = when (serverStatus) {
                            is ServerConnectionState.Connected -> Quad("متصل بالسيرفر", "جاهز للمطابقة", StatusSuccess, Icons.Default.CloudDone)
                            is ServerConnectionState.Checking -> Quad("جارٍ الفحص...", "جسر الاتصال", StatusWarning, Icons.Default.Refresh)
                            is ServerConnectionState.Disconnected -> Quad("تعذر الاتصال ❌", "اضغط لإعادة الفحص", StatusError, Icons.Default.CloudOff)
                            ServerConnectionState.Unknown -> Quad("فحص السيرفر", "اضغط للفحص", StatusNeutral, Icons.Default.Sync)
                        }

                        MetricCard(
                            title = "الاتصال بالسيرفر",
                            value = serverValue,
                            subtitle = serverSub,
                            icon = serverIcon,
                            accentColor = serverColor,
                            onClick = { viewModel.checkServerConnection() },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 3. Last Sync Card
                        val syncTimestamp: Long? = lastSyncTime
                        val timeStr = if (syncTimestamp != null && syncTimestamp > 0L) {
                            SimpleDateFormat("hh:mm a", Locale("ar")).format(Date(syncTimestamp))
                        } else {
                            "لا يوجد"
                        }
                        MetricCard(
                            title = "آخر مزامنة",
                            value = timeStr,
                            subtitle = if (lastSyncTime != null) "آخر إرسال ناجح" else "بانتظار أول عملية",
                            icon = Icons.Default.Sync,
                            accentColor = BankAhlyTeal,
                            onClick = { viewModel.syncNow() },
                            modifier = Modifier.weight(1f)
                        )

                        // 4. Today's Payments Card
                        MetricCard(
                            title = "مدفوعات اليوم",
                            value = "${todayMetrics.count} عملية",
                            subtitle = String.format(Locale.US, "%.2f ج.م", todayMetrics.totalAmountMajor),
                            icon = Icons.Default.Payments,
                            accentColor = Emerald600,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Section Header & Filters
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "سجل المدفوعات الواردة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${events.size} عملية",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    EventFilter.values().forEach { filter ->
                        FilterChip(
                            selected = selectedFilter == filter,
                            onClick = { viewModel.setFilter(filter) },
                            label = { Text(filter.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Events List or Empty State
            if (events.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Payments,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "لا توجد عمليات ملتقطة حالياً",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "سيتم التقاط أي إشعار دفع وارد من فودافون كاش أو البنك الأهلي تلقائياً بمجرد وصوله للهاتف.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                items(events, key = { it.eventId }) { event ->
                    EventItemCard(
                        event = event,
                        onClick = { viewModel.selectEvent(event) }
                    )
                }
            }
        }
    }

    // Detail BottomSheet
    if (selectedEvent != null) {
        EventDetailSheet(
            event = selectedEvent,
            sheetState = sheetState,
            onDismiss = { viewModel.selectEvent(null) },
            onRetryUpload = { eventId -> viewModel.retryUpload(eventId) }
        )
    }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
