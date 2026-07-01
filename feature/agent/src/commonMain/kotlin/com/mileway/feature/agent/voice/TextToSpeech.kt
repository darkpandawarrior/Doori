package com.mileway.feature.agent.voice

interface TextToSpeech {
    suspend fun speak(text: String)
    fun stop()
}

fun String.stripMarkdownForTts(): String =
    replace(Regex("\\*\\*(.*?)\\*\\*"), "$1")
        .replace(Regex("\\*(.*?)\\*"), "$1")
        .replace(Regex("_(.*?)_"), "$1")
        .replace(Regex("#+\\s?"), "")
        .replace(Regex("\\[([^]]+)]\\([^)]+\\)"), "$1")
        .replace(Regex("[*_#`>|]"), "")
        .trim()
