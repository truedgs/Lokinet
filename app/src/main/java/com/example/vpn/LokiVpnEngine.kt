package com.example.vpn

import android.content.Context
import android.util.Log
import com.example.model.VpnServer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.example.model.VpnState
import java.util.concurrent.atomic.AtomicBoolean

class LokiVpnEngine(private val context: Context) : VpnEngine {

    private val _vpnState = MutableStateFlow(VpnState.DISCONNECTED)
    val vpnState: StateFlow<VpnState> = _vpnState.asStateFlow()
    
    private val isEngineRunning = AtomicBoolean(false)
    private var currentTunFd: Int = -1

    override fun setTunFd(fd: Int) {
        this.currentTunFd = fd
        Log.d("LokiVpnEngine", "Received TUN File Descriptor: $fd")
    }

    override fun start(server: VpnServer) {
        if (isEngineRunning.getAndSet(true)) return
        Log.i("LokiVpnEngine", "Starting VPN Engine for: ${server.name}")
        _vpnState.value = VpnState.CONNECTING
        
        Thread {
            try {
                // 1. Initialize the Core Environment
                XrayCoreWrapper.initCoreEnv(context.filesDir.absolutePath)
                
                // 2. We wait up to 3 seconds for the VPN Service to provide the tunFd
                var waitCount = 0
                while (currentTunFd == -1 && waitCount < 30) {
                    Thread.sleep(100)
                    waitCount++
                }

                if (currentTunFd == -1) {
                    Log.e("LokiVpnEngine", "Failed to get Android TUN File Descriptor")
                    _vpnState.value = VpnState.ERROR
                    isEngineRunning.set(false)
                    return@Thread
                }

                // 3. Parse URI to full Xray/V2Ray client JSON config
                val configContent = try {
                    val json = V2rayConfigBuilder.buildConfig(server.uri)
                    Log.d("LokiVpnEngine", "Generated Xray Config:\n$json")
                    json
                } catch (e: Exception) {
                    // Do NOT fall back to an empty/no-outbound config - Xray would "start"
                    // successfully but route nothing, which is indistinguishable from a
                    // silent disconnect from the user's point of view. Fail loudly instead.
                    Log.e("LokiVpnEngine", "Failed to parse server URI into Xray config: ${e.message}", e)
                    _vpnState.value = VpnState.ERROR
                    isEngineRunning.set(false)
                    return@Thread
                }

                // 4. Start the Xray Core Loop with the FD
                Log.i("LokiVpnEngine", "Starting Xray Core Loop with FD: $currentTunFd")
                val started = XrayCoreWrapper.startLoop(configContent, currentTunFd)

                if (started) {
                    _vpnState.value = VpnState.CONNECTED
                } else {
                    Log.e("LokiVpnEngine", "Xray core reported it did not start")
                    _vpnState.value = VpnState.ERROR
                    isEngineRunning.set(false)
                }
            } catch (e: Exception) {
                Log.e("LokiVpnEngine", "Error starting VPN", e)
                _vpnState.value = VpnState.ERROR
                isEngineRunning.set(false)
            }
        }.start()
    }

    override fun stop() {
        if (!isEngineRunning.getAndSet(false)) return
        Log.i("LokiVpnEngine", "Stopping VPN Engine")
        _vpnState.value = VpnState.DISCONNECTING
        
        Thread {
            XrayCoreWrapper.stopLoop()
            currentTunFd = -1
            Thread.sleep(500)
            _vpnState.value = VpnState.DISCONNECTED
        }.start()
    }

    override fun isRunning(): Boolean = isEngineRunning.get()
}
