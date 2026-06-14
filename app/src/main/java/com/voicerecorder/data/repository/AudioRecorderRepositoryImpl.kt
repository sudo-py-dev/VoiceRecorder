package com.voicerecorder.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Build
import com.voicerecorder.data.util.WavHeaderWriter
import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.RecordingState
import com.voicerecorder.domain.repository.AudioRecorderRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import android.media.AudioFormat as AndroidAudioFormat

class AudioRecorderRepositoryImpl(
    private val context: Context,
) : AudioRecorderRepository {
    private val _recordingState = MutableStateFlow<RecordingState>(RecordingState.Idle)
    override val recordingState: StateFlow<RecordingState> = _recordingState.asStateFlow()

    private val _amplitudeFlow = MutableStateFlow<List<Float>>(emptyList())
    override val amplitudeFlow: StateFlow<List<Float>> = _amplitudeFlow.asStateFlow()

    private var mediaRecorder: MediaRecorder? = null
    private var audioRecord: AudioRecord? = null
    private var recordingJob: Job? = null
    private var pcmWritingJob: Job? = null

    private var durationMs = 0L
    private var pcmDataLength = 0L
    private var currentMaxAmplitude = 0
    private val amplitudeHistory = mutableListOf<Float>()
    private val scope = CoroutineScope(Dispatchers.Default)

    @SuppressLint("MissingPermission")
    @Synchronized
    override fun startRecording(
        outputPath: String,
        format: AudioFormat,
        quality: AudioQuality,
    ): Result<Unit> {
        if (_recordingState.value !is RecordingState.Idle) {
            return Result.failure(IllegalStateException("Already recording or paused"))
        }

        // Delete file if already exists
        val file = File(outputPath)
        if (file.exists()) {
            file.delete()
        }

        currentMaxAmplitude = 0
        return if (format == AudioFormat.WAV) {
            startWavRecording(outputPath, quality)
        } else {
            startMediaRecorderRecording(outputPath, format, quality)
        }
    }

    private fun startMediaRecorderRecording(
        outputPath: String,
        format: AudioFormat,
        quality: AudioQuality,
    ): Result<Unit> {
        var recorder: MediaRecorder? = null
        return runCatching {
            val newRecorder =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
            recorder = newRecorder

            val outputFormat =
                when (format) {
                    AudioFormat.M4A -> MediaRecorder.OutputFormat.MPEG_4
                    AudioFormat.AMR -> MediaRecorder.OutputFormat.THREE_GPP
                    AudioFormat.AAC -> MediaRecorder.OutputFormat.AAC_ADTS
                    AudioFormat.OGG ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaRecorder.OutputFormat.OGG
                        } else {
                            MediaRecorder.OutputFormat.MPEG_4
                        }
                    else -> MediaRecorder.OutputFormat.MPEG_4
                }

            val audioEncoder =
                when (format) {
                    AudioFormat.M4A -> MediaRecorder.AudioEncoder.AAC
                    AudioFormat.AMR -> MediaRecorder.AudioEncoder.AMR_NB
                    AudioFormat.AAC -> MediaRecorder.AudioEncoder.AAC
                    AudioFormat.OGG ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            MediaRecorder.AudioEncoder.OPUS
                        } else {
                            MediaRecorder.AudioEncoder.AAC
                        }
                    else -> MediaRecorder.AudioEncoder.AAC
                }

            val samplingRate =
                when (format) {
                    AudioFormat.AMR -> 8000
                    else ->
                        when (quality) {
                            AudioQuality.HIGH -> 48000
                            AudioQuality.MEDIUM -> 44100
                            AudioQuality.LOW -> 22050
                        }
                }

            val bitRate =
                when (format) {
                    AudioFormat.AMR -> 12200
                    else ->
                        when (quality) {
                            AudioQuality.HIGH -> 256000
                            AudioQuality.MEDIUM -> 128000
                            AudioQuality.LOW -> 64000
                        }
                }

            newRecorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(outputFormat)
                setAudioEncoder(audioEncoder)
                setAudioEncodingBitRate(bitRate)
                setAudioSamplingRate(samplingRate)
                setOutputFile(outputPath)
                prepare()
                start()
            }

            mediaRecorder = newRecorder
            durationMs = 0L
            amplitudeHistory.clear()
            _amplitudeFlow.value = emptyList()
            _recordingState.value = RecordingState.Recording(0L, 0f)

            startTicker()
        }.onFailure {
            recorder?.release()
            recorder = null
            runCatching { File(outputPath).delete() }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startWavRecording(
        outputPath: String,
        quality: AudioQuality,
    ): Result<Unit> {
        return runCatching {
            val sampleRate =
                when (quality) {
                    AudioQuality.HIGH -> 48000
                    AudioQuality.MEDIUM -> 44100
                    AudioQuality.LOW -> 22050
                }
            val minBufferSize =
                AudioRecord.getMinBufferSize(
                    sampleRate,
                    AndroidAudioFormat.CHANNEL_IN_MONO,
                    AndroidAudioFormat.ENCODING_PCM_16BIT,
                )

            val recorder =
                AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AndroidAudioFormat.CHANNEL_IN_MONO,
                    AndroidAudioFormat.ENCODING_PCM_16BIT,
                    minBufferSize * 2,
                )

            if (recorder.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("AudioRecord initialization failed")
            }

            recorder.startRecording()
            audioRecord = recorder
            pcmDataLength = 0L
            durationMs = 0L
            amplitudeHistory.clear()
            _amplitudeFlow.value = emptyList()
            _recordingState.value = RecordingState.Recording(0L, 0f)

            // Start writing PCM data to file
            startPcmWriting(outputPath, sampleRate, minBufferSize)
            startTicker()
        }.onFailure {
            audioRecord?.release()
            audioRecord = null
            runCatching { File(outputPath).delete() }
        }
    }

    private fun startPcmWriting(
        outputPath: String,
        sampleRate: Int,
        bufferSize: Int,
    ) {
        pcmWritingJob?.cancel()
        pcmWritingJob =
            scope.launch(Dispatchers.IO) {
                val file = File(outputPath)
                FileOutputStream(file).use { outputStream ->
                    // Write dummy header first
                    outputStream.write(ByteArray(44))

                    val buffer = ShortArray(bufferSize)
                    while (isActive) {
                        val state = _recordingState.value
                        if (state is RecordingState.Recording) {
                            val read = audioRecord?.read(buffer, 0, buffer.size) ?: 0
                            if (read > 0) {
                                val byteBuffer = ByteArray(read * 2)
                                var maxAmp = 0
                                for (i in 0 until read) {
                                    val s = buffer[i]
                                    val absS = if (s < 0) -s.toInt() else s.toInt()
                                    if (absS > maxAmp) maxAmp = absS
                                    byteBuffer[i * 2] = (s.toInt() and 0x00FF).toByte()
                                    byteBuffer[i * 2 + 1] = ((s.toInt() and 0xFF00) shr 8).toByte()
                                }
                                currentMaxAmplitude = maxAmp
                                outputStream.write(byteBuffer)
                                pcmDataLength += byteBuffer.size
                            }
                        } else if (state is RecordingState.Paused) {
                            currentMaxAmplitude = 0
                            delay(100)
                        } else {
                            break
                        }
                    }
                }

                // After recording stops, write the real WAV header
                RandomAccessFile(file, "rw").use { raf ->
                    WavHeaderWriter.writeHeader(
                        raf,
                        sampleRate,
                        1,
                        16,
                        pcmDataLength,
                    )
                }
            }
    }

    @Synchronized
    override fun pauseRecording(): Result<Unit> {
        val state = _recordingState.value
        if (state !is RecordingState.Recording) {
            return Result.failure(IllegalStateException("Not recording"))
        }

        return runCatching {
            if (mediaRecorder != null) {
                mediaRecorder?.pause()
            } else if (audioRecord != null) {
                // For AudioRecord, we handle pause in the writing loop
            }
            stopTicker()
            _recordingState.value = RecordingState.Paused(durationMs)
        }
    }

    @Synchronized
    override fun resumeRecording(): Result<Unit> {
        val state = _recordingState.value
        if (state !is RecordingState.Paused) {
            return Result.failure(IllegalStateException("Not paused"))
        }

        return runCatching {
            if (mediaRecorder != null) {
                mediaRecorder?.resume()
            } else if (audioRecord != null) {
                // Resume handled in writing loop
            }
            _recordingState.value = RecordingState.Recording(durationMs, 0f)
            startTicker()
        }
    }

    @Synchronized
    override fun stopRecording(): Result<Long> {
        val state = _recordingState.value
        if (state is RecordingState.Idle) {
            return Result.failure(IllegalStateException("Not recording"))
        }

        return runCatching {
            stopTicker()

            mediaRecorder?.apply {
                try {
                    stop()
                } catch (e: RuntimeException) {
                }
                release()
            }
            mediaRecorder = null

            audioRecord?.apply {
                stop()
                release()
            }
            audioRecord = null

            // Wait for PCM writing job to finish
            val finalDuration = durationMs
            _recordingState.value = RecordingState.Idle
            _amplitudeFlow.value = emptyList()
            finalDuration
        }
    }

    override fun reset() {
        stopTicker()
        pcmWritingJob?.cancel()
        runCatching {
            mediaRecorder?.release()
        }
        mediaRecorder = null
        runCatching {
            audioRecord?.release()
        }
        audioRecord = null
        durationMs = 0L
        amplitudeHistory.clear()
        _amplitudeFlow.value = emptyList()
        _recordingState.value = RecordingState.Idle
    }

    private fun startTicker() {
        recordingJob?.cancel()
        recordingJob =
            scope.launch {
                while (true) {
                    delay(100)
                    durationMs += 100

                    val rawAmplitude =
                        try {
                            if (mediaRecorder != null) {
                                mediaRecorder?.maxAmplitude ?: 0
                            } else if (audioRecord != null) {
                                currentMaxAmplitude
                            } else {
                                0
                            }
                        } catch (e: Exception) {
                            0
                        }

                    // Max amplitude returns 0 to 32767. Let's normalize it to 0.0f - 1.0f
                    val normalized = (rawAmplitude.toFloat() / 32767.0f).coerceIn(0.0f, 1.0f)

                    // Add to scroll list
                    amplitudeHistory.add(normalized)
                    if (amplitudeHistory.size > 100) {
                        amplitudeHistory.removeAt(0)
                    }

                    _amplitudeFlow.value = amplitudeHistory.toList()
                    _recordingState.value = RecordingState.Recording(durationMs, normalized)
                }
            }
    }

    private fun stopTicker() {
        recordingJob?.cancel()
        recordingJob = null
    }
}
