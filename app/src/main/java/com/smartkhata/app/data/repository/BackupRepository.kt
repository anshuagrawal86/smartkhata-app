package com.smartkhata.app.data.repository

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.smartkhata.app.data.local.AppDatabase
import com.smartkhata.app.data.local.entity.ContactEntity
import com.smartkhata.app.data.local.entity.EntryEntity
import com.smartkhata.app.data.local.entity.ReminderEntity
import com.smartkhata.app.data.model.BackupManifest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(
    private val context: Context,
    private val db: AppDatabase
) {
    private val gson = Gson()

    data class BackupPayload(
        val manifest: BackupManifest,
        val contacts: List<ContactEntity>,
        val entries: List<EntryEntity>,
        val reminders: List<ReminderEntity>
    )

    suspend fun createBackupFile(): File = withContext(Dispatchers.IO) {
        val contacts = db.contactDao().getAllContacts()
        val entries = db.entryDao().getAllEntries()
        val reminders = db.reminderDao().getAllReminders()

        val mediaFiles = entries.mapNotNull { it.mediaPath }
            .map { File(it) }
            .filter { it.exists() }

        val manifest = BackupManifest(
            appVersion = "1.0.0",
            backupTimestamp = System.currentTimeMillis(),
            totalContacts = contacts.size,
            totalEntries = entries.size,
            totalReminders = reminders.size,
            mediaFiles = mediaFiles.map { it.name }
        )

        val payload = BackupPayload(
            manifest = manifest,
            contacts = contacts,
            entries = entries,
            reminders = reminders
        )

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val backupDir = File(context.cacheDir, "backups").apply { mkdirs() }
        val backupZip = File(backupDir, "SmartKhata_Backup_$timestamp.skbackup")

        ZipOutputStream(BufferedOutputStream(FileOutputStream(backupZip))).use { zos ->
            // 1. Write data JSON
            val dataJson = gson.toJson(payload)
            val jsonEntry = ZipEntry("data.json")
            zos.putNextEntry(jsonEntry)
            zos.write(dataJson.toByteArray(Charsets.UTF_8))
            zos.closeEntry()

            // 2. Add media files
            for (media in mediaFiles) {
                if (media.exists()) {
                    val mediaEntry = ZipEntry("media/${media.name}")
                    zos.putNextEntry(mediaEntry)
                    FileInputStream(media).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()
                }
            }
        }

        backupZip
    }

    suspend fun restoreBackup(backupUri: Uri): Result<BackupManifest> = withContext(Dispatchers.IO) {
        runCatching {
            var dataJsonString: String? = null
            val restoredMediaDir = File(context.filesDir, "media").apply { mkdirs() }

            context.contentResolver.openInputStream(backupUri)?.use { inputStream ->
                ZipInputStream(BufferedInputStream(inputStream)).use { zis ->
                    var entry: ZipEntry? = zis.nextEntry
                    val buffer = ByteArray(8192)

                    while (entry != null) {
                        if (!entry.isDirectory) {
                            if (entry.name == "data.json") {
                                val out = ByteArrayOutputStream()
                                var len: Int
                                while (zis.read(buffer).also { len = it } > 0) {
                                    out.write(buffer, 0, len)
                                }
                                dataJsonString = out.toString(Charsets.UTF_8.name())
                            } else if (entry.name.startsWith("media/")) {
                                val filename = File(entry.name).name
                                val mediaTarget = File(restoredMediaDir, filename)
                                FileOutputStream(mediaTarget).use { fos ->
                                    var len: Int
                                    while (zis.read(buffer).also { len = it } > 0) {
                                        fos.write(buffer, 0, len)
                                    }
                                }
                            }
                        }
                        zis.closeEntry()
                        entry = zis.nextEntry
                    }
                }
            } ?: throw IllegalStateException("Could not open backup file stream")

            if (dataJsonString.isNullOrBlank()) {
                throw IllegalStateException("Invalid backup file: data.json not found")
            }

            val payload = gson.fromJson(dataJsonString, BackupPayload::class.java)

            // Restore Database
            db.runInTransaction {
                // We preserve existing data by merging or replacing
                kotlinx.coroutines.runBlocking {
                    // Update media paths to point to current device path
                    val updatedEntries = payload.entries.map { e ->
                        if (!e.mediaPath.isNullOrBlank()) {
                            val mediaName = File(e.mediaPath).name
                            val localFile = File(restoredMediaDir, mediaName)
                            e.copy(mediaPath = localFile.absolutePath)
                        } else {
                            e
                        }
                    }

                    db.contactDao().insertAll(payload.contacts)
                    db.entryDao().insertAll(updatedEntries)
                    db.reminderDao().insertAll(payload.reminders)
                }
            }

            payload.manifest
        }
    }
}
