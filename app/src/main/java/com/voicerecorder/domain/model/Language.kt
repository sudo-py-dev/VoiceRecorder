package com.voicerecorder.domain.model

enum class Language(val code: String) {
    SYSTEM("system"),
    ENGLISH("en"),
    SPANISH("es"),
    FRENCH("fr"),
    GERMAN("de"),
    HEBREW("iw"),
    ;

    companion object {
        fun fromCode(code: String): Language {
            return entries.firstOrNull { it.code == code } ?: SYSTEM
        }
    }
}
