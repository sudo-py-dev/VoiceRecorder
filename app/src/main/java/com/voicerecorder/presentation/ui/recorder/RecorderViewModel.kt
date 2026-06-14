package com.voicerecorder.presentation.ui.recorder

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicerecorder.R
import com.voicerecorder.domain.model.RecordingState
import com.voicerecorder.domain.repository.AudioRecorderRepository
import com.voicerecorder.domain.repository.PreferencesRepository
import com.voicerecorder.domain.repository.RecordingRepository
import com.voicerecorder.presentation.service.RecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecorderViewModel(
    private val application: Application,
    private val recorderRepository: AudioRecorderRepository,
    private val recordingRepository: RecordingRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val recordingState: StateFlow<RecordingState> =
        recorderRepository.recordingState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordingState.Idle)

    val amplitudeFlow: StateFlow<List<Float>> =
        recorderRepository.amplitudeFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _saveSuccess = MutableStateFlow<String?>(null)
    val saveSuccess: StateFlow<String?> = _saveSuccess.asStateFlow()

    private var activeFilePath: String? = null
    private var activeTitle: String? = null

    init {
        // Observe state changes to auto-save metadata when recording finishes
        viewModelScope.launch {
            recorderRepository.recordingState.collectLatest { state ->
                if (state is RecordingState.Idle) {
                    saveCompletedRecording()
                }
            }
        }
    }

    fun startRecording(context: Context) {
        viewModelScope.launch {
            val format = preferencesRepository.audioFormat.first()
            val quality = preferencesRepository.audioQuality.first()

            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateString = dateFormat.format(Date(timestamp))
            val prefix = context.getString(R.string.default_recording_title_prefix)
            val title = "$prefix$dateString"

            // Output path: record to a temporary file in cacheDir
            val tempFile = File(context.cacheDir, "$title.${format.extension}")

            activeFilePath = tempFile.absolutePath
            activeTitle = title

            val intent =
                Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_OUTPUT_PATH, tempFile.absolutePath)
                    putExtra(RecordingService.EXTRA_FORMAT, format.name)
                    putExtra(RecordingService.EXTRA_QUALITY, quality.name)
                }

            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Fallback: start directly on repository if service start fails due to background execution restrictions
                val fallbackResult = recorderRepository.startRecording(tempFile.absolutePath, format, quality)
                fallbackResult.onFailure { fallbackError ->
                    _saveError.value = "${context.getString(R.string.error_record_failed)}: ${fallbackError.localizedMessage}"
                }.onSuccess {
                    _saveError.value = "${context.getString(R.string.error_record_failed)}: ${e.localizedMessage}"
                }
            }
        }
    }

    fun pauseRecording(context: Context) {
        val intent =
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_PAUSE
            }
        context.startService(intent)
    }

    fun resumeRecording(context: Context) {
        val intent =
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_RESUME
            }
        context.startService(intent)
    }

    fun stopRecording(context: Context) {
        val intent =
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
        context.startService(intent)
    }

    fun clearError() {
        _saveError.value = null
    }

    fun clearSuccess() {
        _saveSuccess.value = null
    }

    private suspend fun saveCompletedRecording() {
        val path = activeFilePath ?: return
        val title = activeTitle ?: return

        activeFilePath = null
        activeTitle = null

        val file = File(path)
        if (file.exists() && file.length() > 0) {
            val format = preferencesRepository.audioFormat.first()
            val saveLocation = preferencesRepository.saveLocation.first()
            val publicFolderUri = preferencesRepository.publicFolderUri.first()

            val duration = getFileDurationMs(file)

            if (duration < 1000L) {
                file.delete()
                _saveError.value = application.getString(R.string.error_recording_too_short)
                return
            }

            recordingRepository.saveRecording(
                tempFile = file,
                title = title,
                format = format,
                durationMs = duration,
                saveLocation = saveLocation,
                publicFolderUri = publicFolderUri,
            ).onSuccess {
                _saveSuccess.value = application.getString(R.string.recording_completed, title)
            }.onFailure {
                _saveError.value = it.message ?: application.getString(R.string.error_save_failed)
            }
        } else {
            file.delete()
        }
    }

    private fun getFileDurationMs(file: File): Long {
        // Standard MediaPlayer metadata retriever fallback
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            recorderRepository: AudioRecorderRepository,
            recordingRepository: RecordingRepository,
            preferencesRepository: PreferencesRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecorderViewModel(
                        application,
                        recorderRepository,
                        recordingRepository,
                        preferencesRepository,
                    ) as T
                }
            }
        }
    }
}
