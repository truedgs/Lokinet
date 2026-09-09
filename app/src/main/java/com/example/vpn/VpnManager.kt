package com.example.vpn

import android.content.Context
import android.content.Intent
import com.example.model.VpnServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class VpnManager(private val context: Context) {
    private val _engine = LokiVpnEngine(context)
    val vpnState = _engine.vpnState
    
    private val _selectedServer = MutableStateFlow<VpnServer?>(null)
    val selectedServer: StateFlow<VpnServer?> = _selectedServer.asStateFlow()

    fun setSelectedServer(server: VpnServer) {
        _selectedServer.value = server
    }

    fun connect() {
        val server = _selectedServer.value ?: return

        // Assign the engine BEFORE starting the service. startForegroundService() is
        // asynchronous, so if engine were assigned after this call there's a window
        // where onStartCommand()/setupVpn() could run and find LokiVpnService.engine
        // still null, silently dropping the TUN fd.
        LokiVpnService.engine = _engine
        _engine.start(server)

        val intent = Intent(context, LokiVpnService::class.java).apply {
            action = LokiVpnService.ACTION_START
        }
        context.startForegroundService(intent)
    }

    fun disconnect() {
        val intent = Intent(context, LokiVpnService::class.java).apply {
            action = LokiVpnService.ACTION_STOP
        }
        context.startService(intent)
    }
}
