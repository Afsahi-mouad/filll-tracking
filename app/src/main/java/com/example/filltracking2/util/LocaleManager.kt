package com.example.filltracking2.util

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

object LocaleManager {
    val LocalAppLocale = staticCompositionLocalOf { "en" }

    fun wrapContext(context: Context, language: String): Context {
        val locale = Locale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLayoutDirection(locale)
        return context.createConfigurationContext(config)
    }
}
