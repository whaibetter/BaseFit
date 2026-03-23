package com.basefit.app.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.basefit.app.ui.theme.AppThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "theme_prefs")

class ThemePreferencesManager(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")

    val themeModeFlow: Flow<AppThemeMode> = context.dataStore.data.map { preferences ->
        val themeName = preferences[themeModeKey] ?: AppThemeMode.SYSTEM.name
        try {
            AppThemeMode.valueOf(themeName)
        } catch (e: IllegalArgumentException) {
            AppThemeMode.SYSTEM
        }
    }

    suspend fun setThemeMode(themeMode: AppThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[themeModeKey] = themeMode.name
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: ThemePreferencesManager? = null

        fun getInstance(context: Context): ThemePreferencesManager {
            return INSTANCE ?: synchronized(this) {
                val instance = ThemePreferencesManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}