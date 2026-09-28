package com.smartkhata.app.ui.screens.newentry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
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
import com.smartkhata.app.ui.components.LiveSpeechToTextDialog
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.Formatters
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewEntryScreen(
    viewModel: NewEntryViewModel,
    initialMode: String = "type",
    contactId: Long = 0L,
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
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val entryDate by viewModel.entryDate.collectAsState()
    val isProcessingAI by viewModel.isProcessingAI.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    var showLiveSpeechDialog by remember { mutableStateOf(false) }
    var showVideoChoiceDialog by remember { mutableStateOf(false) }
    var showEntryDatePicker by remember { mutableStateOf(false) }

    // Initialize contact or trigger initial mode
    LaunchedEffect(Unit) {
        viewModel.init(initialMode, contactId)
        when (initialMode) {
            "voice" -> {
                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    showLiveSpeechDialog = true
                }
            }
            "video" -> {
                showVideoChoiceDialog = true
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

    // Video Gallery / Files Picker Launcher (100% reliable on BlueStacks & real devices)
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
            showLiveSpeechDialog = true
        } else {
            Toast.makeText(context, "Microphone permission required for voice input", Toast.LENGTH_SHORT).show()
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
            // 1. Smart Text / Speech-to-Text Input Card
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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Voice / Note Input",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Quick Speech-to-Text Mic Button
                        IconButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    showLiveSpeechDialog = true
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Live Speech-to-Text", tint = PrimaryBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Text Area for Note
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Type or speak: 'Ramesh 500' or 'Anita se 1200 mila' or 'Paid milk 60'…", fontSize = 13.sp)
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    // Action buttons row: [⚡ Convert Text] [🎙️ Speak / Live STT] [📹 Video Note]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // 1. Explicit Convert Button
                        Button(
                            onClick = { viewModel.parseAndApplyText() },
                            modifier = Modifier.weight(1.2f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Convert Note", fontSize = 12.sp)
                        }

                        // 2. Speak Button (Live on-screen STT)
                        OutlinedButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) {
                                    showLiveSpeechDialog = true
                                } else {
                                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            },
                            modifier = Modifier.weight(1.1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Speak (बोलें)", fontSize = 12.sp)
                        }

                        // 3. Video Note Button
                        OutlinedButton(
                            onClick = { showVideoChoiceDialog = true },
                            modifier = Modifier.weight(1.0f),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Video", fontSize = 12.sp)
                        }
                    }

                    // Direct Audio Recording fallback row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRecordingAudio) "🔴 Recording: ${recordingDuration}s" else "Need to record audio memo?",
                            fontSize = 12.sp,
                            fontWeight = if (isRecordingAudio) FontWeight.Bold else FontWeight.Normal,
                            color = if (isRecordingAudio) GaveRed else TextSecondary
                        )

                        TextButton(
                            onClick = {
                                if (isRecordingAudio) {
                                    viewModel.stopVoiceRecording()
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) viewModel.startVoiceRecording() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        ) {
                            Icon(
                                if (isRecordingAudio) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = GaveRed
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (isRecordingAudio) "Stop Audio Rec" else "Record Voice Audio", fontSize = 12.sp, color = GaveRed)
                        }
                    }

                    // Attached Media Card: Audio Note
                    if (mediaType == MediaType.AUDIO && mediaFile != null) {
                        val file = mediaFile!!
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "Voice Note (${file.length() / 1024} KB)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SecondaryTeal)
                                        Text(text = if (isPlayingAudio) "▶️ Playing..." else "Tap play to listen", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.toggleAudioPlayback() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                            contentDescription = "Play/Pause",
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = { viewModel.removeAttachment() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Attached Media Card: Video Note
                    if (mediaType == MediaType.VIDEO && mediaFile != null) {
                        val file = mediaFile!!
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(22.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(text = "Video Note (${file.length() / (1024 * 1024)} MB)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimaryBlue)
                                        Text(text = "Ready to attach", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(
                                        onClick = {
                                            try {
                                                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                                                val intent = Intent(Intent.ACTION_VIEW).apply {
                                                    setDataAndType(uri, "video/*")
                                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                                }
                                                context.startActivity(intent)
                                            } catch (e: Exception) {
                                                Toast.makeText(context, "Cannot open video player: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text("Watch", fontSize = 11.sp)
                                    }

                                    Spacer(modifier = Modifier.width(4.dp))

                                    IconButton(
                                        onClick = { viewModel.removeAttachment() },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }

                    // Smart Extraction Chips Deck
                    AnimatedVisibility(
                        visible = parsedPreview != null && ((parsedPreview?.amount ?: 0.0) > 0 || (parsedPreview?.personName != null)),
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
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("You Got (लिया)", fontWeight = FontWeight.SemiBold)
                        }
                    }

                    // Person Name Field
                    OutlinedTextField(
                        value = personName,
                        onValueChange = { viewModel.personName.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Person / Party Name (नाम)*") },
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
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = PrimaryBlue, modifier = Modifier.padding(start = 12.dp)) },
                        placeholder = { Text("0") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Editable Transaction Date Field (DatePicker)
                    OutlinedTextField(
                        value = Formatters.formatDate(entryDate),
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showEntryDatePicker = true },
                        label = { Text("Entry Date (तारीख)") },
                        leadingIcon = {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, tint = PrimaryBlue)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showEntryDatePicker = true }) {
                                Icon(Icons.Default.EditCalendar, contentDescription = "Select Date", tint = PrimaryBlue)
                            }
                        },
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

        // Live Speech-To-Text Dialog (Real-time on-screen transcription)
        if (showLiveSpeechDialog) {
            LiveSpeechToTextDialog(
                onDismiss = { showLiveSpeechDialog = false },
                onApplySpokenText = { spoken ->
                    viewModel.onSpeechRecognized(spoken)
                },
                onFallbackToAudioRecorder = {
                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                    if (hasPerm) viewModel.startVoiceRecording() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            )
        }

        // Date Picker Dialog
        if (showEntryDatePicker) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = entryDate)
            DatePickerDialog(
                onDismissRequest = { showEntryDatePicker = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            datePickerState.selectedDateMillis?.let { selected ->
                                viewModel.entryDate.value = selected
                            }
                            showEntryDatePicker = false
                        }
                    ) {
                        Text("Select Date")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showEntryDatePicker = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DatePicker(state = datePickerState)
            }
        }

        // Video Choice Dialog (Camera vs Gallery)
        if (showVideoChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showVideoChoiceDialog = false },
                title = { Text("Attach Video Note", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Record a short video or pick a video note from files:", fontSize = 13.sp, color = TextSecondary)
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
                                    Text("Select any video from storage", fontSize = 11.sp, color = TextSecondary)
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
