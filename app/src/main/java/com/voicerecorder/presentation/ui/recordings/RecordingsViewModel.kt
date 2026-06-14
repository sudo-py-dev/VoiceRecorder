package com.voicerecorder.presentation.ui.recordings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.model.PlayerState
import com.voicerecorder.domain.repository.AudioPlayerRepository
import com.voicerecorder.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RecordingsViewModel(
    private val recordingRepository: RecordingRepository,
    private val playerRepository: AudioPlayerRepository,
) : ViewModel() {
    val playerState: StateFlow<PlayerState> =
        playerRepository.playerState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PlayerState.Idle)

    private val _actionError = MutableStateFlow<Int?>(null)
    val actionError: StateFlow<Int?> = _actionError.asStateFlow()

    fun clearActionError() {
        _actionError.value = null
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    val recordings: StateFlow<List<AudioRecording>> =
        combine(
            recordingRepository.getAllRecordings(),
            _searchQuery,
        ) { list, query ->
            if (query.isBlank()) {
                list
            } else {
                list.filter { it.title.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun playRecording(recording: AudioRecording) {
        viewModelScope.launch {
            playerRepository.play(recording.id, recording.filePath)
        }
    }

    fun pausePlayback() {
        playerRepository.pause()
    }

    fun resumePlayback() {
        playerRepository.resume()
    }

    fun stopPlayback() {
        playerRepository.stop()
    }

    fun seekTo(positionMs: Long) {
        playerRepository.seekTo(positionMs)
    }

    fun deleteRecording(id: Long) {
        viewModelScope.launch {
            val state = playerState.value
            val isActivePlaying =
                when (state) {
                    is PlayerState.Playing -> state.recordingId == id
                    is PlayerState.Paused -> state.recordingId == id
                    else -> false
                }
            if (isActivePlaying) {
                playerRepository.stop()
            }
            val result = recordingRepository.deleteRecording(id)
            result.onFailure {
                _actionError.value = com.voicerecorder.R.string.error_delete_failed
            }
        }
    }

    fun renameRecording(
        id: Long,
        newTitle: String,
    ) {
        if (newTitle.isBlank()) return
        viewModelScope.launch {
            val state = playerState.value
            val isActivePlaying =
                when (state) {
                    is PlayerState.Playing -> state.recordingId == id
                    is PlayerState.Paused -> state.recordingId == id
                    else -> false
                }
            if (isActivePlaying) {
                // Stop before rename to release file lock
                playerRepository.stop()
            }
            val result = recordingRepository.renameRecording(id, newTitle)
            result.onFailure {
                _actionError.value = com.voicerecorder.R.string.error_rename_failed
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        playerRepository.release()
    }

    companion object {
        fun provideFactory(
            recordingRepository: RecordingRepository,
            playerRepository: AudioPlayerRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecordingsViewModel(recordingRepository, playerRepository) as T
                }
            }
        }
    }
}
