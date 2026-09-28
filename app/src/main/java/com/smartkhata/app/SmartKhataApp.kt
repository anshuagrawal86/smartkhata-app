package com.smartkhata.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.smartkhata.app.data.local.AppDatabase
import com.smartkhata.app.data.repository.BackupRepository
import com.smartkhata.app.data.repository.LedgerRepository

class SmartKhataApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var ledgerRepository: LedgerRepository
        private set

    lateinit var backupRepository: BackupRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        database = AppDatabase.getInstance(this)
        ledgerRepository = LedgerRepository(database)
        backupRepository = BackupRepository(this, database)

        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "smart_khata_reminders",
                getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.channel_reminder_desc)
                enableVibration(true)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        lateinit var instance: SmartKhataApp
            private set
    }
}
