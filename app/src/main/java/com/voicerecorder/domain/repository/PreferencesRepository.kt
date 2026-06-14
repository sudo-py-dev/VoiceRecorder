package com.voicerecorder.domain.repository

import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.Language
import com.voicerecorder.domain.model.SaveLocation
import com.voicerecorder.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface PreferencesRepository {
    val themeMode: Flow<ThemeMode>
    val language: Flow<Language>
    val audioFormat: Flow<AudioFormat>
    val audioQuality: Flow<AudioQuality>
    val saveLocation: Flow<SaveLocation>

    val publicFolderUri: Flow<String>
    val publicFolderName: Flow<String>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setLanguage(language: Language)

    suspend fun setAudioFormat(format: AudioFormat)

    suspend fun setAudioQuality(quality: AudioQuality)

    suspend fun setSaveLocation(location: SaveLocation)

    suspend fun setPublicFolder(
        uri: String,
        name: String,
    )
}
