package com.example.utils

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.dataStore by preferencesDataStore(name = "settings")

class HwidManager(private val context: Context) {
    companion object {
        private val HWID_KEY = stringPreferencesKey("hwid")
    }

    suspend fun getHwid(): String {
        var hwid = context.dataStore.data.map { preferences ->
            preferences[HWID_KEY]
        }.first()

        if (hwid == null) {
            hwid = generateHwid()
            context.dataStore.edit { preferences ->
                preferences[HWID_KEY] = hwid
            }
        }
        return hwid
    }

    private fun generateHwid(): String {
        // Generating a random UUID is privacy-conscious. 
        // Reinstalling or clearing data resets it.
        val uuid = UUID.randomUUID().toString().replace("-", "").uppercase()
        // Format to something like A8F3-29C1-7B82-XXXX
        return "${uuid.substring(0, 4)}-${uuid.substring(4, 8)}-${uuid.substring(8, 12)}-${uuid.substring(12, 16)}"
    }
}
