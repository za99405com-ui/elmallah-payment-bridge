package com.example.paymentbridge.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paymentbridge.data.PaymentRepository
import com.example.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class ConnectionTestState {
    object Idle : ConnectionTestState()
    object Testing : ConnectionTestState()
    data class Success(val message: String) : ConnectionTestState()
    data class Error(val error: String) : ConnectionTestState()
}

class SettingsViewModel(
    private val keyManager: DeviceKeyManager,
    private val repository: PaymentRepository
) : ViewModel() {

    private val _baseUrl = MutableStateFlow(keyManager.apiBaseUrl)
    val baseUrl: StateFlow<String> = _baseUrl.asStateFlow()

    private val _deviceId = MutableStateFlow(keyManager.deviceId)
    val deviceId: StateFlow<String> = _deviceId.asStateFlow()

    private val _deviceSecret = MutableStateFlow(keyManager.deviceSecret)
    val deviceSecret: StateFlow<String> = _deviceSecret.asStateFlow()

    private val _rawDiagnostics = MutableStateFlow(keyManager.isRawDiagnosticsEnabled)
    val rawDiagnostics: StateFlow<Boolean> = _rawDiagnostics.asStateFlow()

    private val _connectionTestState = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTestState: StateFlow<ConnectionTestState> = _connectionTestState.asStateFlow()

    private val _saveStatusMessage = MutableStateFlow<String?>(null)
    val saveStatusMessage: StateFlow<String?> = _saveStatusMessage.asStateFlow()

    fun onBaseUrlChanged(newUrl: String) {
        _baseUrl.value = newUrl
    }

    fun onDeviceSecretChanged(newSecret: String) {
        _deviceSecret.value = newSecret
    }

    fun saveSettings() {
        keyManager.updateApiBaseUrl(_baseUrl.value)
        keyManager.updateDeviceSecret(_deviceSecret.value)
        _saveStatusMessage.value = "تم حفظ الإعدادات بنجاح"
    }

    fun toggleRawDiagnostics(enabled: Boolean) {
        keyManager.setRawDiagnosticsEnabled(enabled)
        _rawDiagnostics.value = enabled
    }

    fun regenerateSecret() {
        val newSecret = keyManager.regenerateSecret()
        _deviceSecret.value = newSecret
        _saveStatusMessage.value = "تم إنشاء مفتاح سري جديد (تأكد من تحديثه في لوحة الإدارة)"
    }

    fun testConnection() {
        _connectionTestState.value = ConnectionTestState.Testing
        viewModelScope.launch {
            val result = repository.testServerConnection()
            if (result.isSuccess) {
                val resp = result.getOrNull()
                _connectionTestState.value = ConnectionTestState.Success(
                    "الاتصال ناجح! استجاب السيرفر بالحالة: ${resp?.status ?: "OK"}"
                )
            } else {
                _connectionTestState.value = ConnectionTestState.Error(
                    "تعذر الاتصال: ${result.exceptionOrNull()?.message ?: "خطأ غير معروف"}"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _saveStatusMessage.value = null
    }
}
