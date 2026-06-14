package com.voicerecorder.presentation.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class FormatUtilsTest {
    @Test
    fun testFormatDuration() {
        // Test standard formatting (MM:SS)
        assertEquals("00:00", FormatUtils.formatDuration(0L))
        assertEquals("00:05", FormatUtils.formatDuration(5000L))
        assertEquals("01:15", FormatUtils.formatDuration(75000L))
        assertEquals("10:00", FormatUtils.formatDuration(600000L))
    }

    @Test
    fun testFormatDurationWithDeciseconds() {
        // Test timer formatting (MM:SS.d)
        assertEquals("00:00.0", FormatUtils.formatDurationWithDeciseconds(0L))
        assertEquals("00:05.2", FormatUtils.formatDurationWithDeciseconds(5200L))
        assertEquals("01:15.5", FormatUtils.formatDurationWithDeciseconds(75580L))
        assertEquals("10:00.9", FormatUtils.formatDurationWithDeciseconds(600910L))
    }

    @Test
    fun testFormatFileSize() {
        // Force locale to US to guarantee consistent decimal separator for standard unit tests
        Locale.setDefault(Locale.US)

        assertEquals("0 B", FormatUtils.formatFileSize(0L))
        assertEquals("0 B", FormatUtils.formatFileSize(-100L))
        assertEquals("1.0 KB", FormatUtils.formatFileSize(1024L))
        assertEquals("512.0 KB", FormatUtils.formatFileSize(524288L))
        assertEquals("1.0 MB", FormatUtils.formatFileSize(1048576L))
        assertEquals("1.5 MB", FormatUtils.formatFileSize(1572864L))
    }
}
