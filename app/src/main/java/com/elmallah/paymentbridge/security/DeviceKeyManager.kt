package com.elmallah.paymentbridge.security

import android.content.Context
import android.content.SharedPreferences
import com.elmallah.paymentbridge.BuildConfig
import java.security.SecureRandom
import java.util.UUID

class DeviceKeyManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREFS_NAME = "almallah_secure_device_credentials"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_SECRET_CIPHER = "device_secret_cipher_v2"
        private const val KEY_SECRET_IV = "device_secret_iv_v2"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_BRIDGE_UPLOAD_ENABLED = "bridge_upload_enabled"
        private const val KEY_RAW_DIAGNOSTICS = "raw_diagnostics_enabled"
        private const val KEY_LAST_SYNC_TIME = "last_sync_timestamp"
        private const val KEY_VF_CASH_ENABLED = "vf_cash_enabled"
        private const val KEY_BANK_ALAHLY_ENABLED = "bank_alahly_enabled"
        private const val KEY_LAST_HEARTBEAT_TIME = "last_heartbeat_timestamp"
        private const val KEY_SERVER_BUSY = "server_busy"
        private const val KEY_BUSY_SESSION_ID = "busy_session_id"

        const val DEFAULT_API_BASE_URL = "https://elmallah-admin3.example.com"
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
        val newId = "pos-almallah-" + UUID.randomUUID().toString().take(12)
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    fun getDeviceSecret(): String {
        val cipher = prefs.getString(KEY_SECRET_CIPHER, null)
        val iv = prefs.getString(KEY_SECRET_IV, null)

        if (!cipher.isNullOrBlank() && !iv.isNullOrBlank()) {
            val decrypted = AndroidKeystoreHelper.decrypt(cipher, iv)
            if (!decrypted.isNullOrBlank()) return decrypted
        }
        return ensureDeviceSecret()
    }

    /** Provision/rotate the HMAC key issued by admin3. */
    fun setDeviceSecret(secret: String): Boolean {
        val clean = secret.trim()
        if (clean.length < 32) return false
        val encrypted = AndroidKeystoreHelper.encrypt(clean) ?: return false
        prefs.edit()
            .putString(KEY_SECRET_CIPHER, encrypted.first)
            .putString(KEY_SECRET_IV, encrypted.second)
            .apply()
        return true
    }

    private fun ensureDeviceSecret(): String {
        // Bootstrap-only random secret. It cannot authenticate until the same device
        // is registered in admin3. Production setup replaces it with the one-time
        // provisioning secret issued by the admin dashboard.
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

    /**
     * Phase 2 upload remains off on a fresh install until the admin3-issued HMAC
     * key is provisioned. SettingsViewModel enables it after successful provisioning.
     */
    var bridgeUploadEnabled: Boolean
        get() = prefs.getBoolean(KEY_BRIDGE_UPLOAD_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BRIDGE_UPLOAD_ENABLED, value).apply()

    var vfCashEnabled: Boolean
        get() = prefs.getBoolean(KEY_VF_CASH_ENABLED, true)
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
}
