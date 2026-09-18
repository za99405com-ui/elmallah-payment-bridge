package com.elmallah.paymentbridge.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import com.elmallah.paymentbridge.capture.RecentNotificationStore
import com.elmallah.paymentbridge.domain.SourceStatus
import com.elmallah.paymentbridge.ui.components.AppIconView
import com.elmallah.paymentbridge.ui.theme.AmberContainer
import com.elmallah.paymentbridge.ui.theme.AmberWarning
import com.elmallah.paymentbridge.ui.theme.CoralContainer
import com.elmallah.paymentbridge.ui.theme.CoralError
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.security.DeviceKeyManager
import com.elmallah.paymentbridge.ui.theme.AmberContainer
import com.elmallah.paymentbridge.ui.theme.AmberWarning
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val baseUrl by viewModel.apiBaseUrlInput.collectAsState()
    val uploadEnabled by viewModel.bridgeUploadEnabled.collectAsState()
    val rawDiagnosticsEnabled by viewModel.rawDiagnosticsEnabled.collectAsState()
    val isProvisioned by viewModel.isProvisioned.collectAsState()
    val isBgRunning by viewModel.isBgRunning.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val appTheme by viewModel.appTheme.collectAsState()
    val lastServerResponse by viewModel.lastServerResponse.collectAsState()
    val lastServerStatusCode by viewModel.lastServerStatusCode.collectAsState()

    val healthCheckStatus by viewModel.healthCheckStatus.collectAsState()
    val provisioningStatus by viewModel.provisioningStatus.collectAsState()
    val purgeResultStatus by viewModel.purgeResultStatus.collectAsState()
    val rulesSyncStatus by viewModel.rulesSyncStatus.collectAsState()

    var urlText by remember(baseUrl) { mutableStateOf(baseUrl) }
    var secretText by remember { mutableStateOf("") }

    var activeEditingRule by remember { mutableStateOf<PaymentSourceRule?>(null) }

    if (activeEditingRule != null) {
        SourceSetupScreen(
            initialRule = activeEditingRule,
            onSave = { savedRule ->
                viewModel.saveRule(savedRule)
                activeEditingRule = null
            },
            onBack = {
                activeEditingRule = null
            }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إعدادات جسر المدفوعات",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "الرجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. SERVER CONFIGURATION CARD
            item {
                SettingsSectionCard(
                    title = "إعدادات سيرفر admin3",
                    icon = Icons.Default.CloudDone
                ) {
                    Text(
                        text = "عنوان خادم الملاح (admin3) المعتمد لاستقبال الإشعارات والتحقق من صحتها:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عنوان API (Base URL)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val saved = viewModel.saveBaseUrl(urlText)
                                Toast.makeText(
                                    context,
                                    if (saved) "تم حفظ العنوان بنجاح" else "العنوان غير صالح (يجب أن يبدأ بـ https://)",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ العنوان")
                        }

                        OutlinedButton(
                            onClick = { viewModel.testServerHealth() },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("فحص الاتصال")
                        }
                    }

                    if (!healthCheckStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = healthCheckStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (!lastServerResponse.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "آخر رد: $lastServerResponse (كود $lastServerStatusCode)",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // 2. SECURITY & PROVISIONING CARD
            item {
                SettingsSectionCard(
                    title = "الأمان والتهيئة (Security & Keystore)",
                    icon = Icons.Default.Security
                ) {
                    // Device ID Display
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "معرف الجهاز المعتمد:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = viewModel.deviceId,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        IconButton(
                            onClick = {
                                clipboard.setText(AnnotatedString(viewModel.deviceId))
                                Toast.makeText(context, "تم نسخ معرف الجهاز", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(16.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Provisioning Status Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isProvisioned) EmeraldContainer else AmberContainer
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(if (isProvisioned) EmeraldSuccess else AmberWarning)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isProvisioned) "الجهاز مهيأ بمفتاح توقيع معتمد في Android Keystore" else "الجهاز بحاجة إلى تهيئة مفتاح HMAC من admin3",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (isProvisioned) EmeraldSuccess else AmberWarning
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = secretText,
                        onValueChange = { secretText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("المفتاح السري (HMAC Secret)") },
                        placeholder = { Text("أدخل المفتاح السري الصادر من لوحة admin3...") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val ok = viewModel.provisionSecret(secretText)
                            if (ok) secretText = ""
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("تثبيت المفتاح السري في Keystore")
                    }

                    if (!provisioningStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = provisioningStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "ملاحظة أمنية: لا يتم حفظ المفتاح كبيانات نصية ولا يُعرض في الواجهة أبداً بعد التثبيت.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 3. BACKGROUND OPERATIONS CARD
            item {
                SettingsSectionCard(
                    title = "التشغيل المستمر في الخلفية (Background Ops)",
                    icon = Icons.Default.Smartphone
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "خدمة المراقبة المستمرة (Foreground Service)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "تبقي التطبيق نشطاً لمنع النظام من إيقاف التقاط الإشعارات وإرسال نبضة القلب الدورية.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isBgRunning,
                            onCheckedChange = { viewModel.toggleBackgroundService(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("فتح إعدادات صلاحية الاستماع للإشعارات")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                            }
                            context.startActivity(intent)
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعدادات إشعارات خدمة الخلفية")
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إعدادات تحسين البطارية للتشغيل المستمر")
                    }
                }
            }

            // 4. مصادر إشعارات الدفع (Payment Notification Sources)
            item {
                SettingsSectionCard(
                    title = "مصادر إشعارات الدفع",
                    icon = Icons.Default.Payment
                ) {
                    PaymentNotificationSourcesSection(
                        rules = rules,
                        hasSynced = viewModel.hasSyncedWithServer,
                        lastSyncTimestamp = viewModel.lastSyncTimestamp,
                        hasUnsavedChanges = viewModel.hasUnsavedChanges,
                        syncStatus = rulesSyncStatus,
                        onRefreshFromServer = { viewModel.fetchRulesFromServer() },
                        onEditRule = { rule -> activeEditingRule = rule }
                    )
                }
            }

            // 5. SYNC & PRIVACY CONTROLS CARD
            item {
                SettingsSectionCard(
                    title = "المزامنة والخصوصية (Sync & Privacy)",
                    icon = Icons.Default.CloudSync
                ) {
                    // Upload Enabled
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تفعيل رفع الإشعارات إلى admin3",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "في حال الإيقاف، سيتم تسجيل الوقائع محلياً فقط في قاعدة بيانات الجهاز دون إرسالها.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uploadEnabled,
                            onCheckedChange = { viewModel.setBridgeUploadEnabled(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Raw Diagnostics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "حفظ نصوص الرسائل الخام للتشخيص الميداني",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "يسمح بحفظ مقتطف نص الإشعار محلياً للمراجعة الفنية عند حدوث أخطاء قراءة.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = rawDiagnosticsEnabled,
                            onCheckedChange = { viewModel.toggleRawDiagnostics(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Clear raw snippets
                    OutlinedButton(
                        onClick = { viewModel.clearAllRawSnippets() },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("مسح جميع النصوص الخام المخزنة محلياً")
                    }

                    if (!purgeResultStatus.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = purgeResultStatus ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // 6. APPEARANCE SETTINGS CARD
            item {
                SettingsSectionCard(
                    title = "المظهر والسمة (Appearance)",
                    icon = Icons.Default.ColorLens
                ) {
                    Text(
                        text = "اختر وضع العرض المناسب لبيئة العمل:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeOptionChip(
                            label = "تلقائي (النظام)",
                            isSelected = appTheme == DeviceKeyManager.THEME_SYSTEM,
                            onClick = { viewModel.setAppTheme(DeviceKeyManager.THEME_SYSTEM) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "فاتح (Light)",
                            isSelected = appTheme == DeviceKeyManager.THEME_LIGHT,
                            onClick = { viewModel.setAppTheme(DeviceKeyManager.THEME_LIGHT) },
                            modifier = Modifier.weight(1f)
                        )
                        ThemeOptionChip(
                            label = "داكن (Dark)",
                            isSelected = appTheme == DeviceKeyManager.THEME_DARK,
                            onClick = { viewModel.setAppTheme(DeviceKeyManager.THEME_DARK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PaymentNotificationSourcesSection(
    rules: List<PaymentSourceRule>,
    hasSynced: Boolean,
    lastSyncTimestamp: Long,
    hasUnsavedChanges: Boolean,
    syncStatus: String?,
    onRefreshFromServer: () -> Unit,
    onEditRule: (PaymentSourceRule) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Sync header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                if (!hasSynced) {
                    Text(
                        text = "لم تتم مزامنة إعدادات مصادر الدفع بعد",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "آخر مزامنة: ${RecentNotificationStore.formatRelativeTime(lastSyncTimestamp)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (hasUnsavedChanges) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = AmberContainer
                            ) {
                                Text(
                                    text = "تعديلات غير محفوظة",
                                    color = AmberWarning,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = onRefreshFromServer,
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("تحديث من السيرفر", fontSize = 12.sp)
            }
        }

        if (!syncStatus.isNullOrBlank()) {
            Text(
                text = syncStatus,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        if (rules.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (!hasSynced) "لا توجد مصادر دفع محددة بعد." else "لم يتم تعيين مصادر دفع في لوحة التحكم.",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أضف المصدر واربطه بهذا الجهاز من لوحة التحكم، ثم اضغط «تحديث من السيرفر».",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            rules.forEach { rule ->
                PaymentSourceCard(
                    rule = rule,
                    onEdit = { onEditRule(rule) }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        OutlinedButton(
            onClick = onRefreshFromServer,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("مزامنة المصادر المعتمدة من لوحة التحكم", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun PaymentSourceCard(
    rule: PaymentSourceRule,
    onEdit: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    AppIconView(
                        packageName = rule.packageNames.firstOrNull(),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = rule.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "التطبيق: ${rule.friendlyDisplayAppName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                SourceStatusBadge(status = rule.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نماذج الرسائل: ${rule.sampleMessages.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (rule.status == SourceStatus.MISSING_APP || rule.status == SourceStatus.NO_SAMPLES) {
                    Button(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("إعداد", fontSize = 12.sp)
                    }
                } else {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("تعديل", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceStatusBadge(status: SourceStatus) {
    val (bgColor, textColor) = when (status) {
        SourceStatus.READY -> EmeraldContainer to EmeraldSuccess
        SourceStatus.LOCAL_DRAFT -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        SourceStatus.MISSING_APP -> AmberContainer to AmberWarning
        SourceStatus.NO_SAMPLES -> CoralContainer to CoralError
        SourceStatus.NEEDS_TEST -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        SourceStatus.DISABLED_BY_ADMIN -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = bgColor
    ) {
        Text(
            text = status.label,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ThemeOptionChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = { Text(label, fontSize = 12.sp, maxLines = 1) },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}

@Composable
private fun SettingsSectionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}
