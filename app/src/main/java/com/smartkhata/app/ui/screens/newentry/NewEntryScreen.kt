package com.smartkhata.app.ui.screens.newentry

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
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
    val audioAmplitudes by viewModel.audioAmplitudes.collectAsState()
    val isPlayingAudio by viewModel.isPlayingAudio.collectAsState()
    val entryDate by viewModel.entryDate.collectAsState()
    val isProcessingAI by viewModel.isProcessingAI.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val infoMessage by viewModel.infoMessage.collectAsState()

    var showLiveSpeechDialog by remember { mutableStateOf(false) }
    var showVideoChoiceDialog by remember { mutableStateOf(false) }
    var showEntryDatePicker by remember { mutableStateOf(false) }

    // Safe File Provider URI generator for video capture
    var pendingVideoFile by remember { mutableStateOf<File?>(null) }
    var pendingVideoUri by remember { mutableStateOf<Uri?>(null) }

    fun prepareVideoUri(): Uri {
        val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
        val file = File(mediaDir, "VID_${System.currentTimeMillis()}.mp4")
        pendingVideoFile = file
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        pendingVideoUri = uri
        return uri
    }

    // 1. Video Capture Launcher (With runtime camera permission check)
    val videoCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        val file = pendingVideoFile
        if (success && file != null && file.exists() && file.length() > 0) {
            viewModel.setVideoRecorded(file)
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            try {
                val uri = prepareVideoUri()
                videoCaptureLauncher.launch(uri)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open camera: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Camera permission required to record video", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. Video Gallery Picker Launcher (Never crashes on emulators or Pixels)
    val videoGalleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.copyPickedVideoToInternalStorage(it) }
    }

    // 3. Audio / Mic Permission Launcher
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            showLiveSpeechDialog = true
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    // Initial setup
    LaunchedEffect(Unit) {
        viewModel.init(initialMode, contactId)
        when (initialMode) {
            "voice" -> {
                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                if (hasPerm) {
                    showLiveSpeechDialog = true
                } else {
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
            "video" -> {
                showVideoChoiceDialog = true
            }
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
        contentWindowInsets = WindowInsets.safeDrawing,
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
        },
        bottomBar = {
            // Sticky Bottom Action Dock above System Navigation Bar & Keyboard
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding(),
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                color = SurfaceWhite
            ) {
                Button(
                    onClick = { viewModel.saveEntry() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
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
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(BackgroundLight)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Smart Note / Voice & Video Input Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Voice / Note Input",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }

                        // Mic Button
                        IconButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) showLiveSpeechDialog = true else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = "Live Speech-to-Text", tint = PrimaryBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Main Free-form Note Text Box
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("Type or speak: 'Paid 500 to Ramesh for groceries' or 'Anita se 1200 mila'…", fontSize = 13.sp, color = Color.Gray)
                        },
                        minLines = 3,
                        maxLines = 6,
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action buttons row: [⚡ Convert Note] [🎙️ Speak / Live STT] [📹 Video]
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.parseAndApplyText() },
                            modifier = Modifier.weight(1.2f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Convert Note", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = {
                                val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                if (hasPerm) showLiveSpeechDialog = true else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            },
                            modifier = Modifier.weight(1.1f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryBlue)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Speak", fontSize = 12.sp)
                        }

                        OutlinedButton(
                            onClick = { showVideoChoiceDialog = true },
                            modifier = Modifier.weight(1.0f).height(44.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SecondaryTeal)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(2.dp))
                            Text("Video", fontSize = 12.sp)
                        }
                    }

                    // Direct Audio Recording Flow with Live Waveform
                    Spacer(modifier = Modifier.height(10.dp))
                    if (isRecordingAudio) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = GaveRedBg),
                            border = BorderStroke(1.dp, GaveRed.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(10.dp)
                                                .clip(CircleShape)
                                                .background(GaveRed)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Recording: ${recordingDuration}s",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = GaveRed
                                        )
                                    }

                                    Button(
                                        onClick = { viewModel.stopVoiceRecording() },
                                        colors = ButtonDefaults.buttonColors(containerColor = GaveRed),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Stop", fontSize = 12.sp)
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Live Waveform Bars
                                Row(
                                    modifier = Modifier.height(36.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val amps = audioAmplitudes.takeLast(24)
                                    if (amps.isEmpty()) {
                                        repeat(16) {
                                            Box(modifier = Modifier.width(3.dp).height(6.dp).background(GaveRed.copy(alpha = 0.4f), RoundedCornerShape(2.dp)))
                                        }
                                    } else {
                                        amps.forEach { amp ->
                                            val barHeight = (amp * 36f).coerceIn(4f, 36f).dp
                                            Box(modifier = Modifier.width(3.dp).height(barHeight).background(GaveRed, RoundedCornerShape(2.dp)))
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Or record a quick voice audio clip:",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )

                            TextButton(
                                onClick = {
                                    val hasPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) viewModel.startVoiceRecording() else micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            ) {
                                Icon(Icons.Default.FiberManualRecord, contentDescription = null, modifier = Modifier.size(12.dp), tint = GaveRed)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Record Voice Audio", fontSize = 12.sp, color = GaveRed, fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }

                    // Attached Media: Audio Note Player Card
                    if (mediaType == MediaType.AUDIO && mediaFile != null) {
                        val file = mediaFile!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Audiotrack, contentDescription = null, tint = SecondaryTeal, modifier = Modifier.size(24.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "Voice Note (${file.length() / 1024} KB)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = SecondaryTeal)
                                        Text(text = if (isPlayingAudio) "▶️ Playing audio..." else "Tap play to review", fontSize = 11.sp, color = TextSecondary)
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { viewModel.toggleAudioPlayback() },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlayingAudio) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                            contentDescription = "Play/Pause",
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(28.dp)
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

                    // Attached Media: Video Note Card
                    if (mediaType == MediaType.VIDEO && mediaFile != null) {
                        val file = mediaFile!!
                        Spacer(modifier = Modifier.height(10.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f))
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Videocam, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(26.dp))
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = "Video Note Attached", fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = PrimaryBlue)
                                        Text(text = "${file.length() / (1024 * 1024)} MB", fontSize = 11.sp, color = TextSecondary)
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
                                                Toast.makeText(context, "Cannot open video: ${e.message}", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
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

                    // Extracted Entity Chips Deck
                    AnimatedVisibility(
                        visible = parsedPreview != null && ((parsedPreview?.amount ?: 0.0) > 0 || (parsedPreview?.personName != null)),
                        enter = fadeIn() + slideInVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Text(
                                text = "Smart Extracted (Tap to adjust):",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                parsedPreview?.personName?.let { name ->
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("👤 $name", fontWeight = FontWeight.SemiBold) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = PrimaryBlue.copy(alpha = 0.1f),
                                            labelColor = PrimaryBlue
                                        )
                                    )
                                }

                                if ((parsedPreview?.amount ?: 0.0) > 0) {
                                    val amt = parsedPreview?.amount?.toInt() ?: 0
                                    SuggestionChip(
                                        onClick = {},
                                        label = { Text("💰 ₹$amt", fontWeight = FontWeight.Bold) },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = GotGreenBg,
                                            labelColor = GotGreen
                                        )
                                    )
                                }

                                parsedPreview?.type?.let { t ->
                                    val isGave = t == TransactionType.GAVE
                                    SuggestionChip(
                                        onClick = {
                                            viewModel.transactionType.value = if (isGave) TransactionType.GOT else TransactionType.GAVE
                                        },
                                        label = { Text(if (isGave) "🔴 You Gave (दिया)" else "🟢 You Got (लिया)") },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = if (isGave) GaveRedBg else GotGreenBg,
                                            labelColor = if (isGave) GaveRed else GotGreen
                                        )
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
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
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
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (transactionType == TransactionType.GAVE) GaveRed else GaveRedBg,
                                contentColor = if (transactionType == TransactionType.GAVE) SurfaceWhite else GaveRed
                            )
                        ) {
                            Icon(Icons.Default.ArrowOutward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("You Gave (दिया)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }

                        Button(
                            onClick = { viewModel.transactionType.value = TransactionType.GOT },
                            modifier = Modifier.weight(1f).height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (transactionType == TransactionType.GOT) GotGreen else GotGreenBg,
                                contentColor = if (transactionType == TransactionType.GOT) SurfaceWhite else GotGreen
                            )
                        ) {
                            Icon(Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("You Got (लिया)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
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
                        leadingIcon = { Text("₹", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PrimaryBlue, modifier = Modifier.padding(start = 12.dp)) },
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

                    // Full Descriptive Notes / Tags Field (Never Truncates)
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.notes.value = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Notes / Reason (विवरण)") },
                        placeholder = { Text("Detailed note or remarks for this transaction") },
                        leadingIcon = { Icon(Icons.Default.Notes, contentDescription = null, tint = TextSecondary) },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Live Speech-To-Text Dialog (Real-time on-screen streaming)
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

        // Video Choice Dialog (Safe Camera vs Gallery)
        if (showVideoChoiceDialog) {
            AlertDialog(
                onDismissRequest = { showVideoChoiceDialog = false },
                title = { Text("Attach Video Note", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Record a short video or pick a video note from files:", fontSize = 13.sp, color = TextSecondary)

                        // Option 1: Camera (With Camera Permission Check)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showVideoChoiceDialog = false
                                    val hasCameraPerm = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                                    if (hasCameraPerm) {
                                        try {
                                            val uri = prepareVideoUri()
                                            videoCaptureLauncher.launch(uri)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Cannot open camera: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = PrimaryBlue.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = PrimaryBlue)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Record with Camera", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Record video note using camera", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }

                        // Option 2: Gallery / Files (Zero Crash Risk)
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showVideoChoiceDialog = false
                                    try {
                                        videoGalleryLauncher.launch("video/*")
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Cannot open file picker: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                },
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = SecondaryTeal.copy(alpha = 0.08f)),
                            border = BorderStroke(1.dp, SecondaryTeal.copy(alpha = 0.2f))
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.VideoLibrary, contentDescription = null, tint = SecondaryTeal)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text("Pick from Gallery / Files", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                    Text("Select any video from device storage", fontSize = 11.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showVideoChoiceDialog = false }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
