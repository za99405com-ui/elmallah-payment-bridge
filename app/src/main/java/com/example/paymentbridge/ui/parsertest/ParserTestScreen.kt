package com.example.paymentbridge.ui.parsertest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Science
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusNeutral
import com.example.ui.theme.StatusSuccess
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParserTestScreen(
    viewModel: ParserTestViewModel,
    onBack: () -> Unit
) {
    val inputText by viewModel.inputText.collectAsState()
    val analysis by viewModel.analysis.collectAsState()
    val simulationMessage by viewModel.simulationMessage.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اختبار قراءة الرسائل",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "رجوع"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "جرب قراءة أي رسالة SMS حقيقية أو استعن بالنماذج الجاهزة للتأكد من دقة الاستخراج.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Sample Chips
            Text(
                text = "نماذج رسائل واقعية:",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = false,
                    onClick = { viewModel.loadSample(SampleType.VODAFONE_VALID) },
                    label = { Text("فودافون: إيداع سليم") }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.loadSample(SampleType.NBE_VALID) },
                    label = { Text("الأهلي/إنستاباي: إيداع") }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.loadSample(SampleType.NBE_VALID_ALT_SPELLING) },
                    label = { Text("الأهلي (إضافة تحويل)") }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.loadSample(SampleType.NBE_OUTGOING_IGNORED) },
                    label = { Text("الأهلي: صادر (مستبعد)") }
                )
                FilterChip(
                    selected = false,
                    onClick = { viewModel.loadSample(SampleType.VODAFONE_BALANCE_IGNORED) },
                    label = { Text("فودافون: رصيد (مستبعد)") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Text Input
            OutlinedTextField(
                value = inputText,
                onValueChange = { viewModel.onInputTextChanged(it) },
                label = { Text("نص الرسالة أو الإشعار") },
                placeholder = { Text("الصق هنا الرسالة النصية الواردة...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.analyzeText() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Science, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("فحص وتحليل")
                }

                if (analysis.status == AnalysisStatus.SUCCESS_INCOMING) {
                    OutlinedButton(
                        onClick = { viewModel.simulateNotificationArrival() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("محاكاة الحفظ")
                    }
                }
            }

            if (!simulationMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Text(
                        text = simulationMessage ?: "",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Analysis Result Card
            Text(
                text = "نتيجة التحليل والفحص:",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    when (analysis.status) {
                        AnalysisStatus.IDLE -> {
                            Text(
                                text = "اضغط على فحص وتحليل بعد إدخال الرسالة.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnalysisStatus.IGNORED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Block, contentDescription = null, tint = StatusNeutral)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم تجاهل واستبعاد الرسالة",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusNeutral
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = analysis.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnalysisStatus.FAILED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.Error, contentDescription = null, tint = StatusError)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "فشل التعرف على البيانات",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusError
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = analysis.explanation,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        AnalysisStatus.SUCCESS_INCOMING -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = StatusSuccess)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "تم التعرف على إيداع وارد بنجاح ✅",
                                    fontWeight = FontWeight.Bold,
                                    color = StatusSuccess
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            ResultField(label = "جهة التحويل (Provider)", value = analysis.provider)
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            ResultField(
                                label = "المبلغ المالي",
                                value = String.format(Locale.US, "%.2f ج.م  (%d قرش)", analysis.amountMajor ?: 0.0, analysis.amountMinor ?: 0),
                                isHighlight = true
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            ResultField(
                                label = "رقم العملية / المرجع",
                                value = analysis.transactionReference ?: "غير متوفر",
                                isMonospace = true
                            )
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                            ResultField(
                                label = "هاتف الراسل (Vodafone فقط)",
                                value = analysis.payerPhone ?: "غير متوفر بالرسالة (طبيعي لتحويلات البنك الأهلي)"
                            )

                            if (!analysis.accountLast4.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                ResultField(label = "الحساب المستلم", value = "**** " + analysis.accountLast4)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            ResultField(label = "مستوى الثقة (Confidence)", value = analysis.confidence)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ResultField(
    label: String,
    value: String,
    isHighlight: Boolean = false,
    isMonospace: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isHighlight) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.Default,
            color = if (isHighlight) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
    }
}
