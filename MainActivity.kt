package com.example.sirianroid

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.RecognitionListener
import android.speech.tts.TextToSpeech
import android.view.View
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var micButton: Button
    private lateinit var stopButton: Button
    private lateinit var conversationText: TextView
    private lateinit var conversationScroll: ScrollView

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isListening = false

    private val commandProcessor = CommandProcessor(this)

    companion object {
        private const val REQUEST_RECORD_AUDIO = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        micButton = findViewById(R.id.micButton)
        stopButton = findViewById(R.id.stopButton)
        conversationText = findViewById(R.id.conversationText)
        conversationScroll = findViewById(R.id.conversationScroll)

        textToSpeech = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                textToSpeech?.language = Locale("ar")
            }
        }

        micButton.setOnClickListener {
            if (hasMicPermission()) {
                startListening()
            } else {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.RECORD_AUDIO),
                    REQUEST_RECORD_AUDIO
                )
            }
        }

        stopButton.setOnClickListener {
            stopListening()
        }
    }

    private fun hasMicPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_RECORD_AUDIO &&
            grantResults.isNotEmpty() &&
            grantResults[0] == PackageManager.PERMISSION_GRANTED
        ) {
            startListening()
        } else {
            appendMessage("المساعد: لا يمكنني الاستماع دون إذن الميكروفون.")
        }
    }

    private fun startListening() {
        if (isListening) return

        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            appendMessage("المساعد: التعرف على الكلام غير متوفر على هذا الجهاز.")
            return
        }

        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this).apply {
            setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    isListening = true
                    micButton.text = "🎤 أستمع..."
                }

                override fun onResults(results: Bundle?) {
                    isListening = false
                    micButton.text = "🎤"
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val spokenText = matches?.firstOrNull()
                    if (!spokenText.isNullOrBlank()) {
                        handleUserSpeech(spokenText)
                    }
                }

                override fun onError(error: Int) {
                    isListening = false
                    micButton.text = "🎤"
                    appendMessage("المساعد: لم أتمكن من فهم الصوت، حاول مرة أخرى.")
                }

                override fun onBeginningOfSpeech() {}
                override fun onRmsChanged(rmsdB: Float) {}
                override fun onBufferReceived(buffer: ByteArray?) {}
                override fun onEndOfSpeech() {}
                override fun onPartialResults(partialResults: Bundle?) {}
                override fun onEvent(eventType: Int, params: Bundle?) {}
            })
        }

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ar")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
        }
        speechRecognizer?.startListening(intent)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        micButton.text = "🎤"
    }

    private fun handleUserSpeech(text: String) {
        appendMessage("أنت: $text")
        val reply = commandProcessor.process(text)
        appendMessage("المساعد: $reply")
        speak(reply)
    }

    private fun speak(text: String) {
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "reply")
    }

    private fun appendMessage(message: String) {
        conversationText.append("$message\n\n")
        conversationScroll.post { conversationScroll.fullScroll(View.FOCUS_DOWN) }
    }

    override fun onDestroy() {
        speechRecognizer?.destroy()
        textToSpeech?.shutdown()
        super.onDestroy()
    }
}
