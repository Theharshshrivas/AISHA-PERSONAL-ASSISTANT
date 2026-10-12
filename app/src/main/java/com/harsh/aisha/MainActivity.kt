
package com.harsh.aisha

import android.os.Bundle
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        tts = TextToSpeech(this) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale("hi", "IN")
            }
        }

        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    primary = Purple,
                    secondary = Teal,
                    background = Background,
                    surface = CardColor
                )
            ) {
                AishaApp(
                    onSpeak = { message ->
                        tts?.speak(
                            message,
                            TextToSpeech.QUEUE_FLUSH,
                            null,
                            "aisha_response"
                        )
                    }
                )
            }
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }
}

@Composable
private fun AishaApp(onSpeak: (String) -> Unit) {
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
                            reply = "Chat prototype ready hai. Abhi real AI connected nahi hai."
                        }
                        ActionCard("🔔", "Reminders", Modifier.weight(1f)) {
                            reply = "Reminder screen abhi prototype hai; alarms set nahi hote."
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
                                label = { Text("Type a message") },
                                singleLine = true
                            )
                            Button(
                                onClick = {
                                    reply = if (message.isBlank()) {
                                        "Namaste! Main Aisha hoon."
                                    } else {
                                        "Tumne kaha: $message\n\nAbhi main prototype hoon. Real AI reply ke liye AI provider connect karna baaki hai."
                                    }
                                    onSpeak(reply)
                                    message = ""
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Send & Speak  •  आवाज़ सुनें")
                            }
                        }
                    }
                }

                "Avatar" -> {
                    SectionTitle("AVATAR STUDIO")
                    Text(
                        "Choose a model",
                        color = Color.White,
                        fontSize = 17.sp
                    )
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
                        Text(
                            "A$selectedModel",
                            fontSize = 76.sp,
                            color = Purple
                        )
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
                        "Note: Models, camera views and outfits are UI previews only. Real 3D assets are not connected yet.",
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }

                "Memory" -> {
                    SectionTitle("MY MEMORY")
                    InfoCard(
                        "Personal notes",
                        "Persistent memory storage is not connected yet."
                    )
                    InfoCard(
                        "Reminders",
                        "Alarm scheduling will be added in a later milestone."
                    )
                    InfoCard(
                        "Privacy",
                        "Camera, microphone and background wake-word features are not active."
                    )
                }

                "Settings" -> {
                    SectionTitle("SETTINGS")
                    InfoCard(
                        "Voice",
                        "The device TextToSpeech engine is used for spoken test responses."
                    )
                    InfoCard(
                        "AI Provider",
                        "No external AI API is connected."
                    )
                    InfoCard(
                        "Wake word",
                        "Always-listening wake-word detection is not implemented."
                    )
                    InfoCard(
                        "Phone controls",
                        "Phone automation and app integrations are not enabled."
                    )
                }
            }

            Text(
                "AISHA • EARLY PROTOTYPE",
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
