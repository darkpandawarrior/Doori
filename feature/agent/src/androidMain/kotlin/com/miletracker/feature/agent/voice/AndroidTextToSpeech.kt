package com.miletracker.feature.agent.voice

import android.content.Context
import android.speech.tts.TextToSpeech as AndroidTts
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.Locale
import kotlin.coroutines.resume

class AndroidTextToSpeech(context: Context) : TextToSpeech {

    private var tts: AndroidTts? = null
    private val appContext = context.applicationContext

    init {
        tts = AndroidTts(appContext) { status ->
            if (status == AndroidTts.SUCCESS) {
                tts?.language = Locale.getDefault()
            }
        }
    }

    override suspend fun speak(text: String) {
        val engine = tts ?: return
        val stripped = text.stripMarkdownForTts()
        if (stripped.isBlank()) return
        suspendCancellableCoroutine { cont ->
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}
                override fun onDone(utteranceId: String?) { if (cont.isActive) cont.resume(Unit) }
                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) { if (cont.isActive) cont.resume(Unit) }
            })
            engine.speak(stripped, AndroidTts.QUEUE_FLUSH, null, "mileway_tts")
            cont.invokeOnCancellation { engine.stop() }
        }
    }

    override fun stop() {
        tts?.stop()
    }
}
