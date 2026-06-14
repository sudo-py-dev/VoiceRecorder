package com.voicerecorder.domain.repository

import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.RecordingState
import kotlinx.coroutines.flow.StateFlow

interface AudioRecorderRepository {
    val recordingState: StateFlow<RecordingState>
    val amplitudeFlow: StateFlow<List<Float>> // Emits history of amplitudes for visualizer

    fun startRecording(
        outputPath: String,
        format: AudioFormat = AudioFormat.M4A,
        quality: AudioQuality = AudioQuality.HIGH,
    ): Result<Unit>

    fun pauseRecording(): Result<Unit>

    fun resumeRecording(): Result<Unit>

    fun stopRecording(): Result<Long> // Returns final duration in ms

    fun reset(): Unit
}
