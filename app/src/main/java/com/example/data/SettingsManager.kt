package com.example.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    private val APP_NAME_KEY = stringPreferencesKey("app_name")

    val appNameFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[APP_NAME_KEY] ?: "My Wealth"
    }

    suspend fun saveAppName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[APP_NAME_KEY] = name
        }
    }
}
