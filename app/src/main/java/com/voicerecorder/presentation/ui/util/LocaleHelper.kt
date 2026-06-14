package com.voicerecorder.presentation.ui.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

object LocaleHelper {
    fun applyLocale(
        context: Context,
        languageCode: String,
    ): Context {
        if (languageCode == "system") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val localeManager = context.getSystemService(LocaleManager::class.java)
                if (localeManager != null) {
                    val currentLocales = localeManager.applicationLocales
                    if (!currentLocales.isEmpty) {
                        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
                    }
                }
            }
            return context
        }

        // Android 13+ native language switching support
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val localeManager = context.getSystemService(LocaleManager::class.java)
            if (localeManager != null) {
                val currentLocales = localeManager.applicationLocales
                if (currentLocales.isEmpty || currentLocales.get(0).language != languageCode) {
                    localeManager.applicationLocales = LocaleList.forLanguageTags(languageCode)
                }
            }
        }

        // Legacy system configuration fallback context wrapper for backwards compatibility
        val locale = Locale(languageCode)
        Locale.setDefault(locale)

        val resources = context.resources
        val configuration = Configuration(resources.configuration)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            configuration.setLocale(locale)
            return context.createConfigurationContext(configuration)
        } else {
            @Suppress("DEPRECATION")
            configuration.locale = locale
            @Suppress("DEPRECATION")
            resources.updateConfiguration(configuration, resources.displayMetrics)
            return context
        }
    }
}
