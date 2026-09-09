package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "servers")
data class VpnServer(
    @PrimaryKey
    val id: String,
    val name: String,
    val country: String,
    val protocol: String,
    val tier: String,
    val uri: String
)
