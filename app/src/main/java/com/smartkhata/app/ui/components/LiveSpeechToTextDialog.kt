package com.smartkhata.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.data.parser.LocalHinglishParser
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.SpeechRecognizerHelper

@Composable
fun LiveSpeechToTextDialog(
    onDismiss: () -> Unit,
    onApplySpokenText: (String) -> Unit,
    onFallbackToAudioRecorder: () -> Unit
) {
    val context = LocalContext.current

    var selectedLang by remember { mutableStateOf("hi-IN") } // "hi-IN" or "en-IN"
    var isListening by remember { mutableStateOf(false) }
    var spokenText by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("Preparing voice recognizer...") }
    var isError by remember { mutableStateOf(false) }
    var rmsLevel by remember { mutableFloatStateOf(0f) }

    val infiniteTransition = rememberInfiniteTransition(label = "mic_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isListening) 1.25f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    // Speech Recognizer instance
    val recognizer = remember {
        SpeechRecognizerHelper(
            context = context.applicationContext,
            onReady = {
                isListening = true
                isError = false
                statusMessage = "Listening... Speak now (बोलिए)"
            },
            onRmsChanged = { rms ->
                rmsLevel = (rms.coerceIn(0f, 10f) / 10f)
            },
            onPartialResult = { partial ->
                spokenText = partial
                isError = false
                statusMessage = "Listening to your words..."
            },
            onFinalResult = { finalResult ->
                spokenText = finalResult
                isListening = false
                statusMessage = "Speech captured! Review & Apply"
            },
            onError = { errorMsg ->
                isListening = false
                isError = true
                statusMessage = errorMsg
            }
        )
    }

    // Start listening on mount or language switch
    fun triggerStart() {
        spokenText = ""
        isError = false
        statusMessage = "Listening... Speak now"
        recognizer.startListening(selectedLang)
    }

    LaunchedEffect(selectedLang) {
        triggerStart()
    }

    DisposableEffect(Unit) {
        onDispose {
            recognizer.stopListening()
        }
    }

    // Real-time parsed preview
    val liveParsed = remember(spokenText) {
        if (spokenText.isNotBlank()) LocalHinglishParser.parse(spokenText) else null
    }

    Dialog(
        onDismissRequest = {
            recognizer.stopListening()
            onDismiss()
        },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Voice Input (बोलकर एंट्री)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    IconButton(
                        onClick = {
                            recognizer.stopListening()
                            onDismiss()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Language Selection Pills
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    FilterChip(
                        selected = selectedLang == "hi-IN",
                        onClick = {
                            if (selectedLang != "hi-IN") {
                                selectedLang = "hi-IN"
                            }
                        },
                        label = { Text("🇮🇳 Hindi / Hinglish", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryBlue
                        )
                    )

                    FilterChip(
                        selected = selectedLang == "en-IN",
                        onClick = {
                            if (selectedLang != "en-IN") {
                                selectedLang = "en-IN"
                            }
                        },
                        label = { Text("🌐 English", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = PrimaryBlue.copy(alpha = 0.15f),
                            selectedLabelColor = PrimaryBlue
                        )
                    )
                }

                // Pulsing Mic Visualizer
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .padding(vertical = 8.dp)
                ) {
                    // Outer Waveform / Pulse Ring
                    Box(
                        modifier = Modifier
                            .size(if (isListening) (70 + (rmsLevel * 30)).dp else 70.dp)
                            .scale(if (isListening) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isError) GaveRed.copy(alpha = 0.15f)
                                else if (isListening) PrimaryBlue.copy(alpha = 0.18f)
                                else Color.LightGray.copy(alpha = 0.2f)
                            )
                    )

                    // Inner Mic Circle
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isError) GaveRed
                                else if (isListening) PrimaryBlue
                                else Color.Gray
                            )
                            .clickable {
                                if (isListening) {
                                    recognizer.stopListening()
                                    isListening = false
                                    statusMessage = "Tap mic to resume speaking"
                                } else {
                                    triggerStart()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isListening) Icons.Default.Mic else Icons.Default.MicNone,
                            contentDescription = "Microphone",
                            tint = SurfaceWhite,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                // Status Message
                Text(
                    text = statusMessage,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isError) GaveRed else if (isListening) PrimaryBlue else TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // PROMINENT SPEECH-TO-TEXT DISPLAY BOX
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, PrimaryBlue.copy(alpha = 0.3f), RoundedCornerShape(14.dp)),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (spokenText.isNotBlank()) PrimaryBlue.copy(alpha = 0.04f) else BackgroundLight
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "SPEECH-TO-TEXT (लाइव ट्रांसक्रिप्शन):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )

                            if (isListening) {
                                Text(
                                    text = "🔴 LIVE",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = GaveRed
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        if (spokenText.isNotBlank()) {
                            Text(
                                text = spokenText,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                lineHeight = 22.sp
                            )
                        } else {
                            Text(
                                text = "Speak in English or Hindi...\nE.g.: 'Ramesh ko 500 diye kal chai ke liye'\nOr: 'Anita se 1200 mila'",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                // Real-time Extracted Elements Preview
                liveParsed?.let { parsed ->
                    if ((parsed.personName != null) || parsed.amount > 0 || parsed.type != TransactionType.NOTE) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            parsed.personName?.let { name ->
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("👤 $name", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = PrimaryBlue.copy(alpha = 0.12f)
                                    )
                                )
                            }

                            if (parsed.amount > 0) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text("💰 ₹${parsed.amount.toInt()}", fontSize = 11.sp) },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = GotGreenBg
                                    )
                                )
                            }

                            val isGave = parsed.type == TransactionType.GAVE
                            SuggestionChip(
                                onClick = {},
                                label = { Text(if (isGave) "🔴 Gave (दिया)" else "🟢 Got (लिया)", fontSize = 11.sp) },
                                colors = SuggestionChipDefaults.suggestionChipColors(
                                    containerColor = if (isGave) GaveRedBg else GotGreenBg
                                )
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action Buttons
                if (spokenText.isNotBlank()) {
                    // Success state: Apply text
                    Button(
                        onClick = {
                            recognizer.stopListening()
                            onApplySpokenText(spokenText)
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply to Note (एंट्री करें)", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = { triggerStart() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Speak Again (फिर से बोलें)", fontSize = 13.sp)
                    }
                } else if (isError) {
                    // Error state: Show fallbacks
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { triggerStart() },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry Speaking")
                        }

                        OutlinedButton(
                            onClick = {
                                recognizer.stopListening()
                                onDismiss()
                                onFallbackToAudioRecorder()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Record Direct Voice Note (Audio Memo)", fontSize = 12.sp)
                        }
                    }
                } else {
                    // Listening state
                    OutlinedButton(
                        onClick = {
                            recognizer.stopListening()
                            onDismiss()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cancel", fontSize = 13.sp)
                    }
                }
            }
        }
    }
}
