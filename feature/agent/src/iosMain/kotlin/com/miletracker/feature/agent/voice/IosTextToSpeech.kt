package com.miletracker.feature.agent.voice

class IosTextToSpeech : TextToSpeech {
    override suspend fun speak(text: String) {} // TODO(ios): AVSpeechSynthesizer
    override fun stop() {}
}
