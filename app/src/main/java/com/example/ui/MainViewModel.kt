package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.remote.ApiClient
import com.example.data.repository.RemoteConfigRepository
import com.example.model.VpnServer
import com.example.utils.HwidManager
import com.example.vpn.VpnManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = AppDatabase.getDatabase(application)
    private val repository = RemoteConfigRepository(ApiClient.githubApiService, database.serverDao())
    private val hwidManager = HwidManager(application)
    val vpnManager = VpnManager(application)

    private val _hwid = MutableStateFlow("")
    val hwid: StateFlow<String> = _hwid.asStateFlow()

    private val _isPremium = MutableStateFlow(false)
    val isPremium: StateFlow<Boolean> = _isPremium.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    val servers = repository.allServers.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _hwid.value = hwidManager.getHwid()
            refreshData()
        }
    }

    fun refreshData() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            val configResult = repository.refreshServers()
            val premiumResult = repository.checkPremiumStatus(_hwid.value)
            
            if (premiumResult.isSuccess) {
                _isPremium.value = premiumResult.getOrDefault(false)
            }
            
            if (configResult.isFailure) {
                _error.value = "Failed to update from remote. Showing cached servers."
            }
            _isLoading.value = false
        }
    }

    fun selectServer(server: VpnServer) {
        if (server.tier == "PREMIUM" && !_isPremium.value) {
            _error.value = "This server requires Premium access."
            return
        }
        vpnManager.setSelectedServer(server)
    }

    fun clearError() {
        _error.value = null
    }
}
