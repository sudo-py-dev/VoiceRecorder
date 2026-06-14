package com.voicerecorder.domain

import com.voicerecorder.data.local.RecordingEntity
import com.voicerecorder.data.local.toDomain
import com.voicerecorder.data.local.toEntity
import com.voicerecorder.domain.model.AudioFormat
import com.voicerecorder.domain.model.AudioQuality
import com.voicerecorder.domain.model.AudioRecording
import com.voicerecorder.domain.model.Language
import com.voicerecorder.domain.model.SaveLocation
import org.junit.Assert.assertEquals
import org.junit.Test

class AudioRecordingTest {
    @Test
    fun testEntityToDomainMapping() {
        val entity =
            RecordingEntity(
                id = 5L,
                title = "Test Talk",
                filePath = "/path/to/test.m4a",
                durationMs = 120000L,
                fileSize = 4096L,
                timestamp = 1718320000000L,
            )

        val domain = entity.toDomain()

        assertEquals(entity.id, domain.id)
        assertEquals(entity.title, domain.title)
        assertEquals(entity.filePath, domain.filePath)
        assertEquals(entity.durationMs, domain.durationMs)
        assertEquals(entity.fileSize, domain.fileSize)
        assertEquals(entity.timestamp, domain.timestamp)
    }

    @Test
    fun testDomainToEntityMapping() {
        val domain =
            AudioRecording(
                id = 10L,
                title = "Another Talk",
                filePath = "/path/another.m4a",
                durationMs = 60000L,
                fileSize = 1024L,
                timestamp = 1718330000000L,
            )

        val entity = domain.toEntity()

        assertEquals(domain.id, entity.id)
        assertEquals(domain.title, entity.title)
        assertEquals(domain.filePath, entity.filePath)
        assertEquals(domain.durationMs, entity.durationMs)
        assertEquals(domain.fileSize, entity.fileSize)
        assertEquals(domain.timestamp, entity.timestamp)
    }

    @Test
    fun testLanguageFromCodeFallback() {
        // Correct mappings
        assertEquals(Language.SYSTEM, Language.fromCode("system"))
        assertEquals(Language.ENGLISH, Language.fromCode("en"))
        assertEquals(Language.SPANISH, Language.fromCode("es"))
        assertEquals(Language.FRENCH, Language.fromCode("fr"))
        assertEquals(Language.GERMAN, Language.fromCode("de"))
        assertEquals(Language.HEBREW, Language.fromCode("iw"))

        // Fallback default
        assertEquals(Language.SYSTEM, Language.fromCode("invalid_code"))
    }

    @Test
    fun testAudioFormatFromNameFallback() {
        assertEquals(AudioFormat.M4A, AudioFormat.fromName("M4A"))
        assertEquals(AudioFormat.AMR, AudioFormat.fromName("AMR"))
        assertEquals(AudioFormat.M4A, AudioFormat.fromName("INVALID"))
    }

    @Test
    fun testAudioQualityFromNameFallback() {
        assertEquals(AudioQuality.HIGH, AudioQuality.fromName("HIGH"))
        assertEquals(AudioQuality.MEDIUM, AudioQuality.fromName("MEDIUM"))
        assertEquals(AudioQuality.LOW, AudioQuality.fromName("LOW"))
        assertEquals(AudioQuality.HIGH, AudioQuality.fromName("INVALID"))
    }

    @Test
    fun testSaveLocationFromNameFallback() {
        assertEquals(SaveLocation.PRIVATE, SaveLocation.fromName("PRIVATE"))
        assertEquals(SaveLocation.PUBLIC, SaveLocation.fromName("PUBLIC"))
        assertEquals(SaveLocation.PRIVATE, SaveLocation.fromName("INVALID"))
    }
}
