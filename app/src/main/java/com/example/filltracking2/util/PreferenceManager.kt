package com.example.filltracking2.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceManager {
    private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    private val USER_PASSWORD = stringPreferencesKey("user_password")

    // Locale Management
    fun getLocale(context: Context): Flow<String> = context.dataStore.data.map { 
        it[SELECTED_LANGUAGE] ?: "en" 
    }

    suspend fun setLocale(context: Context, languageCode: String) {
        context.dataStore.edit { it[SELECTED_LANGUAGE] = languageCode }
        // Removed applyLocale to prevent activity restart
    }

    fun applyLocale(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    // Password Management
    fun getPassword(context: Context): Flow<String> = context.dataStore.data.map { 
        it[USER_PASSWORD] ?: "admin" 
    }

    suspend fun setPassword(context: Context, newPassword: String) {
        context.dataStore.edit { it[USER_PASSWORD] = newPassword }
    }
}
