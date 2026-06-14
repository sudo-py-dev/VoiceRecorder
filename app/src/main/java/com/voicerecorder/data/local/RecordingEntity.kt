package com.voicerecorder.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.voicerecorder.domain.model.AudioRecording

@Entity(tableName = "recordings")
data class RecordingEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val filePath: String,
    val durationMs: Long,
    val fileSize: Long,
    val timestamp: Long,
)

fun RecordingEntity.toDomain(): AudioRecording {
    return AudioRecording(
        id = id,
        title = title,
        filePath = filePath,
        durationMs = durationMs,
        fileSize = fileSize,
        timestamp = timestamp,
    )
}

fun AudioRecording.toEntity(): RecordingEntity {
    return RecordingEntity(
        id = id,
        title = title,
        filePath = filePath,
        durationMs = durationMs,
        fileSize = fileSize,
        timestamp = timestamp,
    )
}
