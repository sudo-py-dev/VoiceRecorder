package com.voicerecorder.presentation.ui.recorder

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.voicerecorder.R
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.model.RecordingState
import com.voicerecorder.domain.model.SaveLocation
import com.voicerecorder.domain.repository.AudioRecorderRepository
import com.voicerecorder.domain.repository.PreferencesRepository
import com.voicerecorder.domain.repository.RecordingRepository
import com.voicerecorder.presentation.service.RecordingService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecorderViewModel(
    private val application: Application,
    private val recorderRepository: AudioRecorderRepository,
    private val recordingRepository: RecordingRepository,
    private val preferencesRepository: PreferencesRepository,
) : ViewModel() {
    val recordingState: StateFlow<RecordingState> =
        recorderRepository.recordingState
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordingState.Idle)

    val amplitudeFlow: StateFlow<List<Float>> =
        recorderRepository.amplitudeFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _saveError = MutableStateFlow<String?>(null)
    val saveError: StateFlow<String?> = _saveError.asStateFlow()

    private val _saveSuccess = MutableStateFlow<String?>(null)
    val saveSuccess: StateFlow<String?> = _saveSuccess.asStateFlow()

    private var activeFilePath: String? = null
    private var activeTitle: String? = null

    init {
        // Observe state changes to auto-save metadata when recording finishes
        viewModelScope.launch {
            recorderRepository.recordingState.collectLatest { state ->
                if (state is RecordingState.Idle) {
                    saveCompletedRecording()
                }
            }
        }
    }

    fun startRecording(context: Context) {
        viewModelScope.launch {
            val format = preferencesRepository.audioFormat.first()
            val quality = preferencesRepository.audioQuality.first()

            val timestamp = System.currentTimeMillis()
            val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val dateString = dateFormat.format(Date(timestamp))
            val prefix = context.getString(R.string.default_recording_title_prefix)
            val title = "$prefix$dateString"

            // Output path: record to a temporary file in cacheDir
            val tempFile = File(context.cacheDir, "$title.${format.extension}")

            activeFilePath = tempFile.absolutePath
            activeTitle = title

            val intent =
                Intent(context, RecordingService::class.java).apply {
                    action = RecordingService.ACTION_START
                    putExtra(RecordingService.EXTRA_OUTPUT_PATH, tempFile.absolutePath)
                    putExtra(RecordingService.EXTRA_FORMAT, format.name)
                    putExtra(RecordingService.EXTRA_QUALITY, quality.name)
                }

            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Fallback: start directly on repository if service start fails due to background execution restrictions
                val fallbackResult = recorderRepository.startRecording(tempFile.absolutePath, format, quality)
                fallbackResult.onFailure { fallbackError ->
                    _saveError.value = "${context.getString(R.string.error_record_failed)}: ${fallbackError.localizedMessage}"
                }.onSuccess {
                    _saveError.value = "${context.getString(R.string.error_record_failed)}: ${e.localizedMessage}"
                }
            }
        }
    }

    fun pauseRecording(context: Context) {
        val intent =
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_PAUSE
            }
        context.startService(intent)
    }

    fun resumeRecording(context: Context) {
        val intent =
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_RESUME
            }
        context.startService(intent)
    }

    fun stopRecording(context: Context) {
        val intent =
            Intent(context, RecordingService::class.java).apply {
                action = RecordingService.ACTION_STOP
            }
        context.startService(intent)
    }

    fun clearError() {
        _saveError.value = null
    }

    fun clearSuccess() {
        _saveSuccess.value = null
    }

    private suspend fun saveCompletedRecording() {
        val path = activeFilePath ?: return
        val title = activeTitle ?: return

        activeFilePath = null
        activeTitle = null

        val file = File(path)
        if (file.exists()) {
            if (file.length() > 0) {
                val size = file.length()
                val duration = getFileDurationMs(file)
                val format = preferencesRepository.audioFormat.first()
                val saveLocation = preferencesRepository.saveLocation.first()

                var finalPath = path

                if (saveLocation == SaveLocation.PUBLIC) {
                    val publicUriStr = preferencesRepository.publicFolderUri.first()
                    var uri: android.net.Uri? = null
                    if (publicUriStr.isNotEmpty()) {
                        uri = saveToSafDirectory(application, file, publicUriStr, file.name, format.mimeType)
                    }
                    if (uri == null) {
                        uri = saveToMediaStore(application, file, title, format.mimeType)
                    }
                    if (uri != null) {
                        finalPath = uri.toString()
                    }
                } else {
                    val outputDir = application.getExternalFilesDir(null) ?: application.filesDir
                    val destFile = File(outputDir, file.name)
                    if (file.renameTo(destFile)) {
                        finalPath = destFile.absolutePath
                    } else {
                        finalPath = file.absolutePath
                    }
                }

                val recording =
                    AudioRecording(
                        title = title,
                        filePath = finalPath,
                        durationMs = duration,
                        fileSize = size,
                        timestamp = System.currentTimeMillis(),
                    )
                recordingRepository.insertRecording(recording)
                _saveSuccess.value = application.getString(R.string.recording_completed, title)
            } else {
                file.delete()
            }
        }
    }

    private fun saveToSafDirectory(
        context: Context,
        srcFile: File,
        treeUriStr: String,
        fileName: String,
        mimeType: String,
    ): android.net.Uri? {
        return try {
            val treeUri = android.net.Uri.parse(treeUriStr)
            val documentId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            val parentDocumentUri = android.provider.DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId)

            val newDocumentUri = android.provider.DocumentsContract.createDocument(
                context.contentResolver,
                parentDocumentUri,
                mimeType,
                fileName
            ) ?: return null

            context.contentResolver.openOutputStream(newDocumentUri)?.use { out ->
                srcFile.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            srcFile.delete()
            newDocumentUri
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun saveToMediaStore(
        context: Context,
        file: File,
        title: String,
        mimeType: String,
    ): android.net.Uri? {
        val contentValues =
            android.content.ContentValues().apply {
                put(android.provider.MediaStore.Audio.Media.DISPLAY_NAME, file.name)
                put(android.provider.MediaStore.Audio.Media.TITLE, title)
                put(android.provider.MediaStore.Audio.Media.MIME_TYPE, mimeType)
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(android.provider.MediaStore.Audio.Media.RELATIVE_PATH, "Music/VoiceRecorder")
                    put(android.provider.MediaStore.Audio.Media.IS_PENDING, 1)
                }
            }

        val resolver = context.contentResolver
        val collection = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val uri = resolver.insert(collection, contentValues) ?: return null

        return try {
            resolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { input ->
                    input.copyTo(out)
                }
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(android.provider.MediaStore.Audio.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }

            file.delete()
            uri
        } catch (e: Exception) {
            resolver.delete(uri, null, null)
            null
        }
    }

    private fun getFileDurationMs(file: File): Long {
        // Standard MediaPlayer metadata retriever fallback
        return try {
            val retriever = android.media.MediaMetadataRetriever()
            retriever.setDataSource(file.absolutePath)
            val time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)
            retriever.release()
            time?.toLong() ?: 0L
        } catch (e: Exception) {
            0L
        }
    }

    companion object {
        fun provideFactory(
            application: Application,
            recorderRepository: AudioRecorderRepository,
            recordingRepository: RecordingRepository,
            preferencesRepository: PreferencesRepository,
        ): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return RecorderViewModel(
                        application,
                        recorderRepository,
                        recordingRepository,
                        preferencesRepository,
                    ) as T
                }
            }
        }
    }
}
