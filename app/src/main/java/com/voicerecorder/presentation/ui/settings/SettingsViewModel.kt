package com.voicerecorder.presentation.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.Language
import com.voicerecorder.domain.model.SaveLocation
import com.voicerecorder.domain.model.ThemeMode
import com.voicerecorder.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val themeMode: StateFlow<ThemeMode> =
        preferencesRepository.themeMode
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ThemeMode.SYSTEM)

    val language: StateFlow<Language> =
        preferencesRepository.language
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), Language.ENGLISH)

    val audioFormat: StateFlow<AudioFormat> =
        preferencesRepository.audioFormat
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioFormat.M4A)

    val audioQuality: StateFlow<AudioQuality> =
        preferencesRepository.audioQuality
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AudioQuality.HIGH)

    val saveLocation: StateFlow<SaveLocation> =
        preferencesRepository.saveLocation
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SaveLocation.PRIVATE)

    val publicFolderUri: StateFlow<String> =
        preferencesRepository.publicFolderUri
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val publicFolderName: StateFlow<String> =
        preferencesRepository.publicFolderName
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesRepository.setThemeMode(mode)
        }
    }

    fun setLanguage(lang: Language) {
        viewModelScope.launch {
            preferencesRepository.setLanguage(lang)
        }
    }

    fun setAudioFormat(format: AudioFormat) {
        viewModelScope.launch {
            preferencesRepository.setAudioFormat(format)
        }
    }

    fun setAudioQuality(quality: AudioQuality) {
        viewModelScope.launch {
            preferencesRepository.setAudioQuality(quality)
        }
    }

    fun setSaveLocation(location: SaveLocation) {
        viewModelScope.launch {
            preferencesRepository.setSaveLocation(location)
        }
    }

    fun setPublicFolder(
        uri: String,
        name: String,
    ) {
        viewModelScope.launch {
            preferencesRepository.setPublicFolder(uri, name)
        }
    }

    companion object {
        fun provideFactory(preferencesRepository: PreferencesRepository): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(preferencesRepository) as T
                }
            }
        }
    }
}
