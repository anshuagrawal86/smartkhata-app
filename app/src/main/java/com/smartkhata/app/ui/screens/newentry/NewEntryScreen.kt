package com.smartkhata.app.ui.screens.newentry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters
import com.smartkhata.app.util.SpeechRecognizerHelper
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    viewModel: NewEntryViewModel,
    initialMode: String,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current

    val inputText by viewModel.inputText.collectAsState()
    val personName by viewModel.personName.collectAsState()
    val amountText by viewModel.amountText.collectAsState()
    val transactionType by viewModel.transactionType.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val mediaType by viewModel.mediaType.collectAsState()
    val mediaFile by viewModel.mediaFile.collectAsState()
    val parsedPreview by viewModel.parsedPreview.collectAsState()

    val isRecordingAudio by viewModel.isRecordingAudio.collectAsState()
    val recordingDuration by viewModel.recordingDurationSeconds.collectAsState()
    val isProcessingAI by viewModel.isProcessingAI.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    var showVideoChoiceDialog by remember { mutableStateOf(false) }

    // Fallback System Speech Recognition Dialog Intent (works on Google Keyboard / standard dialogs)
    val speechDialogLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val spoken = matches?.firstOrNull() ?: ""
            if (spoken.isNotBlank()) {
                viewModel.onSpeechRecognized(spoken)
            }
        }
    }

    // Video Capture Launcher
    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        viewModel.mediaFile.value?.let { file ->
            if (success && file.exists() && file.length() > 0) {
                viewModel.setVideoRecorded(file)
            }
        }
    }

    // Video Gallery Picker Launcher (100% reliable on emulators & phones)
    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.copyPickedVideoToInternalStorage(it) }
    }

    // Permission launcher for Mic
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.startVoiceRecording()
        } else {
            Toast.makeText(context, "Microphone permission required for audio recording", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Entry saved to Ledger successfully!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.errorMessage.value = null
        }
    }

    LaunchedEffect(infoMessage) {
        infoMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.infoMessage.value = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Diary & Ledger Entry", fontWeight = FontWeight.Bold, color = SurfaceWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = SurfaceWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Smart Text / Free Speech Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Smart Note / Voice Input",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )

                        // Quick System Mic Button
                        IconButton(
                            onClick = {
                                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak in English or Hindi (e.g., 'Ramesh ko 500 diye')")
                                }
                                try {
                                    speechDialogLauncher.launch(intent)
                                } catch (e: Exception) {
                                    // Fallback to internal audio recorder
                                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) viewModel.startVoiceRecording() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Voice Dialog", tint = PrimaryBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text Area for Free Text Note
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Type: 'Ramesh 500' or 'Anita se 1200 mila' or 'Paid milk 60'…", fontSize = 14.sp)
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons row: [⚡ Convert Text] [🎙️ Record Voice] [📹 Video Note]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Explicit Convert Button
                        Button(
                            onClick = { viewModel.parseAndApplyText() },
                            modifier = Modifier.weight(1.3f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Convert Note", fontSize = 13.sp)
                        }

                        // 2. Direct Audio Recorder Button (Emulator-safe)
                        OutlinedButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    viewModel.stopVoiceRecording()
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) viewModel.startVoiceRecording() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = if (isRecordingAudio) GaveRed else SecondaryTeal
                            )
                        ) {
                            Icon(
                                if (isRecordingAudio) Icons.Default.Stop else Icons.Default.Mic,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRecordingAudio) "Stop (${recordingDuration}s)" else "Voice Rec", fontSize = 12.sp)
                        }

                        // 3. Video Button
                        OutlinedButton(
                            onClick = { showVideoChoiceDialog = true },
                            modifier = Modifier.weight(0.9f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp), tint = PrimaryBlue)
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Video", fontSize = 12.sp)
                        }
                    }

                    // Recording indicator banner
                    if (isRecordingAudio) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(GaveRedBg)
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(GaveRed)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Recording audio: ${recordingDuration}s • Tap 'Stop' to attach",
                                fontSize = 12.sp,
                                color = GaveRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Media Attachment Badge
                    mediaFile?.let { file ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(PrimaryBlue.copy(alpha = 0.08f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (mediaType == MediaType.AUDIO) Icons.Default.Audiotrack else Icons.Default.VideoFile,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "${file.name} (${file.length() / 1024} KB)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = PrimaryBlue
                                )
                            }
                            IconButton(
                                onClick = { viewModel.removeAttachment() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.Gray, modifier = Modifier.size(14.dp))
                            }
                        }
                    }

                    // 2. Smart Extraction Chips Deck
                    AnimatedVisibility(
                        visible = parsedPreview != null && (parsedPreview?.amount ?: 0.0) > 0 || (parsedPreview?.personName != null),
                        enter = fadeIn() + slideInVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Auto-Detected Elements:",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                parsedPreview?.personName?.let { name ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("👤 $name") },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = PrimaryBlue.copy(alpha = 0.1f)
                                        )
                                    )
                                }

                                if ((parsedPreview?.amount ?: 0.0) > 0) {
                                    val amt = parsedPreview?.amount?.toInt() ?: 0
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("💰 ₹$amt") },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = GotGreenBg
                                        )
                                    )
                                }

                                parsedPreview?.type?.let { t ->
                                    val isGave = t == TransactionType.GAVE
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text(if (isGave) "🔴 Gave (दिया)" else "🟢 Got (लिया)") },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isGave) GaveRedBg else GotGreenBg
                                        )
                                    )
                                }

                                if (parsedPreview?.description?.isNotBlank() == true) {
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("📝 ${parsedPreview?.description}") }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Structured Ledger Fields Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Ledger Entry Details",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = TextPrimary
                    )

                    // Transaction Type Selector (Gave / Got)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = { viewModel.transactionType.value = TransactionType.GAVE },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (transactionType == TransactionType.GAVE) GaveRed else GaveRedBg,
                                contentColor = if (transactionType == TransactionType.GAVE) SurfaceWhite else GaveRed
                            )
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("You Gave (दिया)", fontWeight = FontWeight.SemiBold)
                        }

                        Button(
                            onClick = { viewModel.transactionType.value = TransactionType.GOT },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (transactionType == TransactionType.GOT) GotGreen else GotGreenBg,
                                contentColor = if (transactionType == TransactionType.GOT) SurfaceWhite else GotGreen
                            )
                        ) {
                            Icon(Icons.Default.CallReceived, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("You Got (लिया)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Person Name Field
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { viewModel.personName.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Person Name (नाम)") },
                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = PrimaryBlue) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Amount Field
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { viewModel.amountText.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Amount (रुपये)") },
                        leadingIcon = {
                            Text(
                                "₹",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (transactionType == TransactionType.GAVE) GaveRed else GotGreen,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Notes / Tags Field
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.notes.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes / Reason (विवरण)") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = TextSecondary) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 3. Save Button
            Button(
                onClick = { viewModel.saveEntry() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (transactionType == TransactionType.GAVE) GaveRed else GotGreen
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Save Entry to Ledger (सेव करें)",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Video Choice Dialog (Camera vs Gallery)
        if (showVideoChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showVideoChoiceDialog = false },
                title = { Text("Attach Video Note", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Record a short video or pick a video receipt from files:", fontSize = 13.sp, color = TextSecondary)
                        Spacer(modifier = Modifier.height(4.dp))

                        // Option 1: Camera
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showVideoChoiceDialog = false
                                    val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                                    val file = File(mediaDir, "VID_${System.currentTimeMillis()}.mp4")
                                    viewModel.mediaFile.value = file
                                    val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                    videoCaptureLauncher.launch(uri)
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Record with Camera", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Open camera to record a video note", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }

                        // Option 2: Gallery / Files (Emulator-safe)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showVideoChoiceDialog = false
                                    videoGalleryLauncher.launch("video/*")
                                },
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.08f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = SecondaryTeal)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Pick from Gallery / Files", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Select any video from phone/emulator storage", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showVideoChoiceDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
