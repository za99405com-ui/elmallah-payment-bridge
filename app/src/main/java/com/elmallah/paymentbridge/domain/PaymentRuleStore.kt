package com.elmallah.paymentbridge.domain

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages Payment Source Rules received from elmallah-admin3.
 *
 * Rules are cached locally so the Payment Bridge can continue capturing facts
 * even during transient server connectivity interruptions.
 * admin3 remains the sole authoritative source of truth.
 */
class PaymentRuleStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val listType = Types.newParameterizedType(List::class.java, PaymentSourceRule::class.java)
    private val adapter = moshi.adapter<List<PaymentSourceRule>>(listType)

    private val _rulesFlow = MutableStateFlow<List<PaymentSourceRule>>(emptyList())
    val rulesFlow: StateFlow<List<PaymentSourceRule>> = _rulesFlow.asStateFlow()

    init {
        loadRules()
    }

    private fun loadRules() {
        val json = prefs.getString(KEY_CACHED_RULES, null)
        val loaded = if (!json.isNullOrBlank()) {
            try {
                adapter.fromJson(json) ?: getDefaultRules()
            } catch (_: Exception) {
                getDefaultRules()
            }
        } else {
            getDefaultRules()
        }
        _rulesFlow.value = loaded
    }

    fun getActiveRules(): List<PaymentSourceRule> {
        return _rulesFlow.value.filter { it.enabled }.sortedBy { it.priority }
    }

    fun getAllRules(): List<PaymentSourceRule> {
        return _rulesFlow.value
    }

    /**
     * Updates locally cached rules when authoritative rule updates are received from admin3.
     */
    fun updateRules(newRules: List<PaymentSourceRule>) {
        if (newRules.isEmpty()) return
        try {
            val json = adapter.toJson(newRules)
            prefs.edit().putString(KEY_CACHED_RULES, json).apply()
            _rulesFlow.value = newRules
        } catch (_: Exception) {
            // Retain existing cached rules if serialization fails
        }
    }

    fun toggleRule(ruleId: String, enabled: Boolean) {
        val current = _rulesFlow.value
        val updated = current.map {
            if (it.id == ruleId) it.copy(enabled = enabled) else it
        }
        updateRules(updated)
    }

    /**
     * Finds matching rule for an incoming notification.
     * Evaluates package names, sender filters, title content, and body patterns.
     */
    fun findMatchingRule(
        sourcePackage: String,
        senderTitle: String,
        bodyText: String
    ): PaymentSourceRule? {
        val active = getActiveRules()
        val pkg = sourcePackage.trim()
        val sender = senderTitle.trim()

        for (rule in active) {
            // 1. Package verification
            val pkgMatches = rule.packageNames.isEmpty() ||
                rule.packageNames.any { it.equals(pkg, ignoreCase = true) }
            if (!pkgMatches) continue

            // 2. Sender verification
            val senderMatches = rule.senderFilters.isEmpty() ||
                rule.senderFilters.any { it.equals(sender, ignoreCase = true) }
            if (!senderMatches) continue

            // 3. Title contains verification
            val titleMatches = rule.titleContains.isNullOrEmpty() ||
                rule.titleContains.any { sender.contains(it, ignoreCase = true) }
            if (!titleMatches) continue

            // 4. Body contains verification
            val bodyMatches = rule.bodyContains.isNullOrEmpty() ||
                rule.bodyContains.any { bodyText.contains(it, ignoreCase = true) }
            if (!bodyMatches) continue

            // 5. Regex pattern verification
            val regexMatches = rule.regexPatterns.isNullOrEmpty() ||
                rule.regexPatterns.any { pattern ->
                    try {
                        Regex(pattern, RegexOption.IGNORE_CASE).containsMatchIn(bodyText)
                    } catch (_: Exception) {
                        false
                    }
                }
            if (!regexMatches) continue

            return rule
        }
        return null
    }

    companion object {
        private const val PREFS_NAME = "almallah_payment_rules_cache"
        private const val KEY_CACHED_RULES = "cached_payment_rules_json"

        val DEFAULT_MESSAGING_PACKAGES = listOf(
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging"
        )

        /**
         * Seed rules for Egyptian mobile payment channels.
         * admin3 can dynamically reconfigure, disable, or add custom rules.
         */
        fun getDefaultRules(): List<PaymentSourceRule> {
            return listOf(
                // 1. Vodafone Cash
                PaymentSourceRule(
                    id = "vodafone_cash",
                    name = "فودافون كاش (Vodafone Cash)",
                    enabled = true,
                    paymentChannel = "WALLET",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("VF-Cash", "VodafoneCash", "Vodafone Cash"),
                    parserType = "VODAFONE_CASH_LEGACY",
                    priority = 10
                ),
                // 2. Bank AlAhly (NBE) Incoming Transfer / InstaPay
                PaymentSourceRule(
                    id = "nbe_incoming_transfer",
                    name = "البنك الأهلي المصري (NBE / InstaPay)",
                    enabled = true,
                    paymentChannel = "INSTAPAY_OR_BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("Bank-AlAhly", "NBE", "NationalBankOfEgypt"),
                    parserType = "NBE_LEGACY",
                    priority = 20
                ),
                // 3. Banque Misr
                PaymentSourceRule(
                    id = "banque_misr",
                    name = "بنك مصر (Banque Misr)",
                    enabled = true,
                    paymentChannel = "BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("Banque Misr", "BM-Wallet", "BM"),
                    bodyContains = listOf("تم إضافة", "تم اضافه", "تم تحويل", "إيداع", "ايداع", "استلام"),
                    amountExtractionRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة)?\s*(\d+(?:\.\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP)""",
                    parserType = "RULE_BASED",
                    priority = 30
                ),
                // 4. InstaPay Egypt Generic
                PaymentSourceRule(
                    id = "instapay_egypt",
                    name = "إنستاباي (InstaPay Egypt)",
                    enabled = true,
                    paymentChannel = "INSTAPAY",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("InstaPay", "IPN", "InstaPay-Egypt"),
                    bodyContains = listOf("استلام", "تحويل", "إضافة", "اضافة", "مبلغ", "received"),
                    amountExtractionRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة)?\s*(\d+(?:\.\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP)""",
                    parserType = "RULE_BASED",
                    priority = 40
                ),
                // 5. CIB Egypt
                PaymentSourceRule(
                    id = "cib_egypt",
                    name = "البنك التجاري الدولي (CIB)",
                    enabled = true,
                    paymentChannel = "BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("CIB", "CIB-Egypt"),
                    bodyContains = listOf("إضافة", "اضافة", "تحويل", "إيداع", "ايداع", "credited"),
                    amountExtractionRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة)?\s*(\d+(?:\.\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP)""",
                    parserType = "RULE_BASED",
                    priority = 50
                ),
                // 6. QNB AlAhli
                PaymentSourceRule(
                    id = "qnb_alahli",
                    name = "بنك قطر الوطني (QNB AlAhli)",
                    enabled = true,
                    paymentChannel = "BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("QNB", "QNB-AlAhli"),
                    bodyContains = listOf("إضافة", "اضافة", "تحويل", "credited"),
                    parserType = "RULE_BASED",
                    priority = 60
                ),
                // 7. AlexBank
                PaymentSourceRule(
                    id = "alexbank",
                    name = "بنك الإسكندرية (AlexBank)",
                    enabled = true,
                    paymentChannel = "BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("AlexBank", "Bank-Alex"),
                    bodyContains = listOf("إضافة", "اضافة", "تحويل", "credited"),
                    parserType = "RULE_BASED",
                    priority = 70
                )
            )
        }
    }
}
