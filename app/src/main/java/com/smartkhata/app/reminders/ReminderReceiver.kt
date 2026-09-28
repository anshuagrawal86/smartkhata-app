package com.smartkhata.app.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.smartkhata.app.MainActivity
import com.smartkhata.app.R
import com.smartkhata.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action

        if (action == Intent.ACTION_BOOT_COMPLETED) {
            // Phone restarted: reschedule pending alarms
            rescheduleAllPending(context)
            return
        }

        if (action == "com.smartkhata.app.ACTION_TRIGGER_REMINDER") {
            val reminderId = intent.getLongExtra("reminder_id", 0L)
            val contactName = intent.getStringExtra("contact_name") ?: "Contact"
            val amount = intent.getDoubleExtra("amount", 0.0)
            val notes = intent.getStringExtra("notes") ?: ""

            showNotification(context, reminderId, contactName, amount, notes)
        }
    }

    private fun showNotification(
        context: Context,
        reminderId: Long,
        contactName: String,
        amount: Double,
        notes: String
    ) {
        val channelId = "smart_khata_reminders"
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                context.getString(R.string.channel_reminder_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = context.getString(R.string.channel_reminder_desc)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            reminderId.toInt(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = if (amount > 0) {
            "Reminder: ₹${amount.toInt()} with $contactName"
        } else {
            "SmartKhata Reminder: $contactName"
        }

        val contentText = if (notes.isNotBlank()) notes else "You have a scheduled reminder for $contactName."

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(reminderId.toInt(), notification)
    }

    private fun rescheduleAllPending(context: Context) {
        CoroutineScope(Dispatchers.IO).launch {
            val db = AppDatabase.getInstance(context)
            val reminders = db.reminderDao().getAllReminders()
            val scheduler = ReminderScheduler(context)
            val now = System.currentTimeMillis()

            for (r in reminders) {
                if (!r.isCompleted && r.remindAt > now) {
                    scheduler.scheduleReminder(
                        reminderId = r.id,
                        contactName = r.contactName,
                        amount = r.amount,
                        notes = r.notes,
                        remindAtEpochMs = r.remindAt
                    )
                }
            }
        }
    }
}
