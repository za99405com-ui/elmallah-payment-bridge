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
 * Manages Payment Source Rules received from elmallah-admin3 and configured locally.
 *
 * admin3 remains the sole authoritative source of truth.
 * Rules disabled by admin3 cannot be enabled locally for live payments.
 * Empty server rule lists are valid and do NOT fall back to local defaults.
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

    var hasSyncedWithServer: Boolean
        get() = prefs.getBoolean(KEY_HAS_SYNCED, false)
        set(value) = prefs.edit().putBoolean(KEY_HAS_SYNCED, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    init {
        loadRules()
    }

    private fun loadRules() {
        val json = prefs.getString(KEY_CACHED_RULES, null)
        val loaded = if (!json.isNullOrBlank()) {
            try {
                adapter.fromJson(json) ?: emptyList()
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            // Avoid automatic local defaults if never synced
            emptyList()
        }
        _rulesFlow.value = loaded
    }

    fun getActiveRules(): List<PaymentSourceRule> {
        return _rulesFlow.value
            .filter { it.enabled && it.packageNames.isNotEmpty() }
            .sortedBy { it.priority }
    }

    fun getAllRules(): List<PaymentSourceRule> {
        return _rulesFlow.value
    }

    fun getRuleById(ruleId: String): PaymentSourceRule? {
        return _rulesFlow.value.firstOrNull { it.id == ruleId }
    }

    /**
     * Updates locally cached rules when authoritative rule updates are received from admin3.
     */
    fun updateRules(newRules: List<PaymentSourceRule>) {
        hasSyncedWithServer = true
        lastSyncTimestamp = System.currentTimeMillis()

        val current = _rulesFlow.value
        // Keep local draft rules created on device that don't conflict with server IDs
        val localDrafts = current.filter { local ->
            local.isLocalDraft && newRules.none { it.id == local.id }
        }

        // For server rules, admin3 is strictly authoritative over enabled state
        val merged = newRules.map { srv ->
            val localMatch = current.firstOrNull { it.id == srv.id }
            if (localMatch != null) {
                srv.copy(
                    appName = localMatch.appName ?: srv.appName,
                    packageNames = if (localMatch.packageNames.isNotEmpty()) localMatch.packageNames else srv.packageNames,
                    sampleMessages = localMatch.sampleMessages.ifEmpty { srv.sampleMessages },
                    lastTestedSuccess = localMatch.lastTestedSuccess ?: srv.lastTestedSuccess,
                    enabled = srv.enabled // Admin3 authority wins
                )
            } else {
                srv
            }
        } + localDrafts

        try {
            val json = adapter.toJson(merged)
            val authJson = adapter.toJson(newRules)
            prefs.edit()
                .putString(KEY_CACHED_RULES, json)
                .putString(KEY_AUTHORITATIVE_RULES, authJson)
                .apply()
            _rulesFlow.value = merged
        } catch (_: Exception) {
            _rulesFlow.value = merged
        }
    }

    /**
     * Saves a local draft rule created or edited in the guided setup.
     */
    fun saveLocalRuleDraft(rule: PaymentSourceRule) {
        val current = _rulesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == rule.id }
        val updatedRule = rule.copy(isLocalDraft = true)
        if (index >= 0) {
            current[index] = updatedRule
        } else {
            current.add(updatedRule)
        }
        try {
            val json = adapter.toJson(current)
            prefs.edit().putString(KEY_CACHED_RULES, json).apply()
        } catch (_: Exception) {}
        _rulesFlow.value = current
    }

    fun deleteRule(ruleId: String) {
        val updated = _rulesFlow.value.filterNot { it.id == ruleId }
        try {
            val json = adapter.toJson(updated)
            prefs.edit().putString(KEY_CACHED_RULES, json).apply()
        } catch (_: Exception) {}
        _rulesFlow.value = updated
    }

    fun markRuleTested(ruleId: String, success: Boolean) {
        val current = _rulesFlow.value.map {
            if (it.id == ruleId) it.copy(lastTestedSuccess = success) else it
        }
        try {
            val json = adapter.toJson(current)
            prefs.edit().putString(KEY_CACHED_RULES, json).apply()
        } catch (_: Exception) {}
        _rulesFlow.value = current
    }

    fun hasUnsavedChanges(): Boolean {
        return _rulesFlow.value.any { it.isLocalDraft }
    }

    /**
     * Finds matching rule for an incoming notification.
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
        private const val KEY_AUTHORITATIVE_RULES = "authoritative_payment_rules_json"
        private const val KEY_HAS_SYNCED = "rules_has_synced_with_server"
        private const val KEY_LAST_SYNC_TIME = "rules_last_sync_timestamp"

        val DEFAULT_MESSAGING_PACKAGES = listOf(
            "com.samsung.android.messaging",
            "com.google.android.apps.messaging"
        )

        /**
         * Seed rules for Egyptian mobile payment channels (available for tests and references).
         */
        fun getDefaultRules(): List<PaymentSourceRule> {
            return listOf(
                PaymentSourceRule(
                    id = "vodafone_cash",
                    name = "فودافون كاش",
                    appName = "Vodafone Cash",
                    enabled = true,
                    paymentChannel = "WALLET",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("VF-Cash", "VodafoneCash", "Vodafone Cash"),
                    parserType = "VODAFONE_CASH_LEGACY",
                    priority = 10
                ),
                PaymentSourceRule(
                    id = "nbe_incoming_transfer",
                    name = "البنك الأهلي",
                    appName = "NBE Mobile",
                    enabled = true,
                    paymentChannel = "INSTAPAY_OR_BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("Bank-AlAhly", "NBE", "NationalBankOfEgypt"),
                    parserType = "NBE_LEGACY",
                    priority = 20
                ),
                PaymentSourceRule(
                    id = "banque_misr",
                    name = "بنك مصر",
                    appName = "BM Online",
                    enabled = true,
                    paymentChannel = "BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("Banque Misr", "BM-Wallet", "BM"),
                    bodyContains = listOf("تم إضافة", "تم اضافه", "تم تحويل", "إيداع", "ايداع", "استلام"),
                    amountExtractionRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة)?\s*(\d+(?:\.\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP)""",
                    parserType = "RULE_BASED",
                    priority = 30
                ),
                PaymentSourceRule(
                    id = "instapay_egypt",
                    name = "إنستاباي",
                    appName = "InstaPay Egypt",
                    enabled = true,
                    paymentChannel = "INSTAPAY",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("InstaPay", "IPN", "InstaPay-Egypt"),
                    bodyContains = listOf("استلام", "تحويل", "إضافة", "اضافة", "مبلغ", "received"),
                    amountExtractionRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة)?\s*(\d+(?:\.\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP)""",
                    parserType = "RULE_BASED",
                    priority = 40
                ),
                PaymentSourceRule(
                    id = "cib_egypt",
                    name = "CIB",
                    appName = "CIB Egypt",
                    enabled = true,
                    paymentChannel = "BANK_TRANSFER",
                    packageNames = DEFAULT_MESSAGING_PACKAGES,
                    senderFilters = listOf("CIB", "CIB-Egypt"),
                    bodyContains = listOf("إضافة", "اضافة", "تحويل", "إيداع", "ايداع", "credited"),
                    amountExtractionRegex = """(?:مبلغ|بمبلغ|قيمة|بقيمة)?\s*(\d+(?:\.\d{1,2})?)\s*(?:جم|جنيه|ج\.م|EGP)""",
                    parserType = "RULE_BASED",
                    priority = 50
                )
            )
        }
    }
}
