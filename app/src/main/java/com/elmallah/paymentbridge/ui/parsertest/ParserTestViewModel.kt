package com.elmallah.paymentbridge.ui.parsertest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.capture.CapturedNotification
import com.elmallah.paymentbridge.capture.RecentNotificationStore
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.data.RecordResult
import com.elmallah.paymentbridge.domain.PaymentBridgeEvent
import com.elmallah.paymentbridge.domain.PaymentMessageSample
import com.elmallah.paymentbridge.domain.PaymentRuleStore
import com.elmallah.paymentbridge.domain.PaymentSourceRule
import com.elmallah.paymentbridge.domain.RawNotificationMessage
import com.elmallah.paymentbridge.parser.AutoRuleGenerator
import com.elmallah.paymentbridge.parser.CompositePaymentParser
import com.elmallah.paymentbridge.parser.PaymentParseResult
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PresetSample(
    val title: String,
    val sender: String,
    val body: String
)

class ParserTestViewModel(
    private val repository: PaymentRepository,
    private val keyManager: DeviceKeyManager,
    private val ruleStore: PaymentRuleStore? = null
) : ViewModel() {

    private val parser = CompositePaymentParser(
        // Diagnostic mode may test local drafts; LIVE capture never uses this supplier.
        ruleSupplier = { ruleStore?.getAllRules()?.filter { it.enabled } ?: PaymentRuleStore.getDefaultRules() }
    )

    val rules: StateFlow<List<PaymentSourceRule>> = ruleStore?.rulesFlow
        ?: MutableStateFlow(emptyList())

    val selectedRuleId = MutableStateFlow<String?>(null)

    val selectedRule: StateFlow<PaymentSourceRule?> = combine(rules, selectedRuleId) { allRules, id ->
        if (id != null) allRules.firstOrNull { it.id == id }
        else allRules.firstOrNull()
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val recentNotifications: StateFlow<List<CapturedNotification>> =
        RecentNotificationStore.notificationsFlow

    val senderTitleInput = MutableStateFlow("VF-Cash")
    val messageTextInput = MutableStateFlow("")

    val parseResultState = MutableStateFlow<PaymentParseResult?>(null)
    val saveResultState = MutableStateFlow<String?>(null)
    val addSampleMessageFeedback = MutableStateFlow<String?>(null)

    init {
        // Default select the first configured rule, including local drafts for testing.
        val initialRule = ruleStore?.getAllRules()?.firstOrNull()
        if (initialRule != null) {
            selectedRuleId.value = initialRule.id
            senderTitleInput.value = initialRule.senderFilters.firstOrNull() ?: initialRule.name
        }
    }

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

    fun selectRule(ruleId: String) {
        selectedRuleId.value = ruleId
        val rule = rules.value.firstOrNull { it.id == ruleId }
        if (rule != null) {
            if (rule.senderFilters.isNotEmpty()) {
                senderTitleInput.value = rule.senderFilters.first()
            }
        }
        parseResultState.value = null
        saveResultState.value = null
        addSampleMessageFeedback.value = null
    }

    fun loadPreset(preset: PresetSample) {
        senderTitleInput.value = preset.sender
        messageTextInput.value = preset.body
        parseResultState.value = null
        saveResultState.value = null
        addSampleMessageFeedback.value = null
    }

    fun applyCapturedNotification(notification: CapturedNotification) {
        senderTitleInput.value = notification.title
        messageTextInput.value = notification.body
        // Try auto-selecting the matching rule by packageName
        val matchingRule = rules.value.firstOrNull { rule ->
            rule.packageNames.contains(notification.packageName)
        }
        if (matchingRule != null) {
            selectedRuleId.value = matchingRule.id
        }
        parseResultState.value = null
        saveResultState.value = null
        addSampleMessageFeedback.value = null
    }

    fun executeTestParse() {
        val currentRule = selectedRule.value
        val sourcePkg = currentRule?.packageNames?.firstOrNull() ?: "com.manual.parser.test"

        val rawMsg = RawNotificationMessage(
            sourcePackage = sourcePkg,
            title = senderTitleInput.value.trim(),
            text = messageTextInput.value.trim(),
            postedAtMillis = System.currentTimeMillis()
        )
        val diagnosticParser = if (currentRule != null) {
            CompositePaymentParser(ruleSupplier = { listOf(currentRule.copy(enabled = true)) })
        } else {
            parser
        }
        val result = diagnosticParser.parseDiagnosticTestMessage(rawMsg, keyManager.deviceId)
        parseResultState.value = result
        saveResultState.value = null
        addSampleMessageFeedback.value = null

        // Mark rule tested in ruleStore
        if (currentRule != null) {
            val isSuccess = result is PaymentParseResult.Success
            ruleStore?.markRuleTested(currentRule.id, isSuccess)
        }
    }

    fun addCurrentMessageAsSampleToSelectedRule() {
        val currentRule = selectedRule.value ?: return
        val title = senderTitleInput.value.trim()
        val body = messageTextInput.value.trim()

        if (body.isBlank()) return

        val newSample = PaymentMessageSample(title = title, body = body)
        val updatedSamples = currentRule.sampleMessages + newSample

        val updatedRule = AutoRuleGenerator.buildRuleFromSamples(
            existingRule = currentRule,
            ruleId = currentRule.id,
            sourceName = currentRule.name,
            selectedPackage = currentRule.packageNames.firstOrNull() ?: "",
            appName = currentRule.appName,
            samples = updatedSamples
        )

        ruleStore?.saveLocalRuleDraft(updatedRule)
        addSampleMessageFeedback.value = "تمت إضافة هذه الرسالة كنموذج جديد لمصدر \"${currentRule.name}\" وتحديث قواعد الاستخراج بنجاح."
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
