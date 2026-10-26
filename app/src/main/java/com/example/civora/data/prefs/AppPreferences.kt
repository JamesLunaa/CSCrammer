package com.example.civora.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.civora.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppPreferences(private val store: DataStore<Preferences>) {

    val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME_MODE]?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } } ?: ThemeMode.Dark
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME_MODE] = mode.name }
    }

    suspend fun dataVersion(): Int = store.data.first()[DATA_VERSION] ?: 0

    suspend fun setDataVersion(version: Int) {
        store.edit { it[DATA_VERSION] = version }
    }

    private companion object {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DATA_VERSION = intPreferencesKey("data_version")
    }
}
