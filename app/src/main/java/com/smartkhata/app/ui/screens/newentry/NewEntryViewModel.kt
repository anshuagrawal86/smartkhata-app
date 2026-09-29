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
import com.smartkhata.app.util.AudioPlayerHelper
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
    val audioAmplitudes = MutableStateFlow<List<Float>>(emptyList())
    val isPlayingAudio = MutableStateFlow(false)
    val isProcessingAI = MutableStateFlow(false)
    val saveSuccess = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)
    val infoMessage = MutableStateFlow<String?>(null)

    fun init(mode: String, contactId: Long) {
        if (contactId > 0) {
            viewModelScope.launch {
                val contact = repository.getContactById(contactId)
                contact?.let {
                    personName.value = it.name
                }
            }
        }
    }

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
            // Only set notes if empty, and preserve full context
            if (notes.value.isBlank() && parsed.description.isNotBlank()) {
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
            errorMessage.value = "Please type or speak something to convert"
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
        if (parsed.type != TransactionType.NOTE) {
            transactionType.value = parsed.type
        }
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
            stopAudioPlayback()
            val file = audioRecorder.startRecording()
            mediaFile.value = file
            mediaType.value = MediaType.AUDIO
            isRecordingAudio.value = true
            recordingDurationSeconds.value = 0
            audioAmplitudes.value = emptyList()

            // Timer
            viewModelScope.launch {
                while (isRecordingAudio.value) {
                    kotlinx.coroutines.delay(1000)
                    recordingDurationSeconds.value += 1
                }
            }

            // Real-time amplitude waveform polling
            viewModelScope.launch {
                while (isRecordingAudio.value) {
                    kotlinx.coroutines.delay(60)
                    val amp = audioRecorder.getAmplitude()
                    val normalized = (amp.toFloat() / 32768f).coerceIn(0.05f, 1f)
                    val current = audioAmplitudes.value.toMutableList()
                    if (current.size >= 24) current.removeAt(0)
                    current.add(normalized)
                    audioAmplitudes.value = current
                }
            }
        } catch (e: Exception) {
            isRecordingAudio.value = false
            errorMessage.value = "Microphone error: ${e.message}"
        }
    }

    fun stopVoiceRecording() {
        val file = audioRecorder.stopRecording()
        isRecordingAudio.value = false
        if (file != null && file.exists()) {
            mediaFile.value = file
            mediaType.value = MediaType.AUDIO
            if (notes.value.isBlank()) {
                notes.value = if (inputText.value.isNotBlank()) inputText.value else "Voice note (${file.length() / 1024} KB)"
            }
            infoMessage.value = "Voice note attached successfully!"
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
            try {
                isProcessingAI.value = true
                val parsed = geminiParser.parseAudioFile(file)
                if (parsed.personName != null) personName.value = parsed.personName
                if (parsed.amount > 0) amountText.value = parsed.amount.toInt().toString()
                if (parsed.type != TransactionType.NOTE) transactionType.value = parsed.type
                if (parsed.description.isNotBlank()) {
                    if (inputText.value.isBlank()) inputText.value = parsed.description
                    notes.value = parsed.description
                }
                if (parsed.dueDateEpochMs != null) dueDate.value = parsed.dueDateEpochMs
            } catch (_: Exception) {
                // Audio file is saved locally regardless
            } finally {
                isProcessingAI.value = false
            }
        }
    }

    fun setVideoRecorded(file: File) {
        mediaFile.value = file
        mediaType.value = MediaType.VIDEO
        if (notes.value.isBlank()) {
            notes.value = if (inputText.value.isNotBlank()) inputText.value else "Video note (${file.length() / (1024 * 1024)} MB)"
        }
        infoMessage.value = "Video note attached (${file.name})"

        viewModelScope.launch {
            try {
                isProcessingAI.value = true
                val parsed = geminiParser.parseVideoFile(file)
                if (parsed.personName != null) personName.value = parsed.personName
                if (parsed.amount > 0) amountText.value = parsed.amount.toInt().toString()
                if (parsed.type != TransactionType.NOTE) transactionType.value = parsed.type
                if (parsed.description.isNotBlank()) notes.value = parsed.description
                if (parsed.dueDateEpochMs != null) dueDate.value = parsed.dueDateEpochMs
            } catch (_: Exception) {
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

    fun toggleAudioPlayback() {
        val file = mediaFile.value
        if (file == null || !file.exists()) return

        if (isPlayingAudio.value) {
            stopAudioPlayback()
        } else {
            AudioPlayerHelper.play(file.absolutePath) {
                isPlayingAudio.value = false
            }
            isPlayingAudio.value = true
        }
    }

    fun stopAudioPlayback() {
        AudioPlayerHelper.stop()
        isPlayingAudio.value = false
    }

    fun removeAttachment() {
        stopAudioPlayback()
        mediaFile.value = null
        mediaType.value = MediaType.TEXT
        infoMessage.value = "Attachment removed"
    }

    fun saveEntry() {
        var name = personName.value.trim()
        val amount = amountText.value.toDoubleOrNull() ?: 0.0

        if (name.isBlank()) {
            if (mediaType.value == MediaType.AUDIO) {
                name = "Voice Note"
            } else if (mediaType.value == MediaType.VIDEO) {
                name = "Video Note"
            } else {
                errorMessage.value = "Please enter or speak the person's name"
                return
            }
        }

        // Allow saving as NOTE if no amount was given
        val resolvedType = if (amount <= 0.0) TransactionType.NOTE else transactionType.value

        // CRITICAL: NEVER TRUNCATE USER NOTES
        // Preserve the full text entered by the user
        val finalNotes = when {
            notes.value.isNotBlank() -> notes.value.trim()
            inputText.value.isNotBlank() -> inputText.value.trim()
            mediaType.value == MediaType.AUDIO -> "Voice note recording"
            mediaType.value == MediaType.VIDEO -> "Video note attachment"
            else -> ""
        }

        viewModelScope.launch {
            try {
                stopAudioPlayback()
                repository.saveEntry(
                    contactName = name,
                    amount = amount,
                    type = resolvedType,
                    rawText = inputText.value.trim(),
                    entryDate = entryDate.value,
                    dueDate = dueDate.value,
                    mediaType = mediaType.value,
                    mediaPath = mediaFile.value?.absolutePath,
                    notes = finalNotes
                )
                saveSuccess.value = true
            } catch (e: Exception) {
                errorMessage.value = "Failed to save: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopAudioPlayback()
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
