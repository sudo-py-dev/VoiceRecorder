package com.voicerecorder.presentation.service

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.voicerecorder.R
import com.voicerecorder.VoiceRecorderApplication
import com.voicerecorder.domain.model.RecordingState
import com.voicerecorder.domain.repository.AudioRecorderRepository
import com.voicerecorder.presentation.ui.util.FormatUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class RecordingService : Service() {
    private lateinit var recorderRepository: AudioRecorderRepository
    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var stateObserverJob: Job? = null

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_PAUSE = "ACTION_PAUSE"
        const val ACTION_RESUME = "ACTION_RESUME"
        const val ACTION_STOP = "ACTION_STOP"

        const val EXTRA_OUTPUT_PATH = "EXTRA_OUTPUT_PATH"
        const val EXTRA_FORMAT = "EXTRA_FORMAT"
        const val EXTRA_QUALITY = "EXTRA_QUALITY"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        val app = application as VoiceRecorderApplication
        recorderRepository = app.container.audioRecorderRepository

        observeRecorderState()
    }

    override fun onStartCommand(
        intent: Intent?,
        flags: Int,
        startId: Int,
    ): Int {
        when (intent?.action) {
            ACTION_START -> {
                val outputPath = intent.getStringExtra(EXTRA_OUTPUT_PATH)
                val formatName = intent.getStringExtra(EXTRA_FORMAT) ?: com.voicerecorder.domain.model.AudioFormat.M4A.name
                val qualityName = intent.getStringExtra(EXTRA_QUALITY) ?: com.voicerecorder.domain.model.AudioQuality.HIGH.name
                if (outputPath != null) {
                    startRecording(
                        outputPath,
                        com.voicerecorder.domain.model.AudioFormat.fromName(formatName),
                        com.voicerecorder.domain.model.AudioQuality.fromName(qualityName),
                    )
                }
            }
            ACTION_PAUSE -> pauseRecording()
            ACTION_RESUME -> resumeRecording()
            ACTION_STOP -> stopRecording()
        }
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startRecording(
        outputPath: String,
        format: com.voicerecorder.domain.model.AudioFormat,
        quality: com.voicerecorder.domain.model.AudioQuality,
    ) {
        val result = recorderRepository.startRecording(outputPath, format, quality)
        if (result.isSuccess) {
            startForegroundServiceCompat(createNotification(RecordingState.Recording(0, 0f)))
        } else {
            stopSelf()
        }
    }

    private fun pauseRecording() {
        recorderRepository.pauseRecording()
    }

    private fun resumeRecording() {
        recorderRepository.resumeRecording()
    }

    private fun stopRecording() {
        // ViewModel handles saving recording, service just stops itself
        recorderRepository.stopRecording()
        stopForegroundCompat()
        stopSelf()
    }

    private fun startForegroundServiceCompat(notification: Notification) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
                } else {
                    0
                },
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun stopForegroundCompat() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }
    }

    private fun observeRecorderState() {
        stateObserverJob?.cancel()
        stateObserverJob =
            serviceScope.launch {
                recorderRepository.recordingState.collectLatest { state ->
                    if (state is RecordingState.Idle) {
                        stopForegroundCompat()
                        stopSelf()
                    } else {
                        val notification = createNotification(state)
                        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
                        notificationManager.notify(NOTIFICATION_ID, notification)
                    }
                }
            }
    }

    private fun createNotification(state: RecordingState): Notification {
        val mainIntent =
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

        val contentPendingIntent =
            PendingIntent.getActivity(
                this,
                0,
                mainIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val stopIntent = Intent(this, RecordingService::class.java).apply { action = ACTION_STOP }
        val stopPendingIntent =
            PendingIntent.getService(
                this,
                1,
                stopIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )

        val builder =
            NotificationCompat.Builder(this, VoiceRecorderApplication.CHANNEL_ID)
                .setSmallIcon(android.R.drawable.presence_video_busy) // Red recording style default dot
                .setContentIntent(contentPendingIntent)
                .setOngoing(true)
                .setOnlyAlertOnce(true)

        when (state) {
            is RecordingState.Recording -> {
                val durationText = FormatUtils.formatDuration(state.durationMs)
                builder.setContentTitle(getString(R.string.notification_title))
                    .setContentText("${getString(R.string.state_recording)} ($durationText)")
                    .addAction(
                        android.R.drawable.ic_media_pause,
                        getString(R.string.state_paused),
                        getServicePendingIntent(ACTION_PAUSE, 2),
                    )
                    .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_delete),
                        stopPendingIntent,
                    )
            }
            is RecordingState.Paused -> {
                val durationText = FormatUtils.formatDuration(state.durationMs)
                builder.setContentTitle(getString(R.string.notification_title))
                    .setContentText("${getString(R.string.state_paused)} ($durationText)")
                    .addAction(
                        android.R.drawable.ic_media_play,
                        getString(R.string.state_recording),
                        getServicePendingIntent(ACTION_RESUME, 3),
                    )
                    .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        getString(R.string.action_delete),
                        stopPendingIntent,
                    )
            }
            else -> {}
        }

        return builder.build()
    }

    private fun getServicePendingIntent(
        actionStr: String,
        requestCode: Int,
    ): PendingIntent {
        val intent = Intent(this, RecordingService::class.java).apply { action = actionStr }
        return PendingIntent.getService(
            this,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
