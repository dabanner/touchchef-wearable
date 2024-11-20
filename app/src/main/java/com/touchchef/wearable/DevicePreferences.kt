package com.touchchef.wearable

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "device_settings")

class DevicePreferences(private val context: Context) {
    companion object {
        private val DEVICE_ID_KEY = stringPreferencesKey("device_id")
    }

    suspend fun saveDeviceId(deviceId: String) {
        context.dataStore.edit { preferences ->
            preferences[DEVICE_ID_KEY] = deviceId
        }
    }

    val deviceId: Flow<String?> = context.dataStore.data
        .map { preferences ->
            preferences[DEVICE_ID_KEY]
        }
}