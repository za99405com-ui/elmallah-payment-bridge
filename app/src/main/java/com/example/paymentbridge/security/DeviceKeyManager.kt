package com.example.paymentbridge.security

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom
import java.util.UUID

class DeviceKeyManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    val deviceId: String
        get() {
            var id = prefs.getString(KEY_DEVICE_ID, null)
            if (id.isNullOrBlank()) {
                id = "almallah-pos-" + UUID.randomUUID().toString().take(12)
                prefs.edit().putString(KEY_DEVICE_ID, id).apply()
            }
            return id
        }

    val deviceSecret: String
        get() {
            var secret = prefs.getString(KEY_DEVICE_SECRET, null)
            if (secret.isNullOrBlank()) {
                secret = generateSecureSecretHex()
                prefs.edit().putString(KEY_DEVICE_SECRET, secret).apply()
            }
            return secret
        }

    val apiBaseUrl: String
        get() = prefs.getString(KEY_API_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL

    val isRawDiagnosticsEnabled: Boolean
        get() = prefs.getBoolean(KEY_RAW_DIAGNOSTICS, false)

    fun updateApiBaseUrl(newUrl: String) {
        val clean = newUrl.trim().removeSuffix("/")
        prefs.edit().putString(KEY_API_BASE_URL, clean).apply()
    }

    fun updateDeviceSecret(newSecret: String) {
        prefs.edit().putString(KEY_DEVICE_SECRET, newSecret.trim()).apply()
    }

    fun setRawDiagnosticsEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RAW_DIAGNOSTICS, enabled).apply()
    }

    fun regenerateSecret(): String {
        val newSecret = generateSecureSecretHex()
        prefs.edit().putString(KEY_DEVICE_SECRET, newSecret).apply()
        return newSecret
    }

    private fun generateSecureSecretHex(): String {
        val bytes = ByteArray(32)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val PREFS_NAME = "almallah_bridge_secure_prefs"
        private const val KEY_DEVICE_ID = "device_id"
        private const val KEY_DEVICE_SECRET = "device_secret"
        private const val KEY_API_BASE_URL = "api_base_url"
        private const val KEY_RAW_DIAGNOSTICS = "raw_diagnostics"

        // Default URL for elmallah-admin3 backend API
        const val DEFAULT_BASE_URL = "https://elmallah-admin3.vercel.app"
    }
}
