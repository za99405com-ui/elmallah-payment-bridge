package com.elmallah.paymentbridge.ui.parsertest

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.elmallah.paymentbridge.parser.PaymentParseResult
import com.elmallah.paymentbridge.ui.theme.AmberContainer
import com.elmallah.paymentbridge.ui.theme.AmberWarning
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
fun ParserTestScreen(
    viewModel: ParserTestViewModel,
    onBack: () -> Unit
) {
    val senderTitle by viewModel.senderTitleInput.collectAsState()
    val messageText by viewModel.messageTextInput.collectAsState()
    val parseResult by viewModel.parseResultState.collectAsState()
    val saveResult by viewModel.saveResultState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "اختبار قراءة الرسائل",
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
            // Notice about manual test environment
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFFF1F5F9),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "هذه الشاشة مخصصة لاختبار دقة معالجة نصوص الرسائل المختلفة يدوياً دون الحاجة لانتظار إشعار حقيقي. لا تؤثر على سياسة الأمان الصارمة للإشعارات الحية.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Presets row
            item {
                Column {
                    Text(
                        text = "نماذج رسائل جاهزة للتجربة:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
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

            // Sender Title Input
            item {
                OutlinedTextField(
                    value = senderTitle,
                    onValueChange = { viewModel.senderTitleInput.value = it },
                    label = { Text("مرسل الإشعار (Title)") },
                    placeholder = { Text("VF-Cash أو Bank-AlAhly") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    singleLine = true
                )
            }

            // Message Text Input
            item {
                OutlinedTextField(
                    value = messageText,
                    onValueChange = { viewModel.messageTextInput.value = it },
                    label = { Text("نص الرسالة / الإشعار") },
                    placeholder = { Text("الصق نص رسالة الدفع هنا...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            // Execute Button
            item {
                Button(
                    onClick = { viewModel.executeTestParse() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                    enabled = messageText.isNotBlank()
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("اختبار معالجة الرسالة الآن", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }

            // Save feedback message if available
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

            // Parse Result Card
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
                                            text = "تم التعرف بنجاح على عملية دفع واردة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = EmeraldSuccess
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    ResultDetailRow("المزود", event.provider)
                                    ResultDetailRow("القناة", event.paymentChannel)
                                    ResultDetailRow("المبلغ بالجنيه", "${String.format(Locale.US, "%.2f", event.amountInMajorUnits)} ج.م")
                                    ResultDetailRow("المبلغ بالقروش (Minor)", "${event.amountMinor} قرش")
                                    ResultDetailRow("الرقم المرجعي للعملية", event.transactionReference ?: "غير متوفر في الإشعار")
                                    if (event.payerPhone != null) ResultDetailRow("هاتف الراسل", event.payerPhone)
                                    if (event.accountLast4 != null) ResultDetailRow("آخر 4 أرقام من الحساب", "****${event.accountLast4}")
                                    if (event.walletPhone != null) ResultDetailRow("محفظة الاستلام", event.walletPhone)
                                    ResultDetailRow("دقة قراءة الإشعار", "${event.parseConfidence} (خاصة بسلامة القراءة فقط)")

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

                        is PaymentParseResult.Ignored -> {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = AmberContainer),
                                border = BorderStroke(1.dp, AmberWarning.copy(alpha = 0.5f))
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Block,
                                            contentDescription = null,
                                            tint = AmberWarning,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "تم تجاهل الإشعار طبقاً للسياسة",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = AmberWarning
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = result.message,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "سبب التجاهل: ${result.reason}",
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                            }
                        }

                        is PaymentParseResult.Failed -> {
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
                                            imageVector = Icons.Default.Error,
                                            contentDescription = null,
                                            tint = CoralError,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "فشل التعرف على صيغة استلام أموال",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = CoralError
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = result.message,
                                        fontSize = 13.sp,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
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
        Text(text = label, fontSize = 12.sp, color = TextSecondary)
        Text(text = value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
    }
}
