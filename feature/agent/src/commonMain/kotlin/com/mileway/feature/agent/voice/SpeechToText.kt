package com.mileway.feature.agent.voice

import kotlinx.coroutines.flow.Flow

sealed interface SpeechEvent {
    data class Partial(val text: String) : SpeechEvent
    data class Final(val text: String) : SpeechEvent
    data class RmsChanged(val rms: Float) : SpeechEvent
    data object Error : SpeechEvent
}

interface SpeechToText {
    fun listen(): Flow<SpeechEvent>
    fun stop()
}
