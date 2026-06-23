package com.miletracker

import com.miletracker.feature.agent.voice.TextToSpeech

class FakeTextToSpeech : TextToSpeech {
    override suspend fun speak(text: String) {}
    override fun stop() {}
}
