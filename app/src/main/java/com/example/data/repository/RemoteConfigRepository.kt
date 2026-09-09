package com.example.data.repository

import com.example.data.local.ServerDao
import com.example.data.remote.GithubApiService
import com.example.model.VpnServer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class RemoteConfigRepository(
    private val apiService: GithubApiService,
    private val serverDao: ServerDao
) {
    // Configurable URLs
    private val configUrl = "https://raw.githubusercontent.com/mrkonaymyoaung/backup/refs/heads/main/servers.json"
    private val premiumUrl = "https://raw.githubusercontent.com/mrkonaymyoaung/backup/refs/heads/main/premium.txt"

    val allServers: Flow<List<VpnServer>> = serverDao.getAllServers()

    suspend fun refreshServers(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val remoteConfig = apiService.getRemoteConfig(configUrl)
            
            val validServers = remoteConfig.servers.mapNotNull { config ->
                try {
                    val inferredProtocol = config.protocol ?: when {
                        config.uri.startsWith("vless://") -> "VLESS"
                        config.uri.startsWith("vmess://") -> "VMESS"
                        config.uri.startsWith("trojan://") -> "TROJAN"
                        config.uri.startsWith("ss://") -> "SHADOWSOCKS"
                        else -> "UNKNOWN"
                    }

                    VpnServer(
                        id = config.id,
                        name = config.name,
                        country = config.country ?: "Unknown",
                        protocol = inferredProtocol.uppercase(),
                        tier = config.tier.uppercase(),
                        uri = config.uri
                    )
                } catch (e: Exception) {
                    null
                }
            }

            if (validServers.isNotEmpty()) {
                serverDao.clearServers()
                serverDao.insertAll(validServers)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun checkPremiumStatus(hwid: String): Result<Boolean> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPremiumHwids(premiumUrl)
            val hwids = response.string().lines().map { it.trim() }.filter { it.isNotEmpty() }
            Result.success(hwids.contains(hwid))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
