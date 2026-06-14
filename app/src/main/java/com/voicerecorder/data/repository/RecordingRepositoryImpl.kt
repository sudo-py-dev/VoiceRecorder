package com.voicerecorder.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.voicerecorder.data.local.RecordingDao
import com.voicerecorder.data.local.toDomain
import com.voicerecorder.data.local.toEntity
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.repository.RecordingRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.io.File

class RecordingRepositoryImpl(
    private val context: Context,
    private val dao: RecordingDao,
) : RecordingRepository {
    override fun getAllRecordings(): Flow<List<AudioRecording>> {
        return dao.getAllRecordings().map { list ->
            list.map { it.toDomain() }
        }
    }

    override fun getRecordingById(id: Long): Flow<AudioRecording?> {
        return dao.getRecordingById(id).map { it?.toDomain() }
    }

    override suspend fun insertRecording(recording: AudioRecording): Long {
        return dao.insertRecording(recording.toEntity())
    }

    override suspend fun renameRecording(
        id: Long,
        newTitle: String,
    ): Result<Unit> {
        return runCatching {
            val entity = dao.getRecordingByIdSuspend(id) ?: throw NoSuchElementException("Recording not found")
            val filePath = entity.filePath

            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                var updatedPath = filePath
                var renamed = false

                // Try renaming via DocumentsContract (for SAF directory files)
                try {
                    val renamedUri = DocumentsContract.renameDocument(context.contentResolver, uri, newTitle)
                    if (renamedUri != null) {
                        updatedPath = renamedUri.toString()
                        renamed = true
                    }
                } catch (e: Exception) {
                    // Ignore and try MediaStore fallback
                }

                // If not SAF or SAF rename failed, try MediaStore rename
                if (!renamed) {
                    try {
                        val values = android.content.ContentValues().apply {
                            put(MediaStore.Audio.Media.TITLE, newTitle)
                            
                            // Retrieve the extension to construct new display name
                            val oldDisplayName = context.contentResolver.query(
                                uri, 
                                arrayOf(MediaStore.Audio.Media.DISPLAY_NAME), 
                                null, null, null
                            )?.use { cursor ->
                                if (cursor.moveToFirst()) cursor.getString(0) else null
                            }
                            val ext = oldDisplayName?.substringAfterLast('.', "") ?: "m4a"
                            val newDisplayName = if (ext.isNotEmpty()) "$newTitle.$ext" else newTitle
                            put(MediaStore.Audio.Media.DISPLAY_NAME, newDisplayName)
                        }
                        context.contentResolver.update(uri, values, null, null)
                    } catch (e: Exception) {
                        // Ignore, fallback to title rename only
                    }
                }

                val updatedEntity = entity.copy(title = newTitle, filePath = updatedPath)
                dao.insertRecording(updatedEntity)
            } else {
                val oldFile = File(filePath)
                if (oldFile.exists()) {
                    val directory = oldFile.parentFile
                    val extension = oldFile.extension
                    val newFile = File(directory, "$newTitle.$extension")

                    if (oldFile.renameTo(newFile)) {
                        val updatedEntity =
                            entity.copy(
                                title = newTitle,
                                filePath = newFile.absolutePath,
                            )
                        dao.insertRecording(updatedEntity)
                    } else {
                        val updatedEntity = entity.copy(title = newTitle)
                        dao.insertRecording(updatedEntity)
                    }
                } else {
                    val updatedEntity = entity.copy(title = newTitle)
                    dao.insertRecording(updatedEntity)
                }
            }
        }
    }

    override suspend fun deleteRecording(id: Long): Result<Unit> {
        return runCatching {
            val entity = dao.getRecordingByIdSuspend(id)
            if (entity != null) {
                val filePath = entity.filePath
                if (filePath.startsWith("content://")) {
                    try {
                        context.contentResolver.delete(Uri.parse(filePath), null, null)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    val file = File(filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }
                dao.deleteRecording(id)
            }
        }
    }
}
