package com.voicerecorder.domain.model

enum class SaveLocation {
    PRIVATE,
    PUBLIC,
    ;

    companion object {
        fun fromName(name: String): SaveLocation {
            return entries.find { it.name == name } ?: PRIVATE
        }
    }
}
