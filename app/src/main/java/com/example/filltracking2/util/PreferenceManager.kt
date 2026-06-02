package com.example.filltracking2.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

object PreferenceManager {
    private val SELECTED_LANGUAGE = stringPreferencesKey("selected_language")
    private val USER_PASSWORD = stringPreferencesKey("user_password")
    private val IS_LOGGED_IN = booleanPreferencesKey("is_logged_in")
    private val CURRENT_USER_EMAIL = stringPreferencesKey("current_user_email")

    // Locale Management
    fun getLocale(context: Context): Flow<String> = context.dataStore.data.map { 
        it[SELECTED_LANGUAGE] ?: "ar"
    }

    suspend fun setLocale(context: Context, languageCode: String) {
        context.dataStore.edit { it[SELECTED_LANGUAGE] = languageCode }
        applyLocale(languageCode)
    }

    fun applyLocale(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        val currentLocales = AppCompatDelegate.getApplicationLocales()
        
        // Only apply if the requested locale is different from the current one
        // to prevent activity recreation loops
        if (currentLocales.toLanguageTags() != languageCode) {
            AppCompatDelegate.setApplicationLocales(appLocale)
        }
    }

    // Password Management
    fun getPassword(context: Context): Flow<String> = context.dataStore.data.map { 
        it[USER_PASSWORD] ?: "admin" 
    }

    suspend fun setPassword(context: Context, newPassword: String) {
        context.dataStore.edit { it[USER_PASSWORD] = newPassword }
    }

    // Session Management
    fun isLoggedIn(context: Context): Flow<Boolean> = context.dataStore.data.map { 
        it[IS_LOGGED_IN] ?: false 
    }

    suspend fun setLoggedIn(context: Context, loggedIn: Boolean, email: String = "") {
        context.dataStore.edit { 
            it[IS_LOGGED_IN] = loggedIn
            it[CURRENT_USER_EMAIL] = email
        }
    }

    fun getCurrentUserEmail(context: Context): Flow<String> = context.dataStore.data.map { 
        it[CURRENT_USER_EMAIL] ?: ""
    }
}
