package com.elmallah.paymentbridge.security

import android.content.Context
import android.content.SharedPreferences
import android.provider.Settings
import com.elmallah.paymentbridge.BuildConfig
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID

class DeviceKeyManager(context: Context) {

    private val appContext: Context = context.applicationContext

    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "almallah_secure_device_credentials"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SECRET_CIPHER = "device_secret_cipher_v2"
        private const val KEY_SECRET_IV = "device_secret_iv_v2"
        private const val KEY_IS_PROVISIONED = "is_provisioned_v2"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_BRIDGE_UPLOAD_ENABLED = "bridge_upload_enabled"
        private const val KEY_RAW_DIAGNOSTICS = "raw_diagnostics_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
        private const val KEY_VF_CASH_ENABLED = "vf_cash_enabled"
        private const val KEY_BANK_ALAHLY_ENABLED = "bank_alahly_enabled"
        private const val KEY_LAST_HEARTBEAT_TIME = "last_heartbeat_timestamp"
        private const val KEY_SERVER_BUSY = "server_busy"
        private const val KEY_BUSY_SESSION_ID = "busy_session_id"
        private const val KEY_APP_THEME = "app_theme_preference"
        private const val KEY_LAST_SERVER_RESPONSE = "last_server_response_body"
        private const val KEY_LAST_SERVER_STATUS_CODE = "last_server_status_code"
        private const val KEY_ACTIVE_RULES_COUNT = "active_rules_count"
        private const val KEY_LAST_DETECTED_NOTIFICATION = "last_detected_notification"
        private const val KEY_LAST_PARSED_AMOUNT_MINOR = "last_parsed_amount_minor"
        private const val KEY_LAST_PARSED_TIME = "last_parsed_time"

        const val DEFAULT_API_BASE_URL = "https://elmallah-admin3.vercel.app"
        const val THEME_SYSTEM = "SYSTEM"
        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
    }

    init {
        ensureDeviceId()
        ensureDeviceSecret()
    }

    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: ensureDeviceId()

    private fun ensureDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing

        val androidId = Settings.Secure.getString(
            appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ).orEmpty()

        val suffix = if (androidId.isNotBlank()) {
            val seed = "$androidId|${appContext.packageName}|almallah-release-v1"
            MessageDigest.getInstance("SHA-256")
                .digest(seed.toByteArray(Charsets.UTF_8))
                .take(6)
                .joinToString("") { "%02x".format(it) }
        } else {
            UUID.randomUUID().toString().replace("-", "").take(12)
        }

        val newId = "pos-almallah-$suffix"
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    fun getDeviceSecret(): String {
        val cipher = prefs.getString(KEY_SECRET_CIPHER, null)
        val iv = prefs.getString(KEY_SECRET_IV, null)

        if (!cipher.isNullOrBlank() && !iv.isNullOrBlank()) {
            val decrypted = AndroidKeystoreHelper.decrypt(cipher, iv)
            if (!decrypted.isNullOrBlank()) return decrypted

            if (prefs.getBoolean(KEY_IS_PROVISIONED, false)) {
                prefs.edit()
                    .putBoolean(KEY_IS_PROVISIONED, false)
                    .putBoolean(KEY_BRIDGE_UPLOAD_ENABLED, false)
                    .apply()
                throw IllegalStateException("Stored HMAC secret could not be decrypted")
            }
        }
        return ensureDeviceSecret()
    }

    /**
     * Provision or rotate the authoritative HMAC key issued by elmallah-admin3.
     * Stored securely in Android Keystore. The key is never displayed back to the UI.
     */
    fun setDeviceSecret(secret: String): Boolean {
        val clean = secret.trim()
        if (clean.length < 32) return false

        val encrypted = AndroidKeystoreHelper.encrypt(clean) ?: return false
        val roundTrip = AndroidKeystoreHelper.decrypt(encrypted.first, encrypted.second)
        if (roundTrip != clean) return false

        prefs.edit()
            .putString(KEY_SECRET_CIPHER, encrypted.first)
            .putString(KEY_SECRET_IV, encrypted.second)
            .putBoolean(KEY_IS_PROVISIONED, true)
            .putBoolean(KEY_BRIDGE_UPLOAD_ENABLED, true)
            .apply()

        return getDeviceSecret() == clean
    }

    var isProvisioned: Boolean
        get() = prefs.getBoolean(KEY_IS_PROVISIONED, false)
        set(value) = prefs.edit().putBoolean(KEY_IS_PROVISIONED, value).apply()

    private fun ensureDeviceSecret(): String {
        // Bootstrap-only fallback random secret.
        val randomBytes = ByteArray(32)
        SecureRandom().nextBytes(randomBytes)
        val generated = randomBytes.joinToString("") { "%02x".format(it) }
        val encrypted = AndroidKeystoreHelper.encrypt(generated)
        if (encrypted != null) {
            prefs.edit()
                .putString(KEY_SECRET_CIPHER, encrypted.first)
                .putString(KEY_SECRET_IV, encrypted.second)
                .apply()
        }
        return generated
    }

    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
        set(value) {
            val clean = value.trim().trimEnd('/')
            if (!clean.startsWith("https://", ignoreCase = true) && !BuildConfig.DEBUG) {
                throw IllegalArgumentException("Production API Base URL must use HTTPS strictly.")
            }
            prefs.edit().putString(KEY_API_BASE_URL, clean).apply()
        }

    var bridgeUploadEnabled: Boolean
        get() = prefs.getBoolean(KEY_BRIDGE_UPLOAD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BRIDGE_UPLOAD_ENABLED, value).apply()

    var vfCashEnabled: Boolean
        get() = prefs.getBoolean(KEY_VF_CASH_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_VF_CASH_ENABLED, value).apply()

    var bankAlAhlyEnabled: Boolean
        get() = prefs.getBoolean(KEY_BANK_ALAHLY_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BANK_ALAHLY_ENABLED, value).apply()

    var rawDiagnosticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAW_DIAGNOSTICS, false)
        set(value) = prefs.edit().putBoolean(KEY_RAW_DIAGNOSTICS, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()

    var lastHeartbeatTimestamp: Long
        get() = prefs.getLong(KEY_LAST_HEARTBEAT_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_HEARTBEAT_TIME, value).apply()

    var serverBusy: Boolean
        get() = prefs.getBoolean(KEY_SERVER_BUSY, false)
        set(value) = prefs.edit().putBoolean(KEY_SERVER_BUSY, value).apply()

    var busySessionId: String?
        get() = prefs.getString(KEY_BUSY_SESSION_ID, null)
        set(value) = prefs.edit().putString(KEY_BUSY_SESSION_ID, value).apply()

    var appTheme: String
        get() = prefs.getString(KEY_APP_THEME, THEME_SYSTEM) ?: THEME_SYSTEM
        set(value) = prefs.edit().putString(KEY_APP_THEME, value).apply()

    var lastServerResponse: String?
        get() = prefs.getString(KEY_LAST_SERVER_RESPONSE, null)
        set(value) = prefs.edit().putString(KEY_LAST_SERVER_RESPONSE, value).apply()

    var lastServerStatusCode: Int
        get() = prefs.getInt(KEY_LAST_SERVER_STATUS_CODE, 0)
        set(value) = prefs.edit().putInt(KEY_LAST_SERVER_STATUS_CODE, value).apply()

    var activeRulesCount: Int
        get() = prefs.getInt(KEY_ACTIVE_RULES_COUNT, 0)
        set(value) = prefs.edit().putInt(KEY_ACTIVE_RULES_COUNT, value).apply()

    var lastDetectedNotification: String?
        get() = prefs.getString(KEY_LAST_DETECTED_NOTIFICATION, null)
        set(value) = prefs.edit().putString(KEY_LAST_DETECTED_NOTIFICATION, value).apply()

    var lastParsedAmountMinor: Long
        get() = prefs.getLong(KEY_LAST_PARSED_AMOUNT_MINOR, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PARSED_AMOUNT_MINOR, value).apply()

    var lastParsedTime: Long
        get() = prefs.getLong(KEY_LAST_PARSED_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_PARSED_TIME, value).apply()
}
