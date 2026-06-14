package com.voicerecorder.domain.model

enum class AudioFormat(val extension: String, val mimeType: String) {
    M4A("m4a", "audio/mp4"),
    AMR("3gp", "audio/3gpp"),
    WAV("wav", "audio/wav"),
    AAC("aac", "audio/aac"),
    OGG("ogg", "audio/ogg"),
    ;

    companion object {
        fun fromName(name: String): AudioFormat {
            return entries.find { it.name == name } ?: M4A
        }
    }
}
