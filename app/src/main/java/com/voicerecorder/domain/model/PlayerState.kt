package com.voicerecorder.domain.model

sealed interface PlayerState {
    object Idle : PlayerState

    data class Playing(
        val progressMs: Long,
        val durationMs: Long,
        val recordingId: Long,
    ) : PlayerState

    data class Paused(
        val progressMs: Long,
        val durationMs: Long,
        val recordingId: Long,
    ) : PlayerState

    data class Error(
        val errorResId: Int,
        val details: String? = null,
    ) : PlayerState
}
