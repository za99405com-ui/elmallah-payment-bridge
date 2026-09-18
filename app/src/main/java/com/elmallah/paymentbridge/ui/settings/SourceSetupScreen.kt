package com.elmallah.paymentbridge.ui.settings

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Save
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
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.elmallah.paymentbridge.domain.PaymentMessageSample
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.parser.AutoRuleGenerator
import com.elmallah.paymentbridge.ui.components.AppIconView
import com.elmallah.paymentbridge.ui.components.InstalledAppHelper
import com.elmallah.paymentbridge.ui.components.InstalledAppItem
import com.elmallah.paymentbridge.ui.components.RecentNotificationPickerSheet
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess
import com.elmallah.paymentbridge.ui.theme.NavyPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSetupScreen(
    initialRule: PaymentSourceRule?,
    onSave: (PaymentSourceRule) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val logicalCode = initialRule?.code ?: ""
    val logicalName = when (logicalCode) {
        "vf_cash" -> "فودافون كاش"
        "instapay" -> "إنستا باي"
        else -> initialRule?.name ?: "مصدر الدفع"
    }

    var selectedPackage by remember { mutableStateOf(initialRule?.packageNames?.firstOrNull() ?: "") }
    var selectedAppName by remember {
        mutableStateOf(initialRule?.appName ?: initialRule?.friendlyDisplayAppName ?: "")
    }
    var senderTitle by remember {
        mutableStateOf(
            initialRule?.sampleMessages?.firstOrNull()?.title
                ?: initialRule?.senderFilters?.firstOrNull()
                ?: ""
        )
    }
    var messageBody by remember {
        mutableStateOf(initialRule?.sampleMessages?.firstOrNull()?.body ?: "")
    }
    var showApps by remember { mutableStateOf(false) }
    var appSearch by remember { mutableStateOf("") }
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }
    var showRecentSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(showApps, appSearch) {
        if (showApps) {
            installedApps = InstalledAppHelper.getInstalledApps(
                context = context,
                showAll = true,
                searchQuery = appSearch
            )
        }
    }

    val analysis = remember(messageBody, senderTitle) {
        if (messageBody.isBlank()) null
        else AutoRuleGenerator.analyzeSample(senderTitle, messageBody)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("إعداد $logicalName", fontWeight = FontWeight.Bold, color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NavyPrimary)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("إعداد بسيط من خطوتين", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    "اختر تطبيق الرسائل الذي يصل عليه الإشعار، ثم ضع رسالة استلام حقيقية كاملة. التطبيق سيتولى قراءة المبلغ والمرجع تلقائياً.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Smartphone, contentDescription = null)
                            Column {
                                Text("1. تطبيق الرسائل", fontWeight = FontWeight.Bold)
                                Text(
                                    if (selectedPackage.isBlank()) "لم يتم اختيار تطبيق"
                                    else selectedAppName.ifBlank { selectedPackage },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (selectedPackage.isNotBlank()) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                tonalElevation = 1.dp,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    AppIconView(packageName = selectedPackage, modifier = Modifier.size(34.dp))
                                    Column {
                                        Text(selectedAppName.ifBlank { "تطبيق الرسائل" }, fontWeight = FontWeight.SemiBold)
                                        Text(
                                            selectedPackage,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        OutlinedButton(
                            onClick = { showApps = !showApps },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(if (showApps) "إغلاق قائمة التطبيقات" else "اختيار تطبيق الرسائل")
                        }

                        if (showApps) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = appSearch,
                                onValueChange = { appSearch = it },
                                label = { Text("ابحث عن Messages / الرسائل") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                installedApps.take(12).forEach { app ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable {
                                                selectedPackage = app.packageName
                                                selectedAppName = app.appName
                                                showApps = false
                                            }
                                            .padding(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        AppIconView(packageName = app.packageName, modifier = Modifier.size(30.dp))
                                        Column {
                                            Text(app.appName, fontWeight = FontWeight.Medium)
                                            Text(
                                                app.packageName,
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Default.Message, contentDescription = null)
                            Column {
                                Text("2. رسالة $logicalName", fontWeight = FontWeight.Bold)
                                Text(
                                    "ضع الرسالة كاملة كما تصل على الهاتف.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { showRecentSheet = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.size(6.dp))
                            Text("اختيار رسالة وصلت على الهاتف")
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedTextField(
                            value = messageBody,
                            onValueChange = { messageBody = it },
                            label = { Text("نص الرسالة بالكامل") },
                            placeholder = { Text("الصق رسالة الاستلام هنا...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        analysis?.let { result ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (result.success) EmeraldSuccess else MaterialTheme.colorScheme.error
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (result.success) EmeraldContainer
                                    else MaterialTheme.colorScheme.errorContainer
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        if (result.success) {
                                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = EmeraldSuccess)
                                        }
                                        Text(
                                            if (result.success) "تم التعرف على الرسالة" else "الرسالة تحتاج تعديل",
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        if (result.success)
                                            "المبلغ المقروء: \${result.formattedAmount ?: "تم التعرف عليه"}"
                                        else result.statusMessage,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Button(
                    onClick = {
                        val sample = PaymentMessageSample(title = senderTitle, body = messageBody)
                        val ruleId = initialRule?.id ?: return@Button
                        val generated = AutoRuleGenerator.buildRuleFromSamples(
                            existingRule = initialRule,
                            ruleId = ruleId,
                            sourceName = logicalName,
                            selectedPackage = selectedPackage,
                            appName = selectedAppName,
                            samples = listOf(sample)
                        )

                        val signature = when {
                            logicalCode == "vf_cash" && messageBody.contains("تم استلام", ignoreCase = true) ->
                                listOf("تم استلام")
                            logicalCode == "instapay" && messageBody.contains("تم إضافة تحويل لحظي", ignoreCase = true) ->
                                listOf("تم إضافة تحويل لحظي")
                            logicalCode == "instapay" && messageBody.contains("تم اضافه تحويل لحظي", ignoreCase = true) ->
                                listOf("تم اضافه تحويل لحظي")
                            logicalCode == "instapay" && messageBody.contains("تم إضافة تحويل", ignoreCase = true) ->
                                listOf("تم إضافة تحويل")
                            else -> generated.bodyContains
                        }

                        onSave(
                            generated.copy(
                                code = initialRule.code,
                                name = logicalName,
                                senderFilters = emptyList(),
                                bodyContains = signature,
                                parserType = "regex",
                                isLocalDraft = true,
                                lastTestedSuccess = true
                            )
                        )
                    },
                    enabled = selectedPackage.isNotBlank() && messageBody.isNotBlank() && analysis?.success == true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("حفظ إعداد $logicalName", fontWeight = FontWeight.Bold)
                }
            }

            item {
                Text(
                    "لن تظهر لك إعدادات Regex أو فلاتر تقنية. إذا تغيّر شكل رسالة البنك أو فودافون، عد إلى هنا واختر الرسالة الجديدة فقط.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showRecentSheet) {
        RecentNotificationPickerSheet(
            sheetState = sheetState,
            onDismiss = { showRecentSheet = false },
            onSelectNotification = { picked ->
                selectedPackage = picked.packageName
                selectedAppName = picked.appName
                senderTitle = picked.title
                messageBody = picked.body
                showRecentSheet = false
            }
        )
    }
}
