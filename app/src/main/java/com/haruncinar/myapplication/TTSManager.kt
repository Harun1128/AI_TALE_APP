package com.haruncinar.myapplication

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TTSManager private constructor(private val context: Context) {

    private var textToSpeech: TextToSpeech? = null
    private var isInitialized = false

    // TTS durumunu izlemek için StateFlow kullanımı
    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    // TTS ayarları
    var speechRate: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setSpeechRate(value)
        }

    var pitch: Float = 1.0f
        set(value) {
            field = value
            textToSpeech?.setPitch(value)
        }

    var language: Locale = Locale("tr", "TR")
        set(value) {
            field = value
            updateLanguage(value)
        }

    init {
        initTTS()
    }

    private fun initTTS() {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                updateLanguage(language)

                // Ayarları uygula
                textToSpeech?.setSpeechRate(speechRate)
                textToSpeech?.setPitch(pitch)

                // İlerleme dinleyicisini ayarla
                textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _isSpeaking.value = true
                    }

                    override fun onDone(utteranceId: String?) {
                        _isSpeaking.value = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        _isSpeaking.value = false
                        Toast.makeText(context, "TTS hatası oluştu", Toast.LENGTH_SHORT).show()
                    }
                })
            } else {
                Toast.makeText(context,
                    "TTS başlatılamadı: Hata kodu $status",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun updateLanguage(locale: Locale) {
        textToSpeech?.let { tts ->
            val result = tts.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA ||
                result == TextToSpeech.LANG_NOT_SUPPORTED) {

                // Türkçe desteklenmezse İngilizce'yi dene
                if (locale.language == "tr") {
                    Toast.makeText(context,
                        "Türkçe dili desteklenmiyor, İngilizce kullanılıyor",
                        Toast.LENGTH_LONG).show()
                    tts.setLanguage(Locale.US)
                } else {
                    Toast.makeText(context,
                        "Belirtilen dil desteklenmiyor",
                        Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Belirtilen metni seslendirir.
     * @param text Seslendirilecek metin
     * @param queueMode QUEUE_FLUSH (mevcut sesi temizler) veya QUEUE_ADD (sıraya ekler)
     */
    fun speak(text: String, queueMode: Int = TextToSpeech.QUEUE_FLUSH) {
        if (!isInitialized || text.isEmpty()) {
            return
        }

        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "ttsUtterance")

        textToSpeech?.speak(text, queueMode, params, "ttsUtterance")
    }

    /**
     * Hikaye için metni paragraf paragraf seslendirme
     * @param paragraphs Seslendirilecek paragrafların listesi
     */
    fun speakStory(text: String) {
        if (!isInitialized || text.isEmpty()) {
            return
        }

        // Metni paragraf paragraf böl
        val paragraphs = text.split("\n\n", "\n").filter { it.isNotBlank() }
        if (paragraphs.isEmpty()) return

        // İlk paragrafı FLUSH ile, diğerlerini ADD ile ekleyin
        val params = Bundle()
        params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "storyUtterance")

        textToSpeech?.speak(paragraphs[0], TextToSpeech.QUEUE_FLUSH, params, "storyUtterance0")

        // Kalan paragrafları sıraya ekleyin
        for (i in 1 until paragraphs.size) {
            textToSpeech?.speak(paragraphs[i], TextToSpeech.QUEUE_ADD, params, "storyUtterance$i")
        }
    }

    /**
     * Mevcut seslendirmeyi durdurur
     */
    fun stop() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    /**
     * TTS motorunu tamamen kapatır, kullanım bittikten sonra çağrılmalıdır
     */
    fun shutdown() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        _isSpeaking.value = false
        instance = null
    }

    companion object {
        @Volatile private var instance: TTSManager? = null

        /**
         * Singleton pattern ile tek bir TTS örneği oluşturur
         */
        fun getInstance(context: Context): TTSManager {
            return instance ?: synchronized(this) {
                instance ?: TTSManager(context.applicationContext).also { instance = it }
            }
        }
    }
}