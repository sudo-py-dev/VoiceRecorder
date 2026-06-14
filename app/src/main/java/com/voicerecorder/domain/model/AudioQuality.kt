package com.voicerecorder.domain.model

enum class AudioQuality {
    HIGH,
    MEDIUM,
    LOW,
    ;

    companion object {
        fun fromName(name: String): AudioQuality {
            return entries.find { it.name == name } ?: HIGH
        }
    }
}
