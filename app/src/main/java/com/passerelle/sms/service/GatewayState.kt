package com.passerelle.sms.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object GatewayState {
    private val _running = MutableStateFlow(false)
    val running: StateFlow<Boolean> = _running.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val _listenUrl = MutableStateFlow<String?>(null)
    val listenUrl: StateFlow<String?> = _listenUrl.asStateFlow()

    private val _localIps = MutableStateFlow<List<String>>(emptyList())
    val localIps: StateFlow<List<String>> = _localIps.asStateFlow()

    fun setRunning(value: Boolean) {
        _running.value = value
    }

    fun setError(message: String?) {
        _lastError.value = message
    }

    fun setListenUrl(url: String?) {
        _listenUrl.value = url
    }

    fun setLocalIps(ips: List<String>) {
        _localIps.value = ips
    }
}
