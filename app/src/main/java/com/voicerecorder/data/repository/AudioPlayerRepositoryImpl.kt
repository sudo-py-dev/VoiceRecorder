package com.voicerecorder.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import com.voicerecorder.R
import com.voicerecorder.domain.model.PlayerState
import com.voicerecorder.domain.repository.AudioPlayerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AudioPlayerRepositoryImpl(
    private val context: Context,
) : AudioPlayerRepository {
    private val _playerState = MutableStateFlow<PlayerState>(PlayerState.Idle)
    override val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Default)
    private var currentRecordingId: Long? = null

    @Synchronized
    override fun play(
        recordingId: Long,
        filePath: String,
    ): Result<Unit> {
        var player: MediaPlayer? = null
        return runCatching {
            // Stop current playback if active
            stopPlaybackInternal()

            val newPlayer = MediaPlayer()
            player = newPlayer

            newPlayer.apply {
                if (filePath.startsWith("content://")) {
                    setDataSource(context, Uri.parse(filePath))
                } else {
                    setDataSource(filePath)
                }
                prepare()
                start()
            }

            currentRecordingId = recordingId
            mediaPlayer = newPlayer

            val duration = newPlayer.duration.toLong()
            _playerState.value = PlayerState.Playing(0L, duration, recordingId)

            newPlayer.setOnCompletionListener {
                stopPlaybackInternal()
            }

            newPlayer.setOnErrorListener { _, what, extra ->
                stopPlaybackInternal(
                    PlayerState.Error(
                        errorResId = R.string.error_play_failed,
                        details = "what=$what extra=$extra",
                    ),
                )
                true
            }

            startTicker(recordingId, duration)
        }.onFailure { e ->
            player?.release()
            player = null
            _playerState.value =
                PlayerState.Error(
                    errorResId = R.string.error_play_failed,
                    details = e.localizedMessage,
                )
        }
    }

    @Synchronized
    override fun pause(): Result<Unit> {
        val state = _playerState.value
        if (state !is PlayerState.Playing) {
            return Result.failure(IllegalStateException("Not playing"))
        }

        return runCatching {
            mediaPlayer?.pause()
            stopTicker()
            _playerState.value =
                PlayerState.Paused(
                    progressMs = state.progressMs,
                    durationMs = state.durationMs,
                    recordingId = state.recordingId,
                )
        }
    }

    @Synchronized
    override fun resume(): Result<Unit> {
        val state = _playerState.value
        if (state !is PlayerState.Paused) {
            return Result.failure(IllegalStateException("Not paused"))
        }

        return runCatching {
            mediaPlayer?.start()
            _playerState.value =
                PlayerState.Playing(
                    progressMs = state.progressMs,
                    durationMs = state.durationMs,
                    recordingId = state.recordingId,
                )
            startTicker(state.recordingId, state.durationMs)
        }
    }

    @Synchronized
    override fun stop(): Result<Unit> {
        return runCatching {
            stopPlaybackInternal()
        }
    }

    @Synchronized
    override fun seekTo(positionMs: Long): Result<Unit> {
        val state = _playerState.value
        if (state is PlayerState.Idle || state is PlayerState.Error) {
            return Result.failure(IllegalStateException("No audio loaded"))
        }

        return runCatching {
            mediaPlayer?.seekTo(positionMs.toInt())
            val duration =
                when (state) {
                    is PlayerState.Playing -> state.durationMs
                    is PlayerState.Paused -> state.durationMs
                    else -> 0L
                }
            val id =
                when (state) {
                    is PlayerState.Playing -> state.recordingId
                    is PlayerState.Paused -> state.recordingId
                    else -> 0L
                }

            if (state is PlayerState.Playing) {
                _playerState.value = PlayerState.Playing(positionMs, duration, id)
            } else {
                _playerState.value = PlayerState.Paused(positionMs, duration, id)
            }
        }
    }

    override fun release() {
        stopPlaybackInternal()
    }

    private fun stopPlaybackInternal(newState: PlayerState = PlayerState.Idle) {
        stopTicker()
        runCatching {
            mediaPlayer?.apply {
                try {
                    if (isPlaying) {
                        stop()
                    }
                } catch (e: Exception) {
                    // Ignore state errors when stopping
                }
                release()
            }
        }
        mediaPlayer = null
        currentRecordingId = null
        _playerState.value = newState
    }

    private fun startTicker(
        recordingId: Long,
        durationMs: Long,
    ) {
        progressJob?.cancel()
        progressJob =
            scope.launch {
                while (true) {
                    delay(200)
                    val player = mediaPlayer
                    if (player == null) {
                        break
                    }
                    val currentPosition =
                        try {
                            player.currentPosition.toLong()
                        } catch (e: Exception) {
                            break
                        }
                    _playerState.value =
                        PlayerState.Playing(
                            progressMs = currentPosition.coerceAtMost(durationMs),
                            durationMs = durationMs,
                            recordingId = recordingId,
                        )
                }
            }
    }

    private fun stopTicker() {
        progressJob?.cancel()
        progressJob = null
    }
}
