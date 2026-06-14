package com.voicerecorder.data.repository

import com.voicerecorder.data.local.PreferencesManager
import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.Language
import com.voicerecorder.domain.model.SaveLocation
import com.voicerecorder.domain.model.ThemeMode
import com.voicerecorder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow

class PreferencesRepositoryImpl(
    private val preferencesManager: PreferencesManager,
) : PreferencesRepository {
    override val themeMode: Flow<ThemeMode> = preferencesManager.themeModeFlow
    override val language: Flow<Language> = preferencesManager.languageFlow
    override val audioFormat: Flow<AudioFormat> = preferencesManager.audioFormatFlow
    override val audioQuality: Flow<AudioQuality> = preferencesManager.audioQualityFlow
    override val saveLocation: Flow<SaveLocation> = preferencesManager.saveLocationFlow
    override val publicFolderUri: Flow<String> = preferencesManager.publicFolderUriFlow
    override val publicFolderName: Flow<String> = preferencesManager.publicFolderNameFlow

    override suspend fun setThemeMode(mode: ThemeMode) {
        preferencesManager.setThemeMode(mode)
    }

    override suspend fun setLanguage(language: Language) {
        preferencesManager.setLanguage(language)
    }

    override suspend fun setAudioFormat(format: AudioFormat) {
        preferencesManager.setAudioFormat(format)
    }

    override suspend fun setAudioQuality(quality: AudioQuality) {
        preferencesManager.setAudioQuality(quality)
    }

    override suspend fun setSaveLocation(location: SaveLocation) {
        preferencesManager.setSaveLocation(location)
    }

    override suspend fun setPublicFolder(uri: String, name: String) {
        preferencesManager.setPublicFolder(uri, name)
    }
}
