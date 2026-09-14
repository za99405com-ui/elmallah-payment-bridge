package com.example.paymentbridge.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.paymentbridge.capture.NotificationPermissionHelper
import com.example.paymentbridge.data.PaymentEventEntity
import com.example.paymentbridge.data.PaymentRepository
import com.example.paymentbridge.data.TodayMetrics
import com.example.paymentbridge.domain.SyncStatus
import com.example.paymentbridge.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class EventFilter(val label: String) {
    ALL("الكل"),
    PENDING("بانتظار الإرسال"),
    UPLOADED("تم الإرسال"),
    MATCHED("تمت المطابقة ✅"),
    DUPLICATE("مكرر ⛔"),
    NEEDS_ATTENTION("يحتاج مراجعة ⚠️")
}

sealed class ServerConnectionState {
    object Unknown : ServerConnectionState()
    object Checking : ServerConnectionState()
    data class Connected(val serverTime: Long?) : ServerConnectionState()
    data class Disconnected(val error: String) : ServerConnectionState()
}

class HomeViewModel(
    private val context: Context,
    private val repository: PaymentRepository
) : ViewModel() {

    private val _isNotificationAccessGranted = MutableStateFlow(false)
    val isNotificationAccessGranted: StateFlow<Boolean> = _isNotificationAccessGranted.asStateFlow()

    private val _serverConnectionState = MutableStateFlow<ServerConnectionState>(ServerConnectionState.Unknown)
    val serverConnectionState: StateFlow<ServerConnectionState> = _serverConnectionState.asStateFlow()

    private val _selectedFilter = MutableStateFlow(EventFilter.ALL)
    val selectedFilter: StateFlow<EventFilter> = _selectedFilter.asStateFlow()

    private val _selectedEventForDetail = MutableStateFlow<PaymentEventEntity?>(null)
    val selectedEventForDetail: StateFlow<PaymentEventEntity?> = _selectedEventForDetail.asStateFlow()

    val todayMetrics: StateFlow<TodayMetrics> = repository.getTodayMetricsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), TodayMetrics(0, 0))

    val lastSyncTime: StateFlow<Long?> = repository.lastSyncTimeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val allEvents: StateFlow<List<PaymentEventEntity>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val filteredEvents: StateFlow<List<PaymentEventEntity>> = combine(
        allEvents,
        _selectedFilter
    ) { events, filter ->
        when (filter) {
            EventFilter.ALL -> events
            EventFilter.PENDING -> events.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD.name || it.syncStatus == SyncStatus.UPLOADING.name }
            EventFilter.UPLOADED -> events.filter { it.syncStatus == SyncStatus.UPLOADED.name }
            EventFilter.MATCHED -> events.filter { it.serverMatchStatus == "MATCHED" }
            EventFilter.DUPLICATE -> events.filter { it.syncStatus == SyncStatus.DUPLICATE.name }
            EventFilter.NEEDS_ATTENTION -> events.filter {
                it.syncStatus == SyncStatus.FAILED_RETRYABLE.name ||
                it.syncStatus == SyncStatus.REJECTED_BY_SERVER.name ||
                it.serverMatchStatus == "AMBIGUOUS" ||
                it.serverMatchStatus == "PENDING_REVIEW"
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshNotificationAccess()
        checkServerConnection()
    }

    fun refreshNotificationAccess() {
        _isNotificationAccessGranted.value = NotificationPermissionHelper.isNotificationAccessGranted(context)
    }

    fun checkServerConnection() {
        _serverConnectionState.value = ServerConnectionState.Checking
        viewModelScope.launch {
            val result = repository.testServerConnection()
            if (result.isSuccess) {
                _serverConnectionState.value = ServerConnectionState.Connected(result.getOrNull()?.serverTime)
            } else {
                _serverConnectionState.value = ServerConnectionState.Disconnected(
                    result.exceptionOrNull()?.message ?: "خطأ في الاتصال"
                )
            }
        }
    }

    fun setFilter(filter: EventFilter) {
        _selectedFilter.value = filter
    }

    fun selectEvent(event: PaymentEventEntity?) {
        _selectedEventForDetail.value = event
    }

    fun syncNow() {
        SyncScheduler.syncNow(context)
        checkServerConnection()
    }

    fun retryUpload(eventId: String) {
        viewModelScope.launch {
            repository.retryEventUpload(eventId)
        }
    }
}
