package com.voicerecorder.data.repository

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.MediaStore
import com.voicerecorder.data.local.RecordingDao
import com.voicerecorder.data.local.toDomain
import com.voicerecorder.data.local.toEntity
import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.model.SaveLocation
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

    override suspend fun saveRecording(
        tempFile: File,
        title: String,
        format: AudioFormat,
        durationMs: Long,
        saveLocation: SaveLocation,
        publicFolderUri: String,
    ): Result<AudioRecording> {
        return runCatching {
            var finalPath = tempFile.absolutePath
            val size = tempFile.length()

            if (saveLocation == SaveLocation.PUBLIC) {
                var uri: Uri? = null
                if (publicFolderUri.isNotEmpty()) {
                    uri = saveToSafDirectory(tempFile, publicFolderUri, tempFile.name, format.mimeType)
                }
                if (uri == null) {
                    uri = saveToMediaStore(tempFile, title, format.mimeType)
                }
                if (uri != null) {
                    finalPath = uri.toString()
                }
            } else {
                val outputDir = context.getExternalFilesDir(null) ?: context.filesDir
                val destFile = File(outputDir, tempFile.name)
                if (tempFile.renameTo(destFile)) {
                    finalPath = destFile.absolutePath
                }
            }

            val recording =
                AudioRecording(
                    title = title,
                    filePath = finalPath,
                    durationMs = durationMs,
                    fileSize = size,
                    timestamp = System.currentTimeMillis(),
                )
            val id = insertRecording(recording)
            recording.copy(id = id)
        }
    }

    private fun saveToSafDirectory(
        srcFile: File,
        treeUriStr: String,
        fileName: String,
        mimeType: String,
    ): Uri? {
        return try {
            val treeUri = Uri.parse(treeUriStr)
            val documentId = DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

            val newDocumentUri =
                DocumentsContract.createDocument(
                    context.contentResolver,
                    parentDocumentUri,
                    mimeType,
                    fileName,
                ) ?: return null

            context.contentResolver.openOutputStream(newDocumentUri)?.use { out ->
                srcFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            srcFile.delete()
            newDocumentUri
        } catch (e: Exception) {
            null
        }
    }

    private fun saveToMediaStore(
        file: File,
        title: String,
        mimeType: String,
    ): Uri? {
        val contentValues =
            android.content.ContentValues().apply {
                put(MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                put(MediaStore.Audio.Media.TITLE, title)
                put(MediaStore.Audio.Media.MIME_TYPE, mimeType)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(MediaStore.Audio.Media.RELATIVE_PATH, "Music/VoiceRecorder")
                    put(MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

        val resolver = context.contentResolver
        val collection = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, contentValues) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            file.delete()
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    override suspend fun renameRecording(
        id: Long,
        newTitle: String,
    ): Result<Unit> {
        return runCatching {
            val sanitizedTitle = newTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_").take(200)
            if (sanitizedTitle.isBlank()) throw IllegalArgumentException("Title cannot be empty after sanitization")

            val entity = dao.getRecordingByIdSuspend(id) ?: throw NoSuchElementException("Recording not found")
            val filePath = entity.filePath

            if (filePath.startsWith("content://")) {
                val uri = Uri.parse(filePath)
                var updatedPath = filePath
                var renamed = false

                // Try renaming via DocumentsContract (for SAF directory files)
                try {
                    val renamedUri = DocumentsContract.renameDocument(context.contentResolver, uri, sanitizedTitle)
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
                        val values =
                            android.content.ContentValues().apply {
                                put(MediaStore.Audio.Media.TITLE, sanitizedTitle)

                                // Retrieve the extension to construct new display name
                                val oldDisplayName =
                                    context.contentResolver.query(
                                        uri,
                                        arrayOf(MediaStore.Audio.Media.DISPLAY_NAME),
                                        null,
                                        null,
                                        null,
                                    )?.use { cursor ->
                                        if (cursor.moveToFirst()) cursor.getString(0) else null
                                    }
                                val ext = oldDisplayName?.substringAfterLast('.', "") ?: "m4a"
                                val newDisplayName = if (ext.isNotEmpty()) "$sanitizedTitle.$ext" else sanitizedTitle
                                put(MediaStore.Audio.Media.DISPLAY_NAME, newDisplayName)
                            }
                        context.contentResolver.update(uri, values, null, null)
                    } catch (e: Exception) {
                        // Ignore, fallback to title rename only
                    }
                }

                val updatedEntity = entity.copy(title = sanitizedTitle, filePath = updatedPath)
                dao.insertRecording(updatedEntity)
            } else {
                val oldFile = File(filePath)
                if (oldFile.exists()) {
                    val directory = oldFile.parentFile
                    val extension = oldFile.extension
                    val newFile = File(directory, "$sanitizedTitle.$extension")

                    if (oldFile.renameTo(newFile)) {
                        val updatedEntity =
                            entity.copy(
                                title = sanitizedTitle,
                                filePath = newFile.absolutePath,
                            )
                        dao.insertRecording(updatedEntity)
                    } else {
                        val updatedEntity = entity.copy(title = sanitizedTitle)
                        dao.insertRecording(updatedEntity)
                    }
                } else {
                    val updatedEntity = entity.copy(title = sanitizedTitle)
                    dao.insertRecording(updatedEntity)
                }
            }
        }
    }

    override suspend fun deleteRecording(id: Long): Result<Unit> {
        return runCatching {
            val entity = dao.getRecordingByIdSuspend(id) ?: return Result.success(Unit)
            val filePath = entity.filePath
            if (filePath.startsWith("content://")) {
                try {
                    context.contentResolver.delete(Uri.parse(filePath), null, null)
                } catch (e: Exception) {
                    // It might be already deleted or permission revoked, proceed with DB deletion
                }
            } else {
                val file = File(filePath)
                if (file.exists()) {
                    if (!file.delete()) {
                        // Log failure or handle accordingly if needed, but we proceed with DB deletion
                    }
                }
            }
            dao.deleteRecording(id)
        }
    }
}
