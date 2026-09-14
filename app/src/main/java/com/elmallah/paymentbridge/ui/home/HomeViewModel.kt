package com.elmallah.paymentbridge.ui.home

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.capture.PaymentNotificationListener
import com.elmallah.paymentbridge.data.PaymentEventEntity
import com.elmallah.paymentbridge.data.PaymentRepository
import com.elmallah.paymentbridge.domain.PaymentProvider
import com.elmallah.paymentbridge.domain.SyncStatus
import com.elmallah.paymentbridge.security.DeviceKeyManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EventFilter(val label: String) {
    ALL("الكل"),
    VODAFONE("فودافون كاش"),
    NBE("البنك الأهلي"),
    DUPLICATE("المكررة")
}

data class TodayMetrics(
    val eventCount: Int = 0,
    val totalAmountMinor: Long = 0L
) {
    val totalAmountMajor: Double
        get() = totalAmountMinor / 100.0
}

class HomeViewModel(
    private val appContext: Context,
    private val repository: PaymentRepository,
    private val keyManager: DeviceKeyManager
) : ViewModel() {

    private val _isNotificationAccessGranted = MutableStateFlow(checkNotificationAccess(appContext))
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted

    private val _isCaptureOnlyMode = MutableStateFlow(!keyManager.bridgeUploadEnabled)
    val isCaptureOnlyMode: StateFlow<Boolean> = _isCaptureOnlyMode

    val allEvents: StateFlow<List<PaymentEventEntity>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayMetrics: StateFlow<TodayMetrics> = combine(
        repository.getTodayEventCount(),
        repository.getTodayTotalAmountMinor()
    ) { count, totalMinor ->
        TodayMetrics(count, totalMinor ?: 0L)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayMetrics())

    val selectedFilter = MutableStateFlow(EventFilter.ALL)

    val filteredEvents: StateFlow<List<PaymentEventEntity>> = combine(
        allEvents,
        selectedFilter
    ) { events, filter ->
        when (filter) {
            EventFilter.ALL -> events
            EventFilter.VODAFONE -> events.filter { it.provider == PaymentProvider.VODAFONE_CASH }
            EventFilter.NBE -> events.filter { it.provider == PaymentProvider.NBE_INCOMING_TRANSFER }
            EventFilter.DUPLICATE -> events.filter { it.syncStatus == SyncStatus.DUPLICATE.name }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedEventForDetail = MutableStateFlow<PaymentEventEntity?>(null)

    fun refreshNotificationAccess() {
        _isNotificationAccessGranted.value = checkNotificationAccess(appContext)
        _isCaptureOnlyMode.value = !keyManager.bridgeUploadEnabled
    }

    fun selectFilter(filter: EventFilter) {
        selectedFilter.value = filter
    }

    fun selectEvent(event: PaymentEventEntity?) {
        selectedEventForDetail.value = event
    }

    companion object {
        fun checkNotificationAccess(context: Context): Boolean {
            val componentName = ComponentName(context, PaymentNotificationListener::class.java)
            val flat = Settings.Secure.getString(
                context.contentResolver,
                "enabled_notification_listeners"
            )
            return flat != null && flat.contains(componentName.flattenToString())
        }
    }
}
