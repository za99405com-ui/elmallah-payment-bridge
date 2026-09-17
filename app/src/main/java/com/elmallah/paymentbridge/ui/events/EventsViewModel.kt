package com.elmallah.paymentbridge.ui.events

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elmallah.paymentbridge.data.PaymentEventEntity
import com.elmallah.paymentbridge.data.PaymentRepository
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
    PENDING("معلقة"),
    UPLOADED("مرفوعة"),
    FAILED("فاشلة/مكررة")
}

class EventsViewModel(
    private val repository: PaymentRepository,
    val keyManager: DeviceKeyManager
) : ViewModel() {

    val allEvents: StateFlow<List<PaymentEventEntity>> = repository.allEventsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val selectedFilter = MutableStateFlow(EventFilter.ALL)

    val searchQuery = MutableStateFlow("")

    val filteredEvents: StateFlow<List<PaymentEventEntity>> = combine(
        allEvents,
        selectedFilter,
        searchQuery
    ) { events, filter, query ->
        val byFilter = when (filter) {
            EventFilter.ALL -> events
            EventFilter.PENDING -> events.filter {
                it.syncStatus == SyncStatus.PENDING_UPLOAD.name || it.syncStatus == SyncStatus.CAPTURE_ONLY.name
            }
            EventFilter.UPLOADED -> events.filter { it.syncStatus == SyncStatus.UPLOADED.name }
            EventFilter.FAILED -> events.filter {
                it.syncStatus == SyncStatus.FAILED_RETRYABLE.name ||
                    it.syncStatus == SyncStatus.REJECTED_BY_SERVER.name ||
                    it.syncStatus == SyncStatus.DUPLICATE.name
            }
        }

        if (query.isBlank()) {
            byFilter
        } else {
            val q = query.trim().lowercase()
            byFilter.filter {
                it.provider.lowercase().contains(q) ||
                    (it.payerPhone?.contains(q) == true) ||
                    (it.transactionReference.lowercase().contains(q)) ||
                    (it.accountLast4?.contains(q) == true) ||
                    (it.amountInMajorUnits.toString().contains(q))
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val expandedEventId = MutableStateFlow<String?>(null)

    fun toggleExpanded(eventId: String) {
        if (expandedEventId.value == eventId) {
            expandedEventId.value = null
        } else {
            expandedEventId.value = eventId
        }
    }

    fun selectFilter(filter: EventFilter) {
        selectedFilter.value = filter
    }

    fun updateSearchQuery(query: String) {
        searchQuery.value = query
    }
}
