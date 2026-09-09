package com.example.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteConfig(
    @Json(name = "version") val version: Int,
    @Json(name = "updatedAt") val updatedAt: String?,
    @Json(name = "servers") val servers: List<ServerConfig>
)

@JsonClass(generateAdapter = true)
data class ServerConfig(
    @Json(name = "id") val id: String,
    @Json(name = "name") val name: String,
    @Json(name = "country") val country: String?,
    @Json(name = "protocol") val protocol: String?,
    @Json(name = "tier") val tier: String,
    @Json(name = "uri") val uri: String
)
