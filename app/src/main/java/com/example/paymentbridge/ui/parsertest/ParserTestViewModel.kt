package com.example.paymentbridge.ui.parsertest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paymentbridge.data.PaymentProcessOutcome
import com.example.paymentbridge.data.PaymentRepository
import com.example.paymentbridge.domain.PaymentBridgeEvent
import com.example.paymentbridge.domain.RawNotificationMessage
import com.example.paymentbridge.parser.CompositePaymentParser
import com.example.paymentbridge.parser.FailureReason
import com.example.paymentbridge.parser.IgnoreReason
import com.example.paymentbridge.parser.PaymentParseResult
import com.example.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ParserAnalysis(
    val status: AnalysisStatus,
    val provider: String = "غير محدد",
    val isIncoming: Boolean = false,
    val amountMajor: Double? = null,
    val amountMinor: Long? = null,
    val payerPhone: String? = null,
    val walletPhone: String? = null,
    val transactionReference: String? = null,
    val accountLast4: String? = null,
    val confidence: String = "low",
    val explanation: String = "",
    val rawHash: String = "",
    val parsedEvent: PaymentBridgeEvent? = null
)

enum class AnalysisStatus {
    IDLE,
    SUCCESS_INCOMING,
    IGNORED,
    FAILED
}

class ParserTestViewModel(
    private val repository: PaymentRepository,
    private val keyManager: DeviceKeyManager
) : ViewModel() {

    private val parser = CompositePaymentParser()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _analysis = MutableStateFlow(ParserAnalysis(status = AnalysisStatus.IDLE))
    val analysis: StateFlow<ParserAnalysis> = _analysis.asStateFlow()

    private val _simulationMessage = MutableStateFlow<String?>(null)
    val simulationMessage: StateFlow<String?> = _simulationMessage.asStateFlow()

    fun onInputTextChanged(text: String) {
        _inputText.value = text
    }

    fun loadSample(type: SampleType) {
        val sample = when (type) {
            SampleType.VODAFONE_VALID ->
                "تم استلام مبلغ 180.00 جنيه من 01012345678\nالمسجل باسم أحمد محمد\nعلى رقم محفظتك 01098765432\nبتاريخ 14-09-26 01:50\nرصيدك الحالي: 258.56 جنيه\nرقم العملية: 023686770281"
            SampleType.VODAFONE_BALANCE_IGNORED ->
                "رصيد حسابك في فودافون كاش الحالي هو 258.56 جنيه. لمعرفة تفاصيل العمليات السابقة اطلب #9*."
            SampleType.NBE_VALID ->
                "تم اضافه تحويل 287.22 EGP\nلحساب 0013\nمرجع 599INTA2625604XW"
            SampleType.NBE_VALID_ALT_SPELLING ->
                "تم إضافة تحويل 150.00 EGP\nلحساب 0013\nمرجع 8872INTA991823"
            SampleType.NBE_OUTGOING_IGNORED ->
                "تم تنفيذ تحويل لحظي من حسابكم رقم 0013 بمبلغ 250.00 جم إلى حساب آخر، رقم مرجعي 92837182."
        }
        _inputText.value = sample
        analyzeText(sample)
    }

    fun analyzeText(textToAnalyze: String = _inputText.value) {
        _simulationMessage.value = null
        if (textToAnalyze.isBlank()) {
            _analysis.value = ParserAnalysis(
                status = AnalysisStatus.IDLE,
                explanation = "يرجى لصق نص الرسالة للتحليل"
            )
            return
        }

        val rawMessage = RawNotificationMessage(
            sourcePackage = "com.google.android.apps.messaging",
            title = inferTitle(textToAnalyze),
            text = textToAnalyze
        )

        when (val result = parser.parse(rawMessage, keyManager.deviceId)) {
            is PaymentParseResult.Success -> {
                val e = result.event
                _analysis.value = ParserAnalysis(
                    status = AnalysisStatus.SUCCESS_INCOMING,
                    provider = e.provider,
                    isIncoming = true,
                    amountMajor = e.amountInMajorUnits,
                    amountMinor = e.amountMinor,
                    payerPhone = e.payerPhone,
                    walletPhone = e.walletPhone,
                    transactionReference = e.transactionReference,
                    accountLast4 = e.accountLast4,
                    confidence = e.confidence,
                    explanation = result.detailsExplanation,
                    rawHash = e.rawMessageHash,
                    parsedEvent = e
                )
            }
            is PaymentParseResult.Ignored -> {
                _analysis.value = ParserAnalysis(
                    status = AnalysisStatus.IGNORED,
                    provider = inferTitle(textToAnalyze),
                    isIncoming = false,
                    explanation = "[تم التجاهل] " + result.message
                )
            }
            is PaymentParseResult.Failed -> {
                _analysis.value = ParserAnalysis(
                    status = AnalysisStatus.FAILED,
                    provider = inferTitle(textToAnalyze),
                    isIncoming = false,
                    explanation = "[فشل التحليل] " + result.message
                )
            }
        }
    }

    fun simulateNotificationArrival() {
        val event = _analysis.value.parsedEvent
        if (event == null) {
            _simulationMessage.value = "لا يوجد إيداع صالح للمحاكاة"
            return
        }

        viewModelScope.launch {
            when (val outcome = repository.ingestPaymentEvent(event, _inputText.value)) {
                is PaymentProcessOutcome.NewPaymentEnqueued -> {
                    _simulationMessage.value = "✅ تم حفظ العملية بنجاح وجارٍ إرسالها إلى السيرفر!"
                }
                is PaymentProcessOutcome.DuplicateDetected -> {
                    _simulationMessage.value = "⚠️ تم الكشف عن عملية مكررة برقم المرجع ${outcome.existingReference}، لم يتم إنشاء إيداع جديد."
                }
                is PaymentProcessOutcome.SaveFailed -> {
                    _simulationMessage.value = "❌ فشل حفظ العملية: ${outcome.error}"
                }
            }
        }
    }

    private fun inferTitle(text: String): String {
        return when {
            text.contains("فودافون") || text.contains("VF-Cash") || text.contains("محفظتك") -> "VF-Cash"
            text.contains("البنك الأهلي") || text.contains("Bank-AlAhly") || text.contains("AlAhly") || text.contains("مرجع") -> "Bank-AlAhly"
            else -> "SMS-Inbox"
        }
    }
}

enum class SampleType {
    VODAFONE_VALID,
    VODAFONE_BALANCE_IGNORED,
    NBE_VALID,
    NBE_VALID_ALT_SPELLING,
    NBE_OUTGOING_IGNORED
}
