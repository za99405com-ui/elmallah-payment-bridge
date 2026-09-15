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

        const val DEFAULT_API_BASE_URL = "https://elmallah-admin3.example.com"
    }

    init {
        ensureDeviceId()
        ensureDeviceSecret()
    }

    /**
     * Non-secret persistent unique identifier for this Android merchant phone.
     */
    val deviceId: String
        get() = prefs.getString(KEY_DEVICE_ID, null) ?: ensureDeviceId()

    private fun ensureDeviceId(): String {
        val existing = prefs.getString(KEY_DEVICE_ID, null)
        if (!existing.isNullOrBlank()) return existing
        val newId = "pos-almallah-" + UUID.randomUUID().toString().take(12)
        prefs.edit().putString(KEY_DEVICE_ID, newId).apply()
        return newId
    }

    /**
     * Retrieves the provisioned financial HMAC device secret.
     * Decrypted on-the-fly using hardware-backed Android Keystore master key.
     */
    fun getDeviceSecret(): String {
        val cipher = prefs.getString(KEY_SECRET_CIPHER, null)
        val iv = prefs.getString(KEY_SECRET_IV, null)

        if (!cipher.isNullOrBlank() && !iv.isNullOrBlank()) {
            val decrypted = AndroidKeystoreHelper.decrypt(cipher, iv)
            if (!decrypted.isNullOrBlank()) {
                return decrypted
            }
        }
        return ensureDeviceSecret()
    }

    /**
     * Securely provisions a new HMAC device secret, encrypting it with Android Keystore AES-GCM.
     */
    fun setDeviceSecret(secret: String): Boolean {
        val encrypted = AndroidKeystoreHelper.encrypt(secret.trim()) ?: return false
        prefs.edit()
            .putString(KEY_SECRET_CIPHER, encrypted.first)
            .putString(KEY_SECRET_IV, encrypted.second)
            .apply()
        return true
    }

    private fun ensureDeviceSecret(): String {
        // Generate cryptographically secure random 256-bit secret if not present
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

    /**
     * Base URL for the elmallah-admin3 backend.
     * Strict requirement: Must use HTTPS in production.
     */
    var apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL
        set(value) {
            val clean = value.trim()
            if (!clean.startsWith("https://", ignoreCase = true)) {
                if (!BuildConfig.DEBUG) {
                    throw IllegalArgumentException("Production API Base URL must use HTTPS strictly.")
                }
            }
            prefs.edit().putString(KEY_API_BASE_URL, clean).apply()
        }

    /**
     * PHASE 1 MANDATE: Hard-locked to CAPTURE_ONLY (bridgeUploadEnabled = false).
     * When false: incoming payments are parsed, deduplicated, and stored locally in Room.
     * No network upload or retries are dispatched. User cannot toggle this in Phase 1.
     */
    var bridgeUploadEnabled: Boolean
        get() = false
        set(@Suppress("UNUSED_PARAMETER") value) {
            // Hard-locked in Phase 1: no upload allowed
        }

    /**
     * Raw diagnostic message capture is OFF by default.
     */
    var rawDiagnosticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAW_DIAGNOSTICS, false)
        set(value) = prefs.edit().putBoolean(KEY_RAW_DIAGNOSTICS, value).apply()

    var lastSyncTimestamp: Long
        get() = prefs.getLong(KEY_LAST_SYNC_TIME, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC_TIME, value).apply()
}
