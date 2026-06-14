package com.voicerecorder.domain.repository

import com.voicerecorder.domain.model.PlayerState
import kotlinx.coroutines.flow.StateFlow

interface AudioPlayerRepository {
    val playerState: StateFlow<PlayerState>

    fun play(
        recordingId: Long,
        filePath: String,
    ): Result<Unit>

    fun pause(): Result<Unit>

    fun resume(): Result<Unit>

    fun stop(): Result<Unit>

    fun seekTo(positionMs: Long): Result<Unit>

    fun release(): Unit
}
