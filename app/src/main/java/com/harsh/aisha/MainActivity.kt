
package com.harsh.aisha

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

private val Background = Color(0xFF100D20)
private val Purple = Color(0xFF9B7BFF)
private val Teal = Color(0xFF55E4D0)
private val CardColor = Color(0xFF201A35)

class MainActivity : ComponentActivity() {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null

    private var speechStatus by mutableStateOf("Ready")
    private var ttsReady = false
    private var onRecognizedText: ((String) -> Unit)? = null

    private var requestMicrophonePermission: (() -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            runOnUiThread {
                ttsReady = status == TextToSpeech.SUCCESS

                if (ttsReady) {
                    val result = tts?.setLanguage(Locale("hi", "IN"))

                    if (result == TextToSpeech.LANG_MISSING_DATA ||
                        result == TextToSpeech.LANG_NOT_SUPPORTED
                    ) {
                        speechStatus =
                            "Hindi voice unavailable; check TTS settings"
                    }
                }
            }
        }

        setContent {
            val microphonePermissionLauncher =
                rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { granted ->
                    if (granted) {
                        startListening()
                    } else {
                        speechStatus = "Microphone permission denied"
                    }
                }

            requestMicrophonePermission = {
                microphonePermissionLauncher.launch(
                    Manifest.permission.RECORD_AUDIO
                )
            }

            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Purple,
                    secondary = Teal,
                    background = Background,
                    surface = CardColor
                )
            ) {
                AishaApp(
                    speechStatus = speechStatus,
                    onSpeak = { speakText(it) },
                    onListen = { callback ->
                        onRecognizedText = callback
                        requestOrStartListening()
                    }
                )
            }
        }
    }

    private fun speakText(message: String) {
        if (ttsReady) {
            tts?.speak(
                message,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "aisha_response"
            )
        } else {
            speechStatus = "Text-to-Speech is not ready"
        }
    }

    private fun requestOrStartListening() {
        if (!SpeechRecognizer.isRecognitionAvailable(this)) {
            speechStatus = "Speech recognition service unavailable"
            return
        }

        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            speechStatus = "Requesting microphone permission..."
            requestMicrophonePermission?.invoke()
            return
        }

        startListening()
    }

    private fun startListening() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            speechStatus = "Microphone permission required"
            return
        }

        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

            speechRecognizer?.setRecognitionListener(
                object : RecognitionListener {

                    override fun onReadyForSpeech(params: Bundle?) {
                        speechStatus = "Listening... ab bolo"
                    }

                    override fun onBeginningOfSpeech() {
                        speechStatus = "Listening to your voice..."
                    }

                    override fun onRmsChanged(rmsdB: Float) = Unit

                    override fun onBufferReceived(buffer: ByteArray?) = Unit

                    override fun onEndOfSpeech() {
                        speechStatus = "Processing speech..."
                    }

                    override fun onError(error: Int) {
                        speechStatus = when (error) {
                            SpeechRecognizer.ERROR_AUDIO ->
                                "Audio error. Please try again."

                            SpeechRecognizer.ERROR_CLIENT ->
                                "Recognition stopped. Tap microphone to retry."

                            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
                                "Microphone permission denied."

                            SpeechRecognizer.ERROR_NETWORK,
                            SpeechRecognizer.ERROR_NETWORK_TIMEOUT ->
                                "Network issue. Check connection and retry."

                            SpeechRecognizer.ERROR_NO_MATCH,
                            SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
                                "No speech detected. Please try again."

                            SpeechRecognizer.ERROR_RECOGNIZER_BUSY ->
                                "Recognition busy. Wait and retry."

                            else ->
                                "Voice recognition failed. Please retry."
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(
                            SpeechRecognizer.RESULTS_RECOGNITION
                        )

                        val spokenText = matches?.firstOrNull()?.trim()

                        if (!spokenText.isNullOrBlank()) {
                            speechStatus = "Speech recognized successfully"
                            onRecognizedText?.invoke(spokenText)
                        } else {
                            speechStatus = "No speech recognized. Try again."
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) = Unit

                    override fun onEvent(eventType: Int, params: Bundle?) = Unit
                }
            )

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "hi-IN")
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(
                    RecognizerIntent.EXTRA_PROMPT,
                    "Aisha se baat karne ke liye bolo"
                )
            }

            speechStatus = "Starting microphone..."
            speechRecognizer?.startListening(intent)

        } catch (exception: Exception) {
            speechStatus = "Could not start microphone. Please retry."
        }
    }

    override fun onDestroy() {
        requestMicrophonePermission = null

        speechRecognizer?.cancel()
        speechRecognizer?.destroy()
        speechRecognizer = null

        tts?.stop()
        tts?.shutdown()
        tts = null

        super.onDestroy()
    }
}

@Composable
private fun AishaApp(
    speechStatus: String,
    onSpeak: (String) -> Unit,
    onListen: (onResult: (String) -> Unit) -> Unit
) {
    var selectedTab by remember { mutableStateOf("Home") }
    var selectedView by remember { mutableStateOf("Close-up") }
    var selectedModel by remember { mutableIntStateOf(1) }
    var selectedOutfit by remember { mutableStateOf("Casual") }
    var message by remember { mutableStateOf("") }
    var reply by remember {
        mutableStateOf("Namaste! Main Aisha hoon. Mujhe Hello bolo!")
    }

    val tabs = listOf("Home", "Avatar", "Memory", "Settings")

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "AISHA",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "YOUR PERSONAL AI COMPANION",
                        color = Teal,
                        fontSize = 10.sp,
                        letterSpacing = 1.5.sp
                    )
                }
                Text("✦", color = Purple, fontSize = 34.sp)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                tabs.forEach { tab ->
                    FilterChip(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(tab, fontSize = 11.sp) }
                    )
                }
            }

            when (selectedTab) {
                "Home" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(225.dp)
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color(0xFF30234F),
                                        Color(0xFF152C3A)
                                    )
                                ),
                                RoundedCornerShape(28.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(104.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(Purple, Teal)
                                        ),
                                        RoundedCornerShape(52.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("A", fontSize = 58.sp, color = Background)
                            }

                            Text(
                                "Hey! I'm Aisha",
                                color = Color.White,
                                fontSize = 23.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Text(
                                "Your day, your ideas, your companion.",
                                color = Color.LightGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    SectionTitle("QUICK ACTIONS")

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionCard("💬", "Chat", Modifier.weight(1f)) {
                            reply = "Chat ready hai. Real AI abhi connect nahi hai."
                        }

                        ActionCard("🔔", "Reminders", Modifier.weight(1f)) {
                            reply = "Reminder scheduling abhi implement nahi hui hai."
                        }

                        ActionCard("🧠", "Memory", Modifier.weight(1f)) {
                            selectedTab = "Memory"
                        }
                    }

                    SectionTitle("TALK TO AISHA")

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = CardColor
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(reply, color = Color.White)

                            OutlinedTextField(
                                value = message,
                                onValueChange = { message = it },
                                modifier = Modifier.fillMaxWidth(),
                                label = { Text("Type or use microphone") },
                                maxLines = 4
                            )

                            Button(
                                onClick = {
                                    onListen { recognized ->
                                        message = recognized
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🎙 Speak to Aisha")
                            }

                            Text(
                                speechStatus,
                                color = Teal,
                                fontSize = 12.sp
                            )

                            Button(
                                onClick = {
                                    val input = message.trim()

                                    reply = when {
                                        input.isBlank() ->
                                            "Namaste! Main Aisha hoon."

                                        input.contains(
                                            "hello",
                                            ignoreCase = true
                                        ) ||
                                        input.contains(
                                            "namaste",
                                            ignoreCase = true
                                        ) ||
                                        input.contains("नमस्ते") ->
                                            "Namaste! 😊 Main Aisha hoon. Batao, main tumhari kya help kar sakti hoon?"

                                        input.contains(
                                            "your name",
                                            ignoreCase = true
                                        ) ||
                                        input.contains("tumhara naam") ->
                                            "Mera naam Aisha hai!"

                                        else ->
                                            "Maine suna: $input. Abhi mera real AI brain connect nahi hua hai, isliye main iska intelligent answer abhi nahi de sakti."
                                    }

                                    onSpeak(reply)
                                    message = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send & Speak • आवाज़ सुनें")
                            }
                        }
                    }
                }

                "Avatar" -> {
                    SectionTitle("AVATAR STUDIO")
                    Text("Choose a model", color = Color.White, fontSize = 17.sp)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (1..5).forEach { number ->
                            FilterChip(
                                selected = selectedModel == number,
                                onClick = { selectedModel = number },
                                label = { Text("Model $number") }
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .background(CardColor, RoundedCornerShape(24.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("A$selectedModel", fontSize = 76.sp, color = Purple)
                    }

                    SectionTitle("CAMERA VIEW")

                    listOf("Close-up", "Half body", "Full body").forEach { view ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = selectedView == view,
                                onClick = { selectedView = view }
                            )
                            Text(view, color = Color.White)
                        }
                    }

                    SectionTitle("OUTFIT")

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Casual", "Traditional", "Smart", "Comfy")
                            .forEach { outfit ->
                                FilterChip(
                                    selected = selectedOutfit == outfit,
                                    onClick = { selectedOutfit = outfit },
                                    label = { Text(outfit) }
                                )
                            }
                    }

                    Text(
                        "Selected: Model $selectedModel • $selectedView • $selectedOutfit",
                        color = Teal
                    )

                    Text(
                        "Models, views and outfits are UI previews only. Real 3D assets are not connected.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                "Memory" -> {
                    SectionTitle("MY MEMORY")
                    InfoCard("Personal notes", "Persistent memory storage is not connected yet.")
                    InfoCard("Reminders", "Alarm scheduling will be added in a later phase.")
                    InfoCard(
                        "Privacy",
                        "Microphone is used when you tap Speak. Background wake-word detection is not active."
                    )
                }

                "Settings" -> {
                    SectionTitle("SETTINGS")
                    InfoCard(
                        "Voice",
                        "Uses Android speech recognition and the device Text-to-Speech engine."
                    )
                    InfoCard("AI Provider", "No external AI API is connected yet.")
                    InfoCard("Wake word", "Always-listening wake-word detection is not implemented.")
                    InfoCard("Phone controls", "Phone automation and app integrations are not enabled.")
                }
            }

            Text(
                "AISHA • VOICE PHASE",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        title,
        color = Teal,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.5.sp
    )
}

@Composable
private fun ActionCard(
    emoji: String,
    title: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = modifier.height(95.dp),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(emoji, fontSize = 23.sp)
            Text(title, color = Color.White, fontSize = 11.sp)
        }
    }
}

@Composable
private fun InfoCard(title: String, description: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
            Text(description, color = Color.LightGray, fontSize = 13.sp)
        }
    }
}
