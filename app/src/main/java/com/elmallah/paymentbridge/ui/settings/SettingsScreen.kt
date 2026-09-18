package com.elmallah.paymentbridge.ui.settings

import android.content.Intent
import android.provider.Settings
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.ui.components.AppIconView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    val baseUrl by viewModel.apiBaseUrlInput.collectAsState()
    val isProvisioned by viewModel.isProvisioned.collectAsState()
    val isBgRunning by viewModel.isBgRunning.collectAsState()
    val rules by viewModel.rules.collectAsState()
    val lastServerResponse by viewModel.lastServerResponse.collectAsState()
    val lastServerStatusCode by viewModel.lastServerStatusCode.collectAsState()
    val healthCheckStatus by viewModel.healthCheckStatus.collectAsState()
    val provisioningStatus by viewModel.provisioningStatus.collectAsState()
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
            onBack = { activeEditingRule = null }
        )
        return
    }

    val paymentRules = rules
        .filter {
            it.code == "vf_cash" ||
                it.code == "instapay" ||
                (it.code == null && (it.name.contains("فودافون") || it.name.contains("إنستا")))
        }
        .sortedBy {
            when (it.code) {
                "vf_cash" -> 0
                "instapay" -> 1
                else -> 2
            }
        }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعدادات جسر المدفوعات", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
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
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.CloudDone, contentDescription = null)
                            Column {
                                Text("الاتصال بلوحة التحكم", fontWeight = FontWeight.Bold)
                                Text(
                                    "هذه البيانات تُضبط مرة واحدة فقط.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = urlText,
                            onValueChange = { urlText = it },
                            label = { Text("عنوان admin3") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    val ok = viewModel.saveBaseUrl(urlText)
                                    Toast.makeText(
                                        context,
                                        if (ok) "تم حفظ العنوان" else "العنوان غير صالح",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.size(5.dp))
                                Text("حفظ")
                            }
                            OutlinedButton(
                                onClick = { viewModel.testServerHealth() },
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.NetworkCheck, contentDescription = null, modifier = Modifier.size(17.dp))
                                Spacer(modifier = Modifier.size(5.dp))
                                Text("فحص")
                            }
                        }

                        if (!healthCheckStatus.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(healthCheckStatus ?: "", style = MaterialTheme.typography.bodySmall)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("معرف الجهاز", style = MaterialTheme.typography.labelSmall)
                                    Text(
                                        viewModel.deviceId,
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
                                    Icon(Icons.Default.ContentCopy, contentDescription = "نسخ")
                                }
                            }
                        }

                        if (!isProvisioned) {
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedTextField(
                                value = secretText,
                                onValueChange = { secretText = it },
                                label = { Text("HMAC Secret") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = {
                                    val ok = viewModel.provisionSecret(secretText)
                                    if (ok) secretText = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null)
                                Spacer(modifier = Modifier.size(6.dp))
                                Text("ربط الجهاز")
                            }
                        }

                        if (!provisioningStatus.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(provisioningStatus ?: "", style = MaterialTheme.typography.bodySmall)
                        }

                        if (!lastServerResponse.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "آخر اتصال: $lastServerResponse (HTTP $lastServerStatusCode)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Column {
                                Text("تشغيل المراقبة", fontWeight = FontWeight.Bold)
                                Text(
                                    if (isBgRunning) "الخدمة تعمل في الخلفية" else "الخدمة متوقفة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { viewModel.toggleBackgroundService(!isBgRunning) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Smartphone, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text(if (isBgRunning) "إيقاف خدمة الخلفية" else "تشغيل خدمة الخلفية")
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedButton(
                            onClick = {
                                context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.NotificationsActive, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("صلاحية قراءة الإشعارات")
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Message, contentDescription = null)
                            Column {
                                Text("رسائل الدفع", fontWeight = FontWeight.Bold)
                                Text(
                                    "مصدران فقط: فودافون كاش وإنستا باي.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (paymentRules.isEmpty()) {
                            Text(
                                "لم تصل إعدادات طرق الدفع من لوحة التحكم بعد.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        } else {
                            paymentRules.forEach { rule ->
                                SimplePaymentRuleCard(
                                    rule = rule,
                                    onEdit = { activeEditingRule = rule }
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                            }
                        }

                        OutlinedButton(
                            onClick = { viewModel.fetchRulesFromServer() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("تحديث من لوحة التحكم")
                        }

                        if (!rulesSyncStatus.isNullOrBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(rulesSyncStatus ?: "", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SimplePaymentRuleCard(
    rule: PaymentSourceRule,
    onEdit: () -> Unit
) {
    val title = when (rule.code) {
        "vf_cash" -> "فودافون كاش"
        "instapay" -> "إنستا باي"
        else -> rule.name
    }
    val configured = rule.packageNames.isNotEmpty() &&
        (!rule.amountExtractionRegex.isNullOrBlank() || rule.sampleMessages.isNotEmpty())

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit),
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (rule.packageNames.isNotEmpty()) {
                AppIconView(rule.packageNames.first(), modifier = Modifier.size(38.dp))
            } else {
                Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(38.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold)
                Text(
                    when {
                        !rule.enabled -> "متوقف من لوحة التحكم"
                        configured -> "تم تحديد تطبيق الرسائل وشكل الرسالة"
                        else -> "اضغط لتحديد تطبيق الرسائل والرسالة"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (rule.packageNames.isNotEmpty()) {
                    Text(
                        rule.friendlyDisplayAppName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Text(
                if (!rule.enabled) "متوقف" else if (configured) "جاهز" else "إعداد",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = when {
                    !rule.enabled -> MaterialTheme.colorScheme.error
                    configured -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
        }
    }
}
