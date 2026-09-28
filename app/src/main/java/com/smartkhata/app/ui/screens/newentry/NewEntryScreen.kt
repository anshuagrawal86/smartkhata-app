package com.smartkhata.app.ui.screens.newentry

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.ui.theme.*
import com.smartkhata.app.util.SpeechRecognizerHelper
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
    val isRecordingAudio by viewModel.isRecordingAudio.collectAsState()
    val isProcessingAI by viewModel.isProcessingAI.collectAsState()
    val saveSuccess by viewModel.saveSuccess.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

    var isListeningSpeech by remember { mutableStateOf(false) }

    // Speech Recognizer helper
    val speechHelper = remember {
        SpeechRecognizerHelper(
            context = context,
            onPartialResult = { partial -> viewModel.onSpeechRecognized(partial) },
            onFinalResult = { final ->
                viewModel.onSpeechRecognized(final)
                isListeningSpeech = false
            },
            onError = { err ->
                isListeningSpeech = false
                Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            }
        )
    }

    // Permission launcher for Mic
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isListeningSpeech = true
            speechHelper.startListening("hi-IN") // Starts in Hindi/Hinglish mode
        } else {
            Toast.makeText(context, "Microphone permission required for voice notes", Toast.LENGTH_SHORT).show()
        }
    }

    // Video Capture launcher
    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CaptureVideo()
    ) { success ->
        // Video captured to mediaFile
        viewModel.mediaFile.value?.let { file ->
            if (success && file.exists()) {
                viewModel.setVideoRecorded(file)
            }
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            Toast.makeText(context, "Entry saved successfully!", Toast.LENGTH_SHORT).show()
            onNavigateBack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.errorMessage.value = null
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
            // 1. Multimodal Quick Input Card (Voice / Video / Type)
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
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Speak or Type in English, Hindi, or Hinglish",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Voice Button
                        Button(
                            onClick = {
                                if (isListeningSpeech) {
                                    speechHelper.stopListening()
                                    isListeningSpeech = false
                                } else {
                                    val hasPerm = ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (hasPerm) {
                                        isListeningSpeech = true
                                        speechHelper.startListening("hi-IN")
                                    } else {
                                        micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isListeningSpeech) GaveRed else PrimaryBlue
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Mic, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(if (isListeningSpeech) "Listening…" else "Speak (बोलें)")
                        }

                        // Video Note Button
                        OutlinedButton(
                            onClick = {
                                val mediaDir = File(context.filesDir, "media").apply { mkdirs() }
                                val file = File(mediaDir, "VID_${System.currentTimeMillis()}.mp4")
                                viewModel.mediaFile.value = file
                                val uri = androidx.core.content.FileProvider.getUriForFile(
                                    context,
                                    "${context.packageName}.fileprovider",
                                    file
                                )
                                videoPickerLauncher.launch(uri)
                            },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = SecondaryTeal)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Video Note", color = SecondaryTeal)
                        }
                    }

                    if (isProcessingAI) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyzing entry with AI…", fontSize = 13.sp, color = PrimaryBlue)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Text Field for Raw Note / Transcription
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { viewModel.onInputTextChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text("e.g. 'Ramesh ko 500 diye kal chai ke' or 'Received 1200 from Anita'", fontSize = 14.sp)
                        },
                        minLines = 2,
                        maxLines = 4,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }

            // 2. Extracted / Structured Fields Card
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
                        text = "Ledger Details",
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

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
