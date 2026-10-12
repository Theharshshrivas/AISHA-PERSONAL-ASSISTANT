
package com.harsh.aisha

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject

data class AishaReminder(
    val id: Int,
    val title: String,
    val note: String,
    val triggerAtMillis: Long
)

class ReminderStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("aisha_reminders", Context.MODE_PRIVATE)
    private val alarms = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun getAll(): List<AishaReminder> {
        return try {
            val array = JSONArray(prefs.getString("items", "[]") ?: "[]")
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        AishaReminder(
                            item.getInt("id"),
                            item.getString("title"),
                            item.optString("note", ""),
                            item.getLong("triggerAtMillis")
                        )
                    )
                }
            }.sortedBy { it.triggerAtMillis }
        } catch (_: Exception) {
            emptyList()
        }
    }

    @Synchronized
    fun add(title: String, note: String, time: Long): Boolean {
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank() || time <= System.currentTimeMillis()) return false

        val items = getAll().toMutableList()
        var id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt().coerceAtLeast(1)
        while (items.any { it.id == id }) {
            id = if (id == Int.MAX_VALUE) 1 else id + 1
        }

        val reminder = AishaReminder(id, cleanTitle, note.trim(), time)
        items.add(reminder)
        save(items)
        schedule(reminder)
        return true
    }

    @Synchronized
    fun delete(id: Int) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW_REMINDER
        }
        val pending = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarms.cancel(pending)
        pending.cancel()
        save(getAll().filterNot { it.id == id })
    }

    fun rescheduleAll() {
        getAll().forEach {
            if (it.triggerAtMillis > System.currentTimeMillis()) {
                schedule(it)
            } else {
                delete(it.id)
            }
        }
    }

    private fun save(items: List<AishaReminder>) {
        val array = JSONArray()
        items.forEach {
            array.put(
                JSONObject()
                    .put("id", it.id)
                    .put("title", it.title)
                    .put("note", it.note)
                    .put("triggerAtMillis", it.triggerAtMillis)
            )
        }
        prefs.edit().putString("items", array.toString()).apply()
    }

    private fun schedule(reminder: AishaReminder) {
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = ReminderReceiver.ACTION_SHOW_REMINDER
            putExtra(ReminderReceiver.EXTRA_ID, reminder.id)
            putExtra(ReminderReceiver.EXTRA_TITLE, reminder.title)
            putExtra(ReminderReceiver.EXTRA_NOTE, reminder.note)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            reminder.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarms.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminder.triggerAtMillis,
                    pending
                )
            } else {
                alarms.set(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
            }
        } catch (_: SecurityException) {
            // Android or device policy may restrict alarm scheduling.
        }
    }
}
