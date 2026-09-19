package com.elmallah.paymentbridge.sync

import com.elmallah.paymentbridge.network.ActivePaymentOrderDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Ephemeral, server-authoritative bridge state.
 *
 * Order/payment status is never inferred from SMS data on the phone. admin3
 * sends the live orders assigned to this device with each heartbeat.
 */
object BridgeRuntimeState {
    private val _activePaymentOrders = MutableStateFlow<List<ActivePaymentOrderDto>>(emptyList())
    val activePaymentOrders: StateFlow<List<ActivePaymentOrderDto>> = _activePaymentOrders.asStateFlow()

    fun updateActivePaymentOrders(orders: List<ActivePaymentOrderDto>) {
        _activePaymentOrders.value = orders
            .filter { it.paymentState == "waiting" && it.orderStatus == "pending" }
            .distinctBy { it.sessionId }
    }

    fun clearActivePaymentOrders() {
        _activePaymentOrders.value = emptyList()
    }
}
