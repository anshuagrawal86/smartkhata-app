package com.smartkhata.app.ui.screens.newentry

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartkhata.app.data.local.AppDatabase
import com.smartkhata.app.data.model.MediaType
import com.smartkhata.app.data.model.TransactionType
import com.smartkhata.app.data.parser.GeminiHinglishParser
import com.smartkhata.app.data.parser.LocalHinglishParser
import com.smartkhata.app.data.repository.LedgerRepository
import com.smartkhata.app.util.AudioRecorderUtil
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

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

    val isRecordingAudio = MutableStateFlow(false)
    val isProcessingAI = MutableStateFlow(false)
    val saveSuccess = MutableStateFlow(false)
    val errorMessage = MutableStateFlow<String?>(null)

    fun onInputTextChanged(text: String) {
        inputText.value = text
        // Live local parsing preview
        if (text.length > 5) {
            val parsed = LocalHinglishParser.parse(text)
            if (personName.value.isBlank() && parsed.personName != null) {
                personName.value = parsed.personName
            }
            if (amountText.value.isBlank() && parsed.amount > 0) {
                amountText.value = parsed.amount.toInt().toString()
            }
            if (parsed.type != TransactionType.NOTE) {
                transactionType.value = parsed.type
            }
            if (notes.value.isBlank() && parsed.description.isNotBlank()) {
                notes.value = parsed.description
            }
            if (dueDate.value == null && parsed.dueDateEpochMs != null) {
                dueDate.value = parsed.dueDateEpochMs
            }
        }
    }

    fun startVoiceRecording() {
        try {
            val file = audioRecorder.startRecording()
            mediaFile.value = file
            mediaType.value = MediaType.AUDIO
            isRecordingAudio.value = true
        } catch (e: Exception) {
            errorMessage.value = "Failed to start audio recording: ${e.message}"
        }
    }

    fun stopVoiceRecording() {
        val file = audioRecorder.stopRecording()
        isRecordingAudio.value = false
        if (file != null && file.exists()) {
            mediaFile.value = file
            processAudioWithAI(file)
        }
    }

    fun onSpeechRecognized(text: String) {
        inputText.value = text
        onInputTextChanged(text)
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
                errorMessage.value = "Audio parsing error: ${e.message}"
            } finally {
                isProcessingAI.value = false
            }
        }
    }

    fun setVideoRecorded(file: File) {
        mediaFile.value = file
        mediaType.value = MediaType.VIDEO
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
                errorMessage.value = "Video processing error: ${e.message}"
            } finally {
                isProcessingAI.value = false
            }
        }
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
