package com.elmallah.paymentbridge.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
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
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val baseUrlState by viewModel.apiBaseUrlInput.collectAsState()
    val bridgeUploadEnabled by viewModel.bridgeUploadEnabled.collectAsState()
    val rawDiagnosticsEnabled by viewModel.rawDiagnosticsEnabled.collectAsState()
    val healthCheckStatus by viewModel.healthCheckStatus.collectAsState()
    val purgeResultStatus by viewModel.purgeResultStatus.collectAsState()

    var urlDraft by remember(baseUrlState) { mutableStateOf(baseUrlState) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "إعدادات الربط والأمان",
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Device Cryptographic Identity
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NeutralBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Security,
                                contentDescription = null,
                                tint = NavyPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "بيانات تعريف وأمان الهاتف",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = TextPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(text = "معرّف الجهاز (Device ID):", fontSize = 12.sp, color = TextSecondary)
                        Text(
                            text = viewModel.deviceId,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(text = "حالة مفتاح التوقيع الرقمي (HMAC Secret):", fontSize = 12.sp, color = TextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Key,
                                contentDescription = null,
                                tint = EmeraldSuccess,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "مشفر ومخزن في خزنة عتاد الهاتف (Android Keystore)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = EmeraldSuccess
                            )
                        }
                    }
                }
            }

            // 2. Operating Mode Switch (Phase 1 vs Phase 2)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NeutralBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تفعيل المزامنة مع السيرفر (المرحلة الثانية)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (bridgeUploadEnabled) "مفعّل: يرسل العمليات المشفرة فوراً للسيرفر" else "معطل (المرحلة الأولى: التقاط ومراجعة محلية فقط)",
                                    fontSize = 12.sp,
                                    color = if (bridgeUploadEnabled) EmeraldSuccess else TextMuted
                                )
                            }
                            Switch(
                                checked = bridgeUploadEnabled,
                                onCheckedChange = { viewModel.toggleBridgeUpload(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NavyPrimary
                                )
                            )
                        }
                    }
                }
            }

            // 3. Backend URL Configuration
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NeutralBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "عنوان السيرفر (elmallah-admin3 Base URL)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = urlDraft,
                            onValueChange = { urlDraft = it },
                            label = { Text("Base URL") },
                            placeholder = { Text("https://elmallah-admin3.example.com") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            shape = RoundedCornerShape(8.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    val success = viewModel.saveBaseUrl(urlDraft)
                                    if (success) {
                                        Toast.makeText(context, "تم حفظ عنوان السيرفر", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "خطأ: يجب أن يبدأ العنوان بـ https:// في نسخ الإنتاج", Toast.LENGTH_LONG).show()
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = NavyPrimary),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.Save, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("حفظ العنوان")
                            }

                            OutlinedButton(
                                onClick = { viewModel.testServerHealth() },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(imageVector = Icons.Default.NetworkCheck, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("فحص الاتصال")
                            }
                        }

                        healthCheckStatus?.let { status ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (status.startsWith("نجح")) EmeraldContainer else AmberContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = status,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = if (status.startsWith("نجح")) EmeraldSuccess else AmberWarning,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 4. Privacy & Raw SMS Diagnostics
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, NeutralBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "الخصوصية وسجل الرسائل الخام",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "حفظ نصوص الرسائل للتشخيص المحلي",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "معطل افتراضياً. عند التفعيل تُحفظ النصوص محلياً فقط وتُحذف تلقائياً بعد 7 أيام.",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                            Switch(
                                checked = rawDiagnosticsEnabled,
                                onCheckedChange = { viewModel.toggleRawDiagnostics(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = NavyPrimary
                                )
                            )
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = NeutralBorder)

                        OutlinedButton(
                            onClick = { viewModel.clearAllRawSnippets() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CleaningServices, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("مسح جميع نصوص الرسائل الخام المخزنة الآن")
                        }

                        purgeResultStatus?.let { res ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(text = res, fontSize = 12.sp, color = EmeraldSuccess)
                        }
                    }
                }
            }
        }
    }
}
