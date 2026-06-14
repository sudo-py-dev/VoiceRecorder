package com.voicerecorder.domain.model

data class AudioRecording(
    val id: Long = 0L,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long,
    val timestamp: Long,
)
