package com.smartkhata.app.ui.screens.backup

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartkhata.app.data.local.AppDatabase
import com.smartkhata.app.data.model.BackupManifest
import com.smartkhata.app.data.repository.BackupRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

class BackupViewModel(
    application: Application,
    private val repository: BackupRepository
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("smart_khata_settings", Context.MODE_PRIVATE)

    val geminiApiKey = MutableStateFlow(prefs.getString("gemini_api_key", "") ?: "")
    val isBackingUp = MutableStateFlow(false)
    val isRestoring = MutableStateFlow(false)
    val generatedBackupFile = MutableStateFlow<File?>(null)
    val restoreResult = MutableStateFlow<BackupManifest?>(null)
    val statusMessage = MutableStateFlow<String?>(null)

    fun saveGeminiApiKey(key: String) {
        geminiApiKey.value = key.trim()
        prefs.edit().putString("gemini_api_key", key.trim()).apply()
        statusMessage.value = "Gemini API Key saved!"
    }

    fun generateBackup() {
        viewModelScope.launch {
            isBackingUp.value = true
            try {
                val file = repository.createBackupFile()
                generatedBackupFile.value = file
                statusMessage.value = "Backup created: ${file.name} (${file.length() / 1024} KB)"
            } catch (e: Exception) {
                statusMessage.value = "Backup failed: ${e.message}"
            } finally {
                isBackingUp.value = false
            }
        }
    }

    fun restoreBackup(uri: Uri) {
        viewModelScope.launch {
            isRestoring.value = true
            try {
                val result = repository.restoreBackup(uri)
                if (result.isSuccess) {
                    val manifest = result.getOrThrow()
                    restoreResult.value = manifest
                    statusMessage.value = "Successfully restored ${manifest.totalEntries} entries and ${manifest.totalContacts} contacts!"
                } else {
                    statusMessage.value = "Restore failed: ${result.exceptionOrNull()?.message}"
                }
            } catch (e: Exception) {
                statusMessage.value = "Restore failed: ${e.message}"
            } finally {
                isRestoring.value = false
            }
        }
    }

    class Factory(
        private val application: Application,
        private val repository: BackupRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return BackupViewModel(application, repository) as T
        }
    }
}
