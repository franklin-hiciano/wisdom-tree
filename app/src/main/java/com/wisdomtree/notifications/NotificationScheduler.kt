package com.wisdomtree.notifications

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.wisdomtree.MainActivity
import java.util.Calendar

object NotificationScheduler {

    const val CHANNEL_ID = "wisdom_tree_daily"
    const val NOTIF_ID = 1001
    const val PREF_HOUR = "notif_hour"
    const val PREF_MINUTE = "notif_minute"
    const val PREF_ENABLED = "notif_enabled"
    const val PREFS_NAME = "wt_notif_prefs"

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Daily Tree Reminder",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminds you to run your wisdom tree for today"
        }
        context.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    fun schedule(context: Context, hour: Int, minute: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putInt(PREF_HOUR, hour)
            .putInt(PREF_MINUTE, minute)
            .putBoolean(PREF_ENABLED, true)
            .apply()

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, DailyReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            // If time already passed today, schedule for tomorrow
            if (before(Calendar.getInstance())) add(Calendar.DATE, 1)
        }

        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                intent
            )
        } catch (e: SecurityException) {
            // Exact alarm permission not granted — use inexact
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                intent
            )
        }
    }

    fun cancel(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putBoolean(PREF_ENABLED, false).apply()

        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val intent = PendingIntent.getBroadcast(
            context, 0,
            Intent(context, DailyReminderReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(intent)
    }

    fun getSettings(context: Context): Triple<Boolean, Int, Int> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Triple(
            prefs.getBoolean(PREF_ENABLED, false),
            prefs.getInt(PREF_HOUR, 20),
            prefs.getInt(PREF_MINUTE, 0)
        )
    }
}

class DailyReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // Re-schedule on boot
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val (enabled, hour, minute) = NotificationScheduler.getSettings(context)
            if (enabled) NotificationScheduler.schedule(context, hour, minute)
            return
        }

        val tapIntent = PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("launch_test", true)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Wisdom Tree")
            .setContentText("Time to run your tree for today 🌱")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Take a moment to reflect. Your tree is ready."))
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .build()

        context.getSystemService(NotificationManager::class.java)
            .notify(NotificationScheduler.NOTIF_ID, notification)
    }
}
