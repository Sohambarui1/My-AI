package com.example.myai

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.*

@RequiresApi(Build.VERSION_CODES.FROYO)
@Composable
fun VoiceInputScreen() {
    var text by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isListening by remember { mutableStateOf(false) }

    val voiceRecognitionHelper = rememberVoiceRecognitionHelper(
        onResult = { result ->
            text = result
            error = null
        },
        onError = { errorMessage ->
            error = errorMessage
        },
        onListeningChange = { listening ->
            isListening = listening
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        GlowingMicIcon(isListening = isListening)

        Spacer(modifier = Modifier.height(32.dp))

        TextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("Speech input") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (voiceRecognitionHelper.isListening) {
                    voiceRecognitionHelper.stopListening()
                } else {
                    voiceRecognitionHelper.startListening()
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isListening) Color.Red else Color.Blue,
                contentColor = Color.White
            ),
            modifier = Modifier.width(200.dp)
        ) {
            Text(if (isListening) "STOP" else "SPEAK")
        }

        error?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = Color.Red,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun GlowingMicIcon(isListening: Boolean, modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition()

    val ringAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse
        )
    )

    val ringScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Restart
        )
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(120.dp)
    ) {
        if (isListening) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.Blue.copy(alpha = ringAlpha),
                    radius = size.minDimension / 2 * ringScale
                )
            }
        }

        Icon(
            imageVector = Icons.Default.Mic,
            contentDescription = "Microphone",
            modifier = Modifier.size(48.dp),
            tint = if (isListening) Color.Blue else Color.Gray
        )
    }
}

@RequiresApi(Build.VERSION_CODES.FROYO)
@Composable
fun rememberVoiceRecognitionHelper(
    onResult: (String) -> Unit,
    onError: (String) -> Unit,
    onListeningChange: (Boolean) -> Unit = {}
): VoiceRecognitionHelper {
    val context = LocalContext.current
    var isListening by remember { mutableStateOf(false) }

    val speechRecognizer = remember {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            SpeechRecognizer.createSpeechRecognizer(context)
        } else {
            onError("Speech recognition not available")
            null
        }
    }

    DisposableEffect(Unit) {
        val listener = object : android.speech.RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                isListening = true
                onListeningChange(true)
            }

            override fun onBeginningOfSpeech() {}

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                onListeningChange(false)
            }

            override fun onError(error: Int) {
                isListening = false
                onListeningChange(false)

                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> return
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> return
                    else -> onError("Error: ${getErrorText(error)}")
                }
            }

            override fun onResults(results: Bundle?) {
                results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()?.let {
                    onResult(it)
                }
                isListening = false
                onListeningChange(false)
            }

            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        }

        speechRecognizer?.setRecognitionListener(listener)

        onDispose {
            speechRecognizer?.destroy()
        }
    }

    return remember {
        object : VoiceRecognitionHelper {
            override fun startListening() {
                if (speechRecognizer == null) {
                    onError("SpeechRecognizer unavailable")
                    return
                }
                try {
                    speechRecognizer.startListening(createSpeechIntent())
                } catch (e: SecurityException) {
                    onError("Microphone permission required")
                }
            }

            override fun stopListening() {
                speechRecognizer?.stopListening()
                isListening = false
                onListeningChange(false)
            }

            override val isListening: Boolean
                get() = isListening
        }
    }
}

private fun createSpeechIntent(): Intent {
    return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
    }
}

private fun getErrorText(errorCode: Int): String {
    return when (errorCode) {
        SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
        SpeechRecognizer.ERROR_CLIENT -> "Operation canceled"
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission denied"
        SpeechRecognizer.ERROR_NETWORK -> "Network error"
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
        SpeechRecognizer.ERROR_NO_MATCH -> "No speech detected"
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
        SpeechRecognizer.ERROR_SERVER -> "Server error"
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
        else -> "Error code: $errorCode"
    }
}

interface VoiceRecognitionHelper {
    fun startListening()
    fun stopListening()
    val isListening: Boolean
}