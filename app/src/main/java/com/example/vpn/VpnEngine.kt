package com.example.vpn

import com.example.model.VpnServer

interface VpnEngine {
    fun setTunFd(fd: Int)
    fun start(server: VpnServer)
    fun stop()
    fun isRunning(): Boolean
}
