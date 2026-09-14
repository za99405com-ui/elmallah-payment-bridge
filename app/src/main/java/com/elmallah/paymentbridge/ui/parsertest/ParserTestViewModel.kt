package com.elmallah.paymentbridge.ui.parsertest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.data.RecordResult
import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.CompositePaymentParser
import com.elmallah.paymentbridge.parser.PaymentParseResult
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class PresetSample(
    val title: String,
    val sender: String,
    val body: String
)

class ParserTestViewModel(
    private val repository: PaymentRepository,
    private val keyManager: DeviceKeyManager
) : ViewModel() {

    private val parser = CompositePaymentParser()

    val senderTitleInput = MutableStateFlow("VF-Cash")
    val messageTextInput = MutableStateFlow("")

    val parseResultState = MutableStateFlow<PaymentParseResult?>(null)
    val saveResultState = MutableStateFlow<String?>(null)

    val samplePresets = listOf(
        PresetSample(
            title = "فودافون كاش - استلام حقيقي",
            sender = "VF-Cash",
            body = """
                تم استلام مبلغ 180.00 جنيه من 01015192040
                المسجل باسم احمد علي
                على رقم محفظتك 01098765432
                بتاريخ 14-09-26 01:50
                رصيدك الحالي: 258.56 جنيه
                رقم العملية: 023686770281
            """.trimIndent()
        ),
        PresetSample(
            title = "البنك الأهلي - تحويل وارد حقيقي",
            sender = "Bank-AlAhly",
            body = """
                تم اضافه تحويل 287.22 EGP
                لحساب 0013
                مرجع 599INTA2625604XW
            """.trimIndent()
        ),
        PresetSample(
            title = "فودافون كاش - استعلام رصيد (يُتجاهل)",
            sender = "VF-Cash",
            body = "رصيد حسابك في فودافون كاش الحالي هو 258.56 جنيه. لمعرفة تفاصيل العمليات السابقة اطلب #9*."
        ),
        PresetSample(
            title = "البنك الأهلي - تحويل صادر (يُتجاهل)",
            sender = "Bank-AlAhly",
            body = "تم تنفيذ تحويل لحظي من حسابكم رقم 0130 بمبلغ 180.00 جم إلى محفظة فودافون كاش رقم مرجعي 643086325070"
        )
    )

    fun loadPreset(preset: PresetSample) {
        senderTitleInput.value = preset.sender
        messageTextInput.value = preset.body
        parseResultState.value = null
        saveResultState.value = null
    }

    fun executeTestParse() {
        val rawMsg = RawNotificationMessage(
            sourcePackage = "com.manual.parser.test",
            title = senderTitleInput.value.trim(),
            text = messageTextInput.value.trim(),
            postedAtMillis = System.currentTimeMillis()
        )
        parseResultState.value = parser.parseDiagnosticTestMessage(rawMsg, keyManager.deviceId)
        saveResultState.value = null
    }

    fun saveParsedEventLocally(event: PaymentBridgeEvent) {
        viewModelScope.launch {
            val result = repository.recordPaymentEvent(event, messageTextInput.value)
            saveResultState.value = when (result) {
                is RecordResult.Success -> "تم حفظ العملية بنجاح في قاعدة البيانات المحلية (وضع التقاط ومراجعة فقط)."
                is RecordResult.Duplicate -> "تنبيه: هذه العملية موجودة مسبقاً في قاعدة البيانات لنفس الرقم المرجعي والمزود."
                is RecordResult.Failure -> "خطأ: ${result.errorMessage}"
            }
        }
    }
}
