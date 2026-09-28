package com.smartkhata.app.ui.screens.newentry

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.ParsedTransaction
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.data.parser.GeminiHinglishParser
import com.smartkhata.app.data.parser.LocalHinglishParser
import com.smartkhata.app.data.repository.LedgerRepository
import com.smartkhata.app.util.AudioRecorderUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

class NewEntryViewModel(
    application: Application,
    private val repository: LedgerRepository
) : AndroidViewModel(application) {

    private val geminiParser = GeminiHinglishParser(application)
    private val audioRecorder = AudioRecorderUtil(application)

    val inputText = MutableStateFlow("")
    val personName = MutableStateFlow("")
    val amountText = MutableStateFlow("")
    val transactionType = MutableStateFlow(TransactionType.GAVE)
    val entryDate = MutableStateFlow(System.currentTimeMillis())
    val dueDate = MutableStateFlow<Long?>(null)
    val notes = MutableStateFlow("")
    val mediaType = MutableStateFlow(MediaType.TEXT)
    val mediaFile = MutableStateFlow<File?>(null)

    // Extracted live tokens for UI feedback
    val parsedPreview = MutableStateFlow<ParsedTransaction?>(null)

    val isRecordingAudio = MutableStateFlow(false)
    val recordingDurationSeconds = MutableStateFlow(0)
    val currentAudioAmplitude = MutableStateFlow(0)
    val isProcessingAI = MutableStateFlow(false)
    val saveSuccess = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    val infoMessage = MutableStateFlow<String?>(null)

    fun onInputTextChanged(text: String) {
        inputText.value = text
        if (text.isNotBlank()) {
            val parsed = LocalHinglishParser.parse(text)
            parsedPreview.value = parsed

            // Auto-fill fields whenever reliable elements are found
            if (parsed.personName != null) {
                personName.value = parsed.personName
            }
            if (parsed.amount > 0) {
                amountText.value = if (parsed.amount % 1.0 == 0.0) parsed.amount.toInt().toString() else parsed.amount.toString()
            }
            if (parsed.type != TransactionType.NOTE) {
                transactionType.value = parsed.type
            }
            if (parsed.description.isNotBlank()) {
                notes.value = parsed.description
            }
            if (parsed.dueDateEpochMs != null) {
                dueDate.value = parsed.dueDateEpochMs
            }
        } else {
            parsedPreview.value = null
        }
    }

    fun parseAndApplyText() {
        val text = inputText.value.trim()
        if (text.isEmpty()) {
            errorMessage.value = "Please type something to convert (e.g., 'Ramesh 500' or 'Anita se 1200 mila')"
            return
        }
        val parsed = LocalHinglishParser.parse(text)
        parsedPreview.value = parsed

        if (parsed.personName != null) {
            personName.value = parsed.personName
        }
        if (parsed.amount > 0) {
            amountText.value = if (parsed.amount % 1.0 == 0.0) parsed.amount.toInt().toString() else parsed.amount.toString()
        }
        transactionType.value = parsed.type
        if (parsed.description.isNotBlank()) {
            notes.value = parsed.description
        }
        if (parsed.dueDateEpochMs != null) {
            dueDate.value = parsed.dueDateEpochMs
        }
        infoMessage.value = "Converted: ${parsed.personName ?: "Party"} • ₹${parsed.amount.toInt()} • ${parsed.type.name}"
    }

    fun startVoiceRecording() {
        try {
            val file = audioRecorder.startRecording()
            mediaFile.value = file
            mediaType.value = MediaType.AUDIO
            isRecordingAudio.value = true
            recordingDurationSeconds.value = 0

            // Background timer & amplitude ticker
            viewModelScope.launch {
                while (isRecordingAudio.value) {
                    kotlinx.coroutines.delay(1000)
                    recordingDurationSeconds.value += 1
                }
            }
        } catch (e: Exception) {
            errorMessage.value = "Failed to start audio recording: ${e.message}"
        }
    }

    fun stopVoiceRecording() {
        val file = audioRecorder.stopRecording()
        isRecordingAudio.value = false
        if (file != null && file.exists()) {
            mediaFile.value = file
            infoMessage.value = "Audio recorded (${file.length() / 1024} KB). Processing..."
            processAudioWithAI(file)
        }
    }

    fun onSpeechRecognized(text: String) {
        if (text.isNotBlank()) {
            inputText.value = text
            onInputTextChanged(text)
            parseAndApplyText()
        }
    }

    private fun processAudioWithAI(file: File) {
        viewModelScope.launch {
            isProcessingAI.value = true
            try {
                val parsed = geminiParser.parseAudioFile(file)
                if (parsed.personName != null) personName.value = parsed.personName
                if (parsed.amount > 0) amountText.value = parsed.amount.toInt().toString()
                transactionType.value = parsed.type
                if (parsed.description.isNotBlank()) {
                    if (inputText.value.isBlank()) inputText.value = parsed.description
                    notes.value = parsed.description
                }
                if (parsed.dueDateEpochMs != null) dueDate.value = parsed.dueDateEpochMs
            } catch (e: Exception) {
                // Audio file is saved locally regardless
                infoMessage.value = "Voice note attached to entry"
            } finally {
                isProcessingAI.value = false
            }
        }
    }

    fun setVideoRecorded(file: File) {
        mediaFile.value = file
        mediaType.value = MediaType.VIDEO
        infoMessage.value = "Video note attached (${file.name})"

        viewModelScope.launch {
            isProcessingAI.value = true
            try {
                val parsed = geminiParser.parseVideoFile(file)
                if (parsed.personName != null) personName.value = parsed.personName
                if (parsed.amount > 0) amountText.value = parsed.amount.toInt().toString()
                transactionType.value = parsed.type
                if (parsed.description.isNotBlank()) notes.value = parsed.description
                if (parsed.dueDateEpochMs != null) dueDate.value = parsed.dueDateEpochMs
            } catch (e: Exception) {
                // Video file is saved locally
            } finally {
                isProcessingAI.value = false
            }
        }
    }

    fun copyPickedVideoToInternalStorage(uri: Uri) {
        viewModelScope.launch {
            try {
                val app = getApplication<Application>()
                val mediaDir = File(app.filesDir, "media").apply { mkdirs() }
                val destFile = File(mediaDir, "VID_PICKED_${System.currentTimeMillis()}.mp4")

                app.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(destFile).use { output ->
                        input.copyTo(output)
                    }
                }
                setVideoRecorded(destFile)
            } catch (e: Exception) {
                errorMessage.value = "Could not load video: ${e.message}"
            }
        }
    }

    fun removeAttachment() {
        mediaFile.value = null
        mediaType.value = MediaType.TEXT
        infoMessage.value = "Attachment removed"
    }

    fun saveEntry() {
        val name = personName.value.trim()
        val amount = amountText.value.toDoubleOrNull() ?: 0.0

        if (name.isBlank()) {
            errorMessage.value = "Please enter or speak the person's name"
            return
        }
        if (amount <= 0.0 && transactionType.value != TransactionType.NOTE) {
            errorMessage.value = "Please specify a valid amount"
            return
        }

        viewModelScope.launch {
            try {
                repository.saveEntry(
                    contactName = name,
                    amount = amount,
                    type = transactionType.value,
                    rawText = inputText.value,
                    entryDate = entryDate.value,
                    dueDate = dueDate.value,
                    mediaType = mediaType.value,
                    mediaPath = mediaFile.value?.absolutePath,
                    notes = notes.value
                )
                saveSuccess.value = true
            } catch (e: Exception) {
                errorMessage.value = "Failed to save: ${e.message}"
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: LedgerRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewEntryViewModel(application, repository) as T
        }
    }
}
