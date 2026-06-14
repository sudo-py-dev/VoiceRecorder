package com.voicerecorder.domain.repository

import com.voicerecorder.domain.model.AudioRecording
import kotlinx.coroutines.flow.Flow

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<AudioRecording>>

    fun getRecordingById(id: Long): Flow<AudioRecording?>

    suspend fun insertRecording(recording: AudioRecording): Long

    suspend fun renameRecording(
        id: Long,
        newTitle: String,
    ): Result<Unit>

    suspend fun deleteRecording(id: Long): Result<Unit>
}
