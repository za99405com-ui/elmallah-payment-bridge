package com.elmallah.paymentbridge.ui.settings

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.domain.PaymentMessageSample
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.parser.AutoDetectionResult
import com.elmallah.paymentbridge.parser.AutoRuleGenerator
import com.elmallah.paymentbridge.ui.components.AppIconView
import com.elmallah.paymentbridge.ui.components.InstalledAppHelper
import com.elmallah.paymentbridge.ui.components.InstalledAppItem
import com.elmallah.paymentbridge.ui.components.RecentNotificationPickerSheet
import com.elmallah.paymentbridge.ui.theme.EmeraldContainer
import com.elmallah.paymentbridge.ui.theme.EmeraldSuccess
import com.elmallah.paymentbridge.ui.theme.NavyPrimary
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SourceSetupScreen(
    initialRule: PaymentSourceRule?,
    onSave: (PaymentSourceRule) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentStep by remember { mutableIntStateOf(1) }

    // Source properties
    var sourceName by remember {
        mutableStateOf(initialRule?.name ?: "")
    }
    var selectedPackage by remember {
        mutableStateOf(initialRule?.packageNames?.firstOrNull() ?: "")
    }
    var selectedAppName by remember {
        mutableStateOf(initialRule?.appName ?: initialRule?.friendlyDisplayAppName ?: "")
    }

    // Step 1: App search & filter
    var appSearchQuery by remember { mutableStateOf("") }
    var showAllApps by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledAppItem>>(emptyList()) }

    LaunchedEffect(showAllApps, appSearchQuery) {
        installedApps = InstalledAppHelper.getInstalledApps(context, showAllApps, appSearchQuery)
    }

    // Step 2: Message samples
    var samples by remember {
        mutableStateOf(
            initialRule?.sampleMessages?.ifEmpty {
                listOf(PaymentMessageSample(title = "", body = ""))
            } ?: listOf(PaymentMessageSample(title = "", body = ""))
        )
    }

    // Recent notifications picker sheet
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showRecentSheet by remember { mutableStateOf(false) }
    var activeSampleIndexForPicker by remember { mutableIntStateOf(-1) }

    // Step 3: Analysis results
    var sampleAnalysisResults by remember {
        mutableStateOf<Map<String, AutoDetectionResult>>(emptyMap())
    }

    fun runAnalysis() {
        val map = mutableMapOf<String, AutoDetectionResult>()
        samples.forEach { sample ->
            if (sample.body.isNotBlank()) {
                map[sample.id] = AutoRuleGenerator.analyzeSample(sample.title, sample.body)
            }
        }
        sampleAnalysisResults = map
    }

    // Step 4: Advanced settings (collapsed by default)
    var isAdvancedExpanded by remember { mutableStateOf(false) }
    var advancedPackages by remember {
        mutableStateOf(initialRule?.packageNames?.joinToString(", ") ?: selectedPackage)
    }
    var advancedTitleContains by remember {
        mutableStateOf(initialRule?.titleContains?.joinToString(", ") ?: "")
    }
    var advancedBodyContains by remember {
        mutableStateOf(initialRule?.bodyContains?.joinToString(", ") ?: "")
    }
    var advancedSenderAliases by remember {
        mutableStateOf(initialRule?.senderFilters?.joinToString(", ") ?: "")
    }
    var advancedAmountRegex by remember {
        mutableStateOf(initialRule?.amountExtractionRegex ?: "")
    }
    var advancedPhoneRegex by remember {
        mutableStateOf(initialRule?.senderPhoneExtractionRegex ?: "")
    }
    var advancedAccountRegex by remember {
        mutableStateOf(initialRule?.accountIdentifierRegex ?: "")
    }
    var advancedParserType by remember {
        mutableStateOf(initialRule?.parserType ?: "RULE_BASED")
    }
    var advancedPriority by remember {
        mutableIntStateOf(initialRule?.priority ?: 100)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (initialRule == null) "إضافة مصدر دفع جديد" else "تعديل مصدر: ${initialRule.name}",
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Step Progress Indicator Bar
            StepProgressHeader(currentStep = currentStep)

            // Step Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (currentStep) {
                    1 -> Step1ChooseApp(
                        installedApps = installedApps,
                        selectedPackage = selectedPackage,
                        searchQuery = appSearchQuery,
                        onSearchChange = { appSearchQuery = it },
                        showAllApps = showAllApps,
                        onToggleShowAll = { showAllApps = it },
                        onSelectApp = { app ->
                            selectedPackage = app.packageName
                            selectedAppName = app.appName
                            if (sourceName.isBlank()) {
                                sourceName = app.appName
                            }
                            advancedPackages = app.packageName
                        }
                    )
                    2 -> Step2MessageSamples(
                        samples = samples,
                        onAddSample = {
                            if (samples.size < 10) {
                                samples = samples + PaymentMessageSample(title = "", body = "")
                            }
                        },
                        onUpdateSample = { idx, updated ->
                            val list = samples.toMutableList()
                            list[idx] = updated
                            samples = list
                        },
                        onDeleteSample = { idx ->
                            if (samples.size > 1) {
                                samples = samples.filterIndexed { i, _ -> i != idx }
                            } else {
                                samples = listOf(PaymentMessageSample(title = "", body = ""))
                            }
                        },
                        onOpenRecentPicker = { idx ->
                            activeSampleIndexForPicker = idx
                            showRecentSheet = true
                        }
                    )
                    3 -> Step3AutoDetectionTest(
                        samples = samples,
                        analysisResults = sampleAnalysisResults,
                        onRunAnalysis = { runAnalysis() }
                    )
                    4 -> Step4SaveAndAdvanced(
                        sourceName = sourceName,
                        onSourceNameChange = { sourceName = it },
                        appName = selectedAppName,
                        packageName = selectedPackage,
                        samplesCount = samples.count { it.body.isNotBlank() },
                        isAdvancedExpanded = isAdvancedExpanded,
                        onToggleAdvanced = { isAdvancedExpanded = !isAdvancedExpanded },
                        advancedPackages = advancedPackages,
                        onPackagesChange = { advancedPackages = it },
                        advancedTitleContains = advancedTitleContains,
                        onTitleContainsChange = { advancedTitleContains = it },
                        advancedBodyContains = advancedBodyContains,
                        onBodyContainsChange = { advancedBodyContains = it },
                        advancedSenderAliases = advancedSenderAliases,
                        onSenderAliasesChange = { advancedSenderAliases = it },
                        advancedAmountRegex = advancedAmountRegex,
                        onAmountRegexChange = { advancedAmountRegex = it },
                        advancedPhoneRegex = advancedPhoneRegex,
                        onPhoneRegexChange = { advancedPhoneRegex = it },
                        advancedAccountRegex = advancedAccountRegex,
                        onAccountRegexChange = { advancedAccountRegex = it },
                        advancedParserType = advancedParserType,
                        onParserTypeChange = { advancedParserType = it },
                        advancedPriority = advancedPriority,
                        onPriorityChange = { advancedPriority = it }
                    )
                }
            }

            // Bottom Navigation Actions
            Surface(
                tonalElevation = 6.dp,
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentStep > 1) {
                        OutlinedButton(
                            onClick = { currentStep -= 1 },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("السابق")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (currentStep < 4) {
                        val canProceed = when (currentStep) {
                            1 -> selectedPackage.isNotBlank()
                            2 -> samples.any { it.body.isNotBlank() }
                            3 -> true
                            else -> true
                        }
                        Button(
                            onClick = {
                                if (currentStep == 2) {
                                    runAnalysis()
                                }
                                currentStep += 1
                            },
                            enabled = canProceed,
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = when (currentStep) {
                                    1 -> "التالي: نماذج الرسائل"
                                    2 -> "التالي: اختبار الاستخراج"
                                    3 -> "التالي: المراجعة والحفظ"
                                    else -> "التالي"
                                }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        // Save Button
                        Button(
                            onClick = {
                                val validSamples = samples.filter { it.body.isNotBlank() }
                                val ruleId = initialRule?.id ?: "source_${UUID.randomUUID().toString().take(8)}"
                                val finalName = sourceName.ifBlank { selectedAppName.ifBlank { "مصدر دفع" } }

                                val generatedRule = AutoRuleGenerator.buildRuleFromSamples(
                                    existingRule = initialRule,
                                    ruleId = ruleId,
                                    sourceName = finalName,
                                    selectedPackage = selectedPackage,
                                    appName = selectedAppName,
                                    samples = validSamples
                                )

                                // Apply any advanced overrides if modified
                                val packagesList = advancedPackages.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                val sendersList = advancedSenderAliases.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                val titlesList = advancedTitleContains.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }
                                val bodiesList = advancedBodyContains.split(",")
                                    .map { it.trim() }
                                    .filter { it.isNotEmpty() }

                                val finalRule = generatedRule.copy(
                                    packageNames = if (packagesList.isNotEmpty()) packagesList else generatedRule.packageNames,
                                    senderFilters = if (sendersList.isNotEmpty()) sendersList else generatedRule.senderFilters,
                                    titleContains = if (titlesList.isNotEmpty()) titlesList else generatedRule.titleContains,
                                    bodyContains = if (bodiesList.isNotEmpty()) bodiesList else generatedRule.bodyContains,
                                    amountExtractionRegex = advancedAmountRegex.ifBlank { generatedRule.amountExtractionRegex },
                                    senderPhoneExtractionRegex = advancedPhoneRegex.ifBlank { generatedRule.senderPhoneExtractionRegex },
                                    accountIdentifierRegex = advancedAccountRegex.ifBlank { generatedRule.accountIdentifierRegex },
                                    parserType = advancedParserType,
                                    priority = advancedPriority,
                                    isLocalDraft = true,
                                    lastTestedSuccess = validSamples.isNotEmpty() && validSamples.all { sample ->
                                        sampleAnalysisResults[sample.id]?.success == true
                                    }
                                )

                                onSave(finalRule)
                            },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("حفظ مصدر الدفع", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Recent notification picker modal
    if (showRecentSheet) {
        RecentNotificationPickerSheet(
            sheetState = sheetState,
            onDismiss = { showRecentSheet = false },
            onSelectNotification = { picked ->
                if (activeSampleIndexForPicker in samples.indices) {
                    val list = samples.toMutableList()
                    val existing = list[activeSampleIndexForPicker]
                    list[activeSampleIndexForPicker] = existing.copy(
                        title = picked.title,
                        body = picked.body
                    )
                    samples = list
                } else {
                    samples = samples + PaymentMessageSample(
                        title = picked.title,
                        body = picked.body
                    )
                }
                // Auto-fill app if not selected yet
                if (selectedPackage.isBlank()) {
                    selectedPackage = picked.packageName
                    selectedAppName = picked.appName
                    if (sourceName.isBlank()) {
                        sourceName = picked.appName
                    }
                }
            }
        )
    }
}

@Composable
private fun StepProgressHeader(currentStep: Int) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepPill(number = 1, title = "التطبيق", isActive = currentStep == 1, isDone = currentStep > 1)
            StepConnector(isDone = currentStep > 1)
            StepPill(number = 2, title = "النماذج", isActive = currentStep == 2, isDone = currentStep > 2)
            StepConnector(isDone = currentStep > 2)
            StepPill(number = 3, title = "الاستخراج", isActive = currentStep == 3, isDone = currentStep > 3)
            StepConnector(isDone = currentStep > 3)
            StepPill(number = 4, title = "الحفظ", isActive = currentStep == 4, isDone = false)
        }
    }
}

@Composable
private fun StepPill(number: Int, title: String, isActive: Boolean, isDone: Boolean) {
    val bgColor = when {
        isDone -> EmeraldSuccess
        isActive -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    val textColor = when {
        isDone || isActive -> Color.White
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(bgColor),
            contentAlignment = Alignment.Center
        ) {
            if (isDone) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            } else {
                Text(text = "$number", color = textColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = title,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
            color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun StepConnector(isDone: Boolean) {
    Box(
        modifier = Modifier
            .width(16.dp)
            .height(2.dp)
            .background(if (isDone) EmeraldSuccess else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
    )
}

// -------------------------------------------------------------------------------------
// STEP 1: CHOOSE APP
// -------------------------------------------------------------------------------------
@Composable
private fun Step1ChooseApp(
    installedApps: List<InstalledAppItem>,
    selectedPackage: String,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    showAllApps: Boolean,
    onToggleShowAll: (Boolean) -> Unit,
    onSelectApp: (InstalledAppItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "اختر التطبيق الذي تصل منه إشعارات الدفع",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "حدد تطبيق المحفظة الإلكترونية، البنك، أو الرسائل القصيرة SMS المسؤولة عن إرسال إشعارات استلام الأموال:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("ابحث باسم التطبيق (فودافون، أهلي، بنك مصر...)") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Show all apps toggle
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleShowAll(!showAllApps) }
                .padding(vertical = 4.dp)
        ) {
            Checkbox(
                checked = showAllApps,
                onCheckedChange = onToggleShowAll
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "إظهار كل التطبيقات المثبتة على الجهاز",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // App list
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(installedApps, key = { it.packageName }) { app ->
                val isSelected = app.packageName == selectedPackage
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (isSelected) 2.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectApp(app) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconView(
                            packageName = app.packageName,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.appName,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = app.packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        RadioButton(
                            selected = isSelected,
                            onClick = { onSelectApp(app) }
                        )
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 2: MESSAGE SAMPLES
// -------------------------------------------------------------------------------------
@Composable
private fun Step2MessageSamples(
    samples: List<PaymentMessageSample>,
    onAddSample: () -> Unit,
    onUpdateSample: (Int, PaymentMessageSample) -> Unit,
    onDeleteSample: (Int) -> Unit,
    onOpenRecentPicker: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "أضف نماذج من رسائل الدفع",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "أضف رسالة أو إشعار حقيقي من التطبيق لكي يعرف البرنامج شكل رسائل الدفع.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Action to use recent notification
        Button(
            onClick = { onOpenRecentPicker(-1) },
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.Notifications, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("استخدام آخر إشعار ملتقط من الهاتف", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(samples.size) { idx ->
                val sample = samples[idx]
                SampleEditCard(
                    sampleNumber = idx + 1,
                    sample = sample,
                    onUpdate = { onUpdateSample(idx, it) },
                    onDelete = { onDeleteSample(idx) },
                    onPasteFromRecent = { onOpenRecentPicker(idx) }
                )
            }

            item {
                if (samples.size < 10) {
                    OutlinedButton(
                        onClick = onAddSample,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("إضافة نموذج رسالة آخر")
                    }
                }
            }
        }
    }
}

@Composable
private fun SampleEditCard(
    sampleNumber: Int,
    sample: PaymentMessageSample,
    onUpdate: (PaymentMessageSample) -> Unit,
    onDelete: () -> Unit,
    onPasteFromRecent: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "نموذج $sampleNumber",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                Row {
                    IconButton(onClick = onPasteFromRecent) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "استخدام إشعار أخير",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "حذف النموذج",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            OutlinedTextField(
                value = sample.title,
                onValueChange = { onUpdate(sample.copy(title = it)) },
                label = { Text("عنوان الإشعار / اسم المرسل (Title)") },
                placeholder = { Text("مثال: VF-Cash أو Bank-AlAhly") },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = sample.body,
                onValueChange = { onUpdate(sample.copy(body = it)) },
                label = { Text("نص الإشعار الحقيقي (Notification Text)") },
                placeholder = { Text("مثال: تم استلام مبلغ 250.00 جنيه من 01012345678 على محفظتك...") },
                minLines = 3,
                maxLines = 6,
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 3: AUTO DETECTION / TEST
// -------------------------------------------------------------------------------------
@Composable
private fun Step3AutoDetectionTest(
    samples: List<PaymentMessageSample>,
    analysisResults: Map<String, AutoDetectionResult>,
    onRunAnalysis: () -> Unit
) {
    LaunchedEffect(Unit) {
        onRunAnalysis()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "اختبار التعرف على البيانات تلقائياً",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "يقوم النظام بتحليل النماذج لاكتشاف المبلغ، رقم المحول، والمصطلحات الدالة على الدفع دون الحاجة لكتابة أي تعبيرات معقدة:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val validSamples = samples.filter { it.body.isNotBlank() }
            if (validSamples.isEmpty()) {
                item {
                    Text(
                        text = "لم يتم إدخال نصوص رسائل بعد. يرجى الرجوع للخطوة السابقة وإضافة نص رسالة.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            } else {
                items(validSamples) { sample ->
                    val result = analysisResults[sample.id]
                    SampleAnalysisCard(sample = sample, result = result)
                }
            }
        }
    }
}

@Composable
private fun SampleAnalysisCard(
    sample: PaymentMessageSample,
    result: AutoDetectionResult?
) {
    val isSuccess = result?.success == true

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSuccess) EmeraldContainer.copy(alpha = 0.4f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        border = BorderStroke(
            1.dp,
            if (isSuccess) EmeraldSuccess else MaterialTheme.colorScheme.error
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isSuccess) EmeraldSuccess else MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isSuccess) "تم التعرف على الرسالة بنجاح" else "لم نستطع تحديد المبلغ",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = if (isSuccess) EmeraldSuccess else MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "\"${sample.body.take(120)}${if (sample.body.length > 120) "..." else ""}\"",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(10.dp))

            if (result != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "المبلغ المكتشف:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = result.formattedAmount ?: "غير محدد",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (result.formattedAmount != null) EmeraldSuccess else MaterialTheme.colorScheme.error
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "رقم المحول:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = result.payerPhone ?: "غير متوفر بالرسالة",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "نوع العملية:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = result.operationType,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!result.transactionReference.isNullOrBlank()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "المرجع:", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = result.transactionReference,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// STEP 4: SAVE & ADVANCED SETTINGS
// -------------------------------------------------------------------------------------
@Composable
private fun Step4SaveAndAdvanced(
    sourceName: String,
    onSourceNameChange: (String) -> Unit,
    appName: String,
    packageName: String,
    samplesCount: Int,
    isAdvancedExpanded: Boolean,
    onToggleAdvanced: () -> Unit,
    advancedPackages: String,
    onPackagesChange: (String) -> Unit,
    advancedTitleContains: String,
    onTitleContainsChange: (String) -> Unit,
    advancedBodyContains: String,
    onBodyContainsChange: (String) -> Unit,
    advancedSenderAliases: String,
    onSenderAliasesChange: (String) -> Unit,
    advancedAmountRegex: String,
    onAmountRegexChange: (String) -> Unit,
    advancedPhoneRegex: String,
    onPhoneRegexChange: (String) -> Unit,
    advancedAccountRegex: String,
    onAccountRegexChange: (String) -> Unit,
    advancedParserType: String,
    onParserTypeChange: (String) -> Unit,
    advancedPriority: Int,
    onPriorityChange: (Int) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "مراجعة وحفظ مصدر الدفع",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "تأكد من اسم المصدر والتطبيق المرتبط به قبل الحفظ:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = sourceName,
                        onValueChange = onSourceNameChange,
                        label = { Text("اسم مصدر الدفع") },
                        placeholder = { Text("مثال: فودافون كاش، البنك الأهلي، بنك مصر...") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconView(packageName = packageName, modifier = Modifier.size(44.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = appName.ifBlank { "تطبيق الدفع" },
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = packageName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "نماذج الرسائل المعتمدة:", style = MaterialTheme.typography.bodySmall)
                        Text(text = "$samplesCount نماذج", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "حالة الإعداد:", style = MaterialTheme.typography.bodySmall)
                        Text(text = "جاهز للحفظ محلياً", color = EmeraldSuccess, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        // Local draft notice
        item {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ملاحظة: سيتم حفظ القاعدة كمسودة محلية (Local Draft) على هذا الجهاز لالتقاط الإشعارات الميدانية فوراً. ستتم مزامنتها كلياً حين توفر واجهة حفظ القواعد في admin3.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // COLLAPSED BY DEFAULT: ADVANCED SETTINGS
        item {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggleAdvanced() },
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "إعدادات متقدمة (Advanced Settings)",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Icon(
                            imageVector = if (isAdvancedExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = null
                        )
                    }

                    AnimatedVisibility(visible = isAdvancedExpanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "هذه الإعدادات مخصصة للمطورين والمشرفين الفنيين:",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            OutlinedTextField(
                                value = advancedPackages,
                                onValueChange = onPackagesChange,
                                label = { Text("حزم التطبيقات المسموحة (Package Names)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = advancedSenderAliases,
                                onValueChange = onSenderAliasesChange,
                                label = { Text("مرشحات المرسل (Sender Filters)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = advancedBodyContains,
                                onValueChange = onBodyContainsChange,
                                label = { Text("كلمات دلالية في نص الرسالة (Body Contains)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = advancedAmountRegex,
                                onValueChange = onAmountRegexChange,
                                label = { Text("تعبير استخراج المبلغ (Amount Regex)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = advancedPhoneRegex,
                                onValueChange = onPhoneRegexChange,
                                label = { Text("تعبير استخراج رقم الهاتف (Phone Regex)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = advancedAccountRegex,
                                onValueChange = onAccountRegexChange,
                                label = { Text("تعبير استخراج رقم الحساب (Account Regex)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )

                            OutlinedTextField(
                                value = advancedParserType,
                                onValueChange = onParserTypeChange,
                                label = { Text("نوع المحلل (Parser Type)") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
