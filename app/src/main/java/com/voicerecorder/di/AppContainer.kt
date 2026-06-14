package com.voicerecorder.di

import android.content.Context
import com.voicerecorder.data.local.PreferencesManager
import com.voicerecorder.data.local.RecordingDatabase
import com.voicerecorder.data.repository.AudioPlayerRepositoryImpl
import com.voicerecorder.data.repository.AudioRecorderRepositoryImpl
import com.voicerecorder.data.repository.PreferencesRepositoryImpl
import com.voicerecorder.data.repository.RecordingRepositoryImpl
import com.voicerecorder.domain.repository.AudioPlayerRepository
import com.voicerecorder.domain.repository.AudioRecorderRepository
import com.voicerecorder.domain.repository.PreferencesRepository
import com.voicerecorder.domain.repository.RecordingRepository

interface AppContainer {
    val audioRecorderRepository: AudioRecorderRepository
    val audioPlayerRepository: AudioPlayerRepository
    val recordingRepository: RecordingRepository
    val preferencesRepository: PreferencesRepository
}

class AppContainerImpl(private val context: Context) : AppContainer {
    private val database: RecordingDatabase by lazy {
        RecordingDatabase.build(context)
    }

    private val preferencesManager: PreferencesManager by lazy {
        PreferencesManager(context)
    }

    override val audioRecorderRepository: AudioRecorderRepository by lazy {
        AudioRecorderRepositoryImpl(context)
    }

    override val audioPlayerRepository: AudioPlayerRepository by lazy {
        AudioPlayerRepositoryImpl(context)
    }

    override val recordingRepository: RecordingRepository by lazy {
        RecordingRepositoryImpl(context, database.dao)
    }

    override val preferencesRepository: PreferencesRepository by lazy {
        PreferencesRepositoryImpl(preferencesManager)
    }
}
