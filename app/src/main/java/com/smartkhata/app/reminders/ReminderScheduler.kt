package com.smartkhata.app.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log

class ReminderScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager

    fun scheduleReminder(
        reminderId: Long,
        contactName: String,
        amount: Double,
        notes: String,
        remindAtEpochMs: Long
    ) {
        if (remindAtEpochMs <= System.currentTimeMillis()) return

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.smartkhata.app.ACTION_TRIGGER_REMINDER"
            putExtra("reminder_id", reminderId)
            putExtra("contact_name", contactName)
            putExtra("amount", amount)
            putExtra("notes", notes)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager?.canScheduleExactAlarms() == true) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        remindAtEpochMs,
                        pendingIntent
                    )
                } else {
                    alarmManager?.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        remindAtEpochMs,
                        pendingIntent
                    )
                }
            } else {
                alarmManager?.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    remindAtEpochMs,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            Log.e("ReminderScheduler", "Could not set exact alarm", e)
        }
    }

    fun cancelReminder(reminderId: Long) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.smartkhata.app.ACTION_TRIGGER_REMINDER"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager?.cancel(pendingIntent)
        }
    }
}
