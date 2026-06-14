package com.voicerecorder.domain.model

sealed interface RecordingState {
    object Idle : RecordingState

    data class Recording(
        val durationMs: Long,
        val amplitude: Float,
    ) : RecordingState

    data class Paused(
        val durationMs: Long,
    ) : RecordingState
}
