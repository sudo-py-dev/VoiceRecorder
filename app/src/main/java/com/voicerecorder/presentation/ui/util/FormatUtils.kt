package com.voicerecorder.presentation.ui.util

import java.text.DateFormat
import java.util.Date
import java.util.Locale

object FormatUtils {
    fun formatDuration(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    fun formatDurationWithDeciseconds(durationMs: Long): String {
        val totalSeconds = durationMs / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        val deciseconds = (durationMs % 1000) / 100
        return String.format(Locale.getDefault(), "%02d:%02d.%d", minutes, seconds, deciseconds)
    }

    fun formatFileSize(sizeBytes: Long): String {
        if (sizeBytes <= 0) return "0 B"
        val kb = sizeBytes.toFloat() / 1024f
        val mb = kb / 1024f
        return if (mb >= 1f) {
            String.format(Locale.getDefault(), "%.1f MB", mb)
        } else {
            String.format(Locale.getDefault(), "%.1f KB", kb)
        }
    }

    fun formatDate(timestampMs: Long): String {
        val date = Date(timestampMs)
        return DateFormat.getDateTimeInstance(
            DateFormat.MEDIUM,
            DateFormat.SHORT,
            Locale.getDefault(),
        ).format(date)
    }
}
