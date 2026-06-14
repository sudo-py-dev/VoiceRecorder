package com.voicerecorder.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.Language
import com.voicerecorder.domain.model.SaveLocation
import com.voicerecorder.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {
    companion object {
        private val KEY_THEME = stringPreferencesKey("theme_mode")
        private val KEY_LANGUAGE = stringPreferencesKey("language")
        private val KEY_FORMAT = stringPreferencesKey("audio_format")
        private val KEY_QUALITY = stringPreferencesKey("audio_quality")
        private val KEY_SAVE_LOCATION = stringPreferencesKey("save_location")
        private val KEY_PUBLIC_FOLDER_URI = stringPreferencesKey("public_folder_uri")
        private val KEY_PUBLIC_FOLDER_NAME = stringPreferencesKey("public_folder_name")
    }

    val themeModeFlow: Flow<ThemeMode> =
        context.dataStore.data.map { preferences ->
            val name = preferences[KEY_THEME] ?: ThemeMode.SYSTEM.name
            try {
                ThemeMode.valueOf(name)
            } catch (e: IllegalArgumentException) {
                ThemeMode.SYSTEM
            }
        }

    val languageFlow: Flow<Language> =
        context.dataStore.data.map { preferences ->
            val code = preferences[KEY_LANGUAGE] ?: Language.SYSTEM.code
            Language.fromCode(code)
        }

    val audioFormatFlow: Flow<AudioFormat> =
        context.dataStore.data.map { preferences ->
            val name = preferences[KEY_FORMAT] ?: AudioFormat.M4A.name
            AudioFormat.fromName(name)
        }

    val audioQualityFlow: Flow<AudioQuality> =
        context.dataStore.data.map { preferences ->
            val name = preferences[KEY_QUALITY] ?: AudioQuality.HIGH.name
            AudioQuality.fromName(name)
        }

    val saveLocationFlow: Flow<SaveLocation> =
        context.dataStore.data.map { preferences ->
            val name = preferences[KEY_SAVE_LOCATION] ?: SaveLocation.PRIVATE.name
            SaveLocation.fromName(name)
        }

    val publicFolderUriFlow: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[KEY_PUBLIC_FOLDER_URI] ?: ""
        }

    val publicFolderNameFlow: Flow<String> =
        context.dataStore.data.map { preferences ->
            preferences[KEY_PUBLIC_FOLDER_NAME] ?: ""
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[KEY_THEME] = mode.name
        }
    }

    suspend fun setLanguage(language: Language) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LANGUAGE] = language.code
        }
    }

    suspend fun setAudioFormat(format: AudioFormat) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FORMAT] = format.name
        }
    }

    suspend fun setAudioQuality(quality: AudioQuality) {
        context.dataStore.edit { preferences ->
            preferences[KEY_QUALITY] = quality.name
        }
    }

    suspend fun setSaveLocation(location: SaveLocation) {
        context.dataStore.edit { preferences ->
            preferences[KEY_SAVE_LOCATION] = location.name
        }
    }

    suspend fun setPublicFolder(
        uri: String,
        name: String,
    ) {
        context.dataStore.edit { preferences ->
            preferences[KEY_PUBLIC_FOLDER_URI] = uri
            preferences[KEY_PUBLIC_FOLDER_NAME] = name
        }
    }
}
