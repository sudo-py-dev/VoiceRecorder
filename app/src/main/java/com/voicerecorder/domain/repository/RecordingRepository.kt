package com.voicerecorder.domain.repository

import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.model.SaveLocation
import kotlinx.coroutines.flow.Flow
import java.io.File

interface RecordingRepository {
    fun getAllRecordings(): Flow<List<AudioRecording>>

    fun getRecordingById(id: Long): Flow<AudioRecording?>

    suspend fun insertRecording(recording: AudioRecording): Long

    suspend fun saveRecording(
        tempFile: File,
        title: String,
        format: AudioFormat,
        durationMs: Long,
        saveLocation: SaveLocation,
        publicFolderUri: String,
    ): Result<AudioRecording>

    suspend fun renameRecording(
        id: Long,
        newTitle: String,
    ): Result<Unit>

    suspend fun deleteRecording(id: Long): Result<Unit>
}
