package com.voicerecorder

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.voicerecorder.di.AppContainer
import com.voicerecorder.di.AppContainerImpl

class VoiceRecorderApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        com.voicerecorder.presentation.ui.crash.CrashHandler.initialize(this)
        super.onCreate()
        container = AppContainerImpl(this)

        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.app_name)
            val descriptionText = "Channel for background recording notifications"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel =
                NotificationChannel(CHANNEL_ID, name, importance).apply {
                    description = descriptionText
                }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    companion object {
        const val CHANNEL_ID = "recording_channel"
    }
}
