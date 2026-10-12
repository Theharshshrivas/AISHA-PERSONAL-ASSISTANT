
package com.harsh.aisha

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_MY_PACKAGE_REPLACED -> {
                ReminderStore(context).rescheduleAll()
            }

            ACTION_SHOW_REMINDER -> showNotification(context, intent)
        }
    }

    private fun showNotification(context: Context, intent: Intent) {
        val id = intent.getIntExtra(EXTRA_ID, 1)
        val title = intent.getStringExtra(EXTRA_TITLE)
            .orEmpty().ifBlank { "Aisha reminder" }
        val note = intent.getStringExtra(EXTRA_NOTE)
            .orEmpty().ifBlank { "It's time!" }

        val manager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Aisha reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for reminders created in Aisha"
                }
            )
        }

        val openApp = PendingIntent.getActivity(
            context,
            id,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(note)
            .setStyle(NotificationCompat.BigTextStyle().bigText(note))
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(id, notification)
        } catch (_: SecurityException) {
            // Notifications may be disabled by the user.
        }
    }

    companion object {
        const val ACTION_SHOW_REMINDER = "com.harsh.aisha.SHOW_REMINDER"
        const val EXTRA_ID = "reminder_id"
        const val EXTRA_TITLE = "reminder_title"
        const val EXTRA_NOTE = "reminder_note"

        private const val CHANNEL_ID = "aisha_reminders_v1"
    }
}
