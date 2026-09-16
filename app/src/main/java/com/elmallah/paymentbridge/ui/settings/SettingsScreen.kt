package com.elmallah.paymentbridge.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.capture.PaymentNotificationListener
import com.elmallah.paymentbridge.ui.theme.AmberContainer
import com.elmallah.paymentbridge.ui.theme.AmberWarning
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess
import com.elmallah.paymentbridge.ui.theme.NavyPrimary
import com.elmallah.paymentbridge.ui.theme.NeutralBorder
import com.elmallah.paymentbridge.ui.theme.TextMuted
import com.elmallah.paymentbridge.ui.theme.TextPrimary
import com.elmallah.paymentbridge.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val baseUrl by viewModel.apiBaseUrlInput.collectAsState()
    val uploadEnabled by viewModel.bridgeUploadEnabled.collectAsState()
    val vfCashEnabled by viewModel.vfCashEnabled.collectAsState()
    val bankEnabled by viewModel.bankAlAhlyEnabled.collectAsState()
    val rawDiagnosticsEnabled by viewModel.rawDiagnosticsEnabled.collectAsState()
    val serverBusy by viewModel.serverBusy.collectAsState()
    val busySessionId by viewModel.busySessionId.collectAsState()
    val lastHeartbeat by viewModel.lastHeartbeatTimestamp.collectAsState()
    val healthStatus by viewModel.healthCheckStatus.collectAsState()
    val provisioningStatus by viewModel.provisioningStatus.collectAsState()
    val purgeStatus by viewModel.purgeResultStatus.collectAsState()

    var urlDraft by remember(baseUrl) { mutableStateOf(baseUrl) }
    var provisioningSecret by remember { mutableStateOf("") }
    var now by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            viewModel.refreshServerState()
            delay(2_000)
        }
    }

    val listenerConnected = PaymentNotificationListener.isConnected
    val heartbeatFresh = lastHeartbeat > 0L && now - lastHeartbeat <= 45_000L
    val online = uploadEnabled && listenerConnected && heartbeatFresh

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Payment Bridge — Phase 2", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Smartphone, contentDescription = null, tint = NavyPrimary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("حالة الجهاز", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(viewModel.deviceId, fontSize = 11.sp, color = TextMuted)
                        }
                        StatusBadge(if (online) "Online" else "Offline", online)
                        Spacer(Modifier.width(6.dp))
                        StatusBadge(if (serverBusy) "Busy" else "Available", !serverBusy)
                    }

                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Notification Listener: ${if (listenerConnected) "Connected" else "Disconnected"}",
                        fontSize = 12.sp,
                        color = if (listenerConnected) EmeraldSuccess else AmberWarning
                    )
                    Text(
                        if (busySessionId.isNullOrBlank()) "لا توجد جلسة محجوزة حالياً" else "الجلسة المحجوزة: $busySessionId",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Text(
                        if (lastHeartbeat == 0L) "لم يتم إرسال Heartbeat ناجح بعد" else "آخر Heartbeat منذ ${(now - lastHeartbeat).coerceAtLeast(0) / 1000} ثانية",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                }
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CloudDone, contentDescription = null, tint = NavyPrimary)
                        Spacer(Modifier.width(8.dp))
                        Column(Modifier.weight(1f)) {
                            Text("الإرسال للسيرفر", fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text("الأحداث تُرسل فقط؛ admin3 هو الذي يقرر المطابقة والتأكيد.", fontSize = 11.sp, color = TextMuted)
                        }
                        Switch(checked = uploadEnabled, onCheckedChange = viewModel::setBridgeUploadEnabled)
                    }
                }
            }

            item {
                SectionCard {
                    Text("وسائل الدفع المفعلة على هذا الجهاز", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(10.dp))
                    ToggleRow(
                        title = "Vodafone Cash",
                        description = "استقبال وتحليل وإرسال إشعارات VF-Cash",
                        checked = vfCashEnabled,
                        onCheckedChange = viewModel::setVfCashEnabled
                    )
                    HorizontalDivider(Modifier.padding(vertical = 10.dp), color = NeutralBorder)
                    ToggleRow(
                        title = "Bank AlAhly / NBE",
                        description = "استقبال التحويلات البنكية المدعومة من parser",
                        checked = bankEnabled,
                        onCheckedChange = viewModel::setBankAlAhlyEnabled
                    )
                }
            }

            item {
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Key, contentDescription = null, tint = NavyPrimary)
                        Spacer(Modifier.width(8.dp))
                        Text("تهيئة HMAC مع admin3", fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "سجّل Device ID في لوحة admin3، ثم انسخ المفتاح الذي يظهر مرة واحدة والصقه هنا. يُحفظ مشفراً داخل Android Keystore.",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = provisioningSecret,
                        onValueChange = { provisioningSecret = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("One-time provisioning secret") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation()
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = {
                            val ok = viewModel.provisionSecret(provisioningSecret)
                            if (ok) {
                                provisioningSecret = ""
                                Toast.makeText(context, "تم حفظ مفتاح الجهاز بأمان", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("حفظ مفتاح التهيئة")
                    }
                    provisioningStatus?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(it, fontSize = 11.sp, color = if (it.startsWith("تم")) EmeraldSuccess else AmberWarning)
                    }
                }
            }

            item {
                SectionCard {
                    Text("عنوان elmallah-admin3", fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlDraft,
                        onValueChange = { urlDraft = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Base URL") },
                        placeholder = { Text("https://admin.example.com") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val ok = viewModel.saveBaseUrl(urlDraft)
                                Toast.makeText(context, if (ok) "تم حفظ العنوان" else "العنوان غير صالح", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("حفظ")
                        }
                        OutlinedButton(onClick = viewModel::testServerHealth, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.NetworkCheck, contentDescription = null)
                            Spacer(Modifier.width(5.dp))
                            Text("فحص")
                        }
                    }
                    healthStatus?.let {
                        Spacer(Modifier.height(8.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (it.startsWith("نجح")) EmeraldContainer else AmberContainer
                        ) {
                            Text(
                                it,
                                modifier = Modifier.fillMaxWidth().padding(10.dp),
                                fontSize = 11.sp,
                                color = if (it.startsWith("نجح")) EmeraldSuccess else AmberWarning
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    ToggleRow(
                        title = "حفظ النص الخام للتشخيص المحلي",
                        description = "معطل افتراضياً؛ لا يُرسل النص الخام للسيرفر ويُحذف تلقائياً بعد 7 أيام.",
                        checked = rawDiagnosticsEnabled,
                        onCheckedChange = viewModel::toggleRawDiagnostics
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(onClick = viewModel::clearAllRawSnippets, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.CleaningServices, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("مسح النصوص الخام المخزنة")
                    }
                    purgeStatus?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it, fontSize = 11.sp, color = EmeraldSuccess)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, NeutralBorder)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), content = content)
    }
}

@Composable
private fun ToggleRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = TextPrimary)
            Text(description, fontSize = 11.sp, color = TextMuted)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun StatusBadge(text: String, positive: Boolean) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (positive) EmeraldContainer else AmberContainer
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = if (positive) EmeraldSuccess else AmberWarning
        )
    }
}
